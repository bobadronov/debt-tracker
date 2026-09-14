-- =============================================================================
-- DebtTracker — фікс гонки в approve/reject_transaction_correction (0014).
--
-- SELECT ... WHERE status = 'pending' і подальший UPDATE не були атомарними:
-- два паралельні виклики (подвійний клік, retry після таймауту) могли обидва
-- пройти перевірку 'pending' до коміту першого, спричиняючи подвійну обробку
-- (двічі списану суму / два сповіщення). Фікс — FOR UPDATE, що блокує рядок
-- на час транзакції функції, тож другий виклик або чекає і бачить уже
-- 'approved'/'rejected' (raise exception), або блокується до коміту першого.
--
-- DebtTracker — fix a race in approve/reject_transaction_correction (0014).
--
-- The SELECT ... WHERE status = 'pending' and the later UPDATE weren't atomic:
-- two concurrent calls (double-click, a retry after timeout) could both pass
-- the 'pending' check before the first one committed, causing double
-- processing (amount applied twice / two notifications). Fix — FOR UPDATE,
-- locking the row for the function's transaction, so the second call either
-- waits and then sees 'approved'/'rejected' (raise exception), or blocks
-- until the first commits.
-- =============================================================================

begin;

create or replace function public.approve_transaction_correction(p_correction_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    v            public.transaction_corrections%rowtype;
    v_old_amount numeric;
    v_new_amount numeric;
    v_name       text;
begin
    select * into v from public.transaction_corrections
     where id = p_correction_id and target_user_id = auth.uid() and status = 'pending'
     for update;
    if not found then
        raise exception 'Correction not found, not yours, or already resolved';
    end if;

    if v.source_kind = 'debt_transaction' then
        select amount into v_old_amount from public.debt_transactions
         where id = v.source_transaction_id and user_id = auth.uid() and is_deleted = false;
    else
        select amount into v_old_amount from public.creditor_transactions
         where id = v.source_transaction_id and user_id = auth.uid() and is_deleted = false;
    end if;
    if v_old_amount is null then
        raise exception 'Source transaction no longer exists';
    end if;

    if v.reason = 'not_happened' then
        if v.source_kind = 'debt_transaction' then
            update public.debt_transactions set is_deleted = true, updated_at = now()
             where id = v.source_transaction_id and user_id = auth.uid();
        else
            update public.creditor_transactions set is_deleted = true, updated_at = now()
             where id = v.source_transaction_id and user_id = auth.uid();
        end if;
    else
        -- Знак (LEND/BORROW < 0, REPAY/RETURN > 0, спек §9) зберігаємо з наявного рядка.
        -- Keep the sign (LEND/BORROW < 0, REPAY/RETURN > 0) from the existing row.
        v_new_amount := case when v_old_amount < 0 then -abs(v.proposed_amount) else abs(v.proposed_amount) end;
        if v.source_kind = 'debt_transaction' then
            update public.debt_transactions set amount = v_new_amount, updated_at = now()
             where id = v.source_transaction_id and user_id = auth.uid();
        else
            update public.creditor_transactions set amount = v_new_amount, updated_at = now()
             where id = v.source_transaction_id and user_id = auth.uid();
        end if;
    end if;

    update public.transaction_corrections
       set status = 'approved', resolved_at = now()
     where id = p_correction_id;

    select coalesce(nullif(display_name, ''), email) into v_name
      from public.profiles where id = auth.uid();

    insert into public.notifications (
        id, user_id, type, actor_user_id, actor_display_name,
        related_correction_id, created_at
    ) values (
        gen_random_uuid(), v.proposer_user_id, 'TRANSACTION_CORRECTION_APPROVED', auth.uid(),
        v_name, p_correction_id, now()
    );
end;
$$;

create or replace function public.reject_transaction_correction(p_correction_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    v       public.transaction_corrections%rowtype;
    v_name  text;
begin
    select * into v from public.transaction_corrections
     where id = p_correction_id and target_user_id = auth.uid() and status = 'pending'
     for update;
    if not found then
        raise exception 'Correction not found, not yours, or already resolved';
    end if;

    update public.transaction_corrections
       set status = 'rejected', resolved_at = now()
     where id = p_correction_id;

    select coalesce(nullif(display_name, ''), email) into v_name
      from public.profiles where id = auth.uid();

    insert into public.notifications (
        id, user_id, type, actor_user_id, actor_display_name,
        related_correction_id, created_at
    ) values (
        gen_random_uuid(), v.proposer_user_id, 'TRANSACTION_CORRECTION_REJECTED', auth.uid(),
        v_name, p_correction_id, now()
    );
end;
$$;

commit;

-- =============================================================================
-- Кінець міграції 0016. / End of migration 0016.
-- =============================================================================
