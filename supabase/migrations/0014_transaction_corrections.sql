-- =============================================================================
-- DebtTracker — правки дзеркальних транзакцій за згодою автора.
--
-- Коли контрагент додає операцію в дзеркальний борг, друга сторона отримує
-- сповіщення DEBT_TRANSACTION_ADDED / CREDIT_TRANSACTION_ADDED. Правити саму
-- операцію може лише її власник (RLS + guard-тригери), тож отримувач надсилає
-- автору ПРОПОЗИЦІЮ правки — «неправильна сума» (з новою сумою) або «операції
-- не було» (soft-delete). Автор приймає/відхиляє. Той самий патерн, що
-- pending_link_requests + approve/reject_link_request (0013).
--
-- DebtTracker — corrections to a mirrored transaction, applied only with the
-- author's consent. Only the transaction's owner may edit it (RLS + guard
-- triggers), so the recipient sends the author a PROPOSAL — "wrong amount"
-- (with the corrected amount) or "didn't happen" (soft-delete). The author
-- approves/rejects. Same shape as pending_link_requests + approve/reject
-- (0013).
-- =============================================================================

begin;

-- -----------------------------------------------------------------------------
-- 1. transaction_corrections — пропозиція правки, що чекає на рішення автора.
--    Інсертити/оновлювати можуть лише SECURITY DEFINER-функції нижче —
--    авторизованим користувачам дозволено лише select власних рядків (як
--    пропонувач, так і ціль), як і в notifications / pending_link_requests.
-- 1. transaction_corrections — a correction proposal awaiting the author's
--    decision. Only the SECURITY DEFINER functions below may insert/update;
--    an authenticated user may only select their own rows (proposer or
--    target), same as notifications / pending_link_requests.
-- -----------------------------------------------------------------------------
create table public.transaction_corrections (
    id                     uuid primary key default gen_random_uuid(),
    source_kind            text not null check (source_kind in ('debt_transaction', 'creditor_transaction')),
    source_transaction_id  uuid not null,          -- рядок у debt_transactions/creditor_transactions автора / the author's debt_transactions/creditor_transactions row
    proposer_user_id       uuid not null references auth.users (id) on delete cascade,  -- отримувач сповіщення, що пропонує правку / notification recipient proposing the fix
    target_user_id         uuid not null references auth.users (id) on delete cascade,  -- автор операції, що приймає/відхиляє / the transaction's author, who approves/rejects
    reason                 text not null check (reason in ('wrong_amount', 'not_happened')),
    proposed_amount        numeric(14, 2) check (proposed_amount is null or proposed_amount > 0),  -- модуль нової суми; null коли reason = 'not_happened' / magnitude of the new amount; null when reason = 'not_happened'
    status                 text not null default 'pending' check (status in ('pending', 'approved', 'rejected')),
    created_at             timestamptz not null default now(),
    resolved_at            timestamptz
);

comment on table public.transaction_corrections is 'Пропозиція правки дзеркальної операції (0014) — propose/approve/reject_transaction_correction. A correction proposal for a mirrored transaction (0014).';

-- Один активний pending-запит на вихідну операцію: повторний виклик
-- propose_transaction_correction для тієї самої операції не плодить дублікати.
-- One active pending request per source transaction: calling
-- propose_transaction_correction again for the same transaction doesn't spawn duplicates.
create unique index idx_transaction_corrections_unique_pending
    on public.transaction_corrections (source_kind, source_transaction_id)
    where status = 'pending';

create index idx_transaction_corrections_target on public.transaction_corrections (target_user_id, status);

alter table public.transaction_corrections enable row level security;

create policy "transaction_corrections_proposer_select" on public.transaction_corrections
    for select
    using (proposer_user_id = auth.uid());

create policy "transaction_corrections_target_select" on public.transaction_corrections
    for select
    using (target_user_id = auth.uid());

-- -----------------------------------------------------------------------------
-- 2. notifications — нові поля + три нових типи в check-constraint.
-- 2. notifications — new columns + three new types in the check constraint.
-- -----------------------------------------------------------------------------
alter table public.notifications
    add column related_transaction_id uuid,  -- id вихідної операції автора (для *_TRANSACTION_ADDED) — заповнює propagate_* / the author's source transaction id (on *_TRANSACTION_ADDED) — set by propagate_*
    add column related_correction_id  uuid references public.transaction_corrections (id) on delete set null;

comment on column public.notifications.related_transaction_id is 'На DEBT_TRANSACTION_ADDED/CREDIT_TRANSACTION_ADDED — id вихідної операції в акаунті актора, яку RPC propose_transaction_correction має право правити. On DEBT_TRANSACTION_ADDED/CREDIT_TRANSACTION_ADDED — id of the source transaction in the actor''s account that propose_transaction_correction may edit.';

alter table public.notifications drop constraint notifications_type_check;

alter table public.notifications
    add constraint notifications_type_check check (
        type in (
            'DEBTOR_LINKED', 'CREDITOR_LINKED', 'DEBT_TRANSACTION_ADDED', 'CREDIT_TRANSACTION_ADDED',
            'LINK_REQUEST', 'LINK_REQUEST_APPROVED',
            'TRANSACTION_CORRECTION', 'TRANSACTION_CORRECTION_APPROVED', 'TRANSACTION_CORRECTION_REJECTED'
        )
    );

-- -----------------------------------------------------------------------------
-- 3. propagate_debt_transaction / propagate_creditor_transaction — copy з 0009
--    + одна зміна: у гілці INSERT сповіщення тепер несе related_transaction_id
--    = NEW.id (id вихідної операції автора), щоб отримувач міг запропонувати
--    правку саме цього рядка.
-- 3. propagate_debt_transaction / propagate_creditor_transaction — copied from
--    0009 with a single change: the INSERT-branch notification now carries
--    related_transaction_id = NEW.id (the author's source transaction id) so
--    the recipient can propose a correction to that exact row.
-- -----------------------------------------------------------------------------
create or replace function public.propagate_debt_transaction()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    v_debtor  public.debtors%rowtype;
begin
    if NEW.mirror_transaction_id is not null then
        return NEW;
    end if;

    if TG_OP = 'UPDATE'
       and NEW.amount is not distinct from OLD.amount
       and NEW.method is not distinct from OLD.method
       and NEW.transaction_date is not distinct from OLD.transaction_date
       and NEW.comment is not distinct from OLD.comment
       and NEW.is_deleted is not distinct from OLD.is_deleted then
        return NEW;
    end if;

    select * into v_debtor from public.debtors where id = NEW.debtor_id;
    if v_debtor.linked_user_id is null or v_debtor.mirror_creditor_id is null then
        return NEW;
    end if;

    if TG_OP = 'INSERT' then
        insert into public.creditor_transactions (
            id, creditor_id, user_id, amount, method,
            transaction_date, comment, mirror_transaction_id, created_at, updated_at
        ) values (
            gen_random_uuid(), v_debtor.mirror_creditor_id, v_debtor.linked_user_id,
            NEW.amount, NEW.method, NEW.transaction_date, NEW.comment,
            NEW.id, now(), now()
        );

        insert into public.notifications (
            id, user_id, type, actor_user_id, actor_display_name,
            related_creditor_id, related_transaction_id, amount, currency, created_at
        ) values (
            gen_random_uuid(), v_debtor.linked_user_id, 'DEBT_TRANSACTION_ADDED', v_debtor.user_id,
            (select coalesce(nullif(display_name, ''), email) from public.profiles where id = v_debtor.user_id),
            v_debtor.mirror_creditor_id, NEW.id, NEW.amount, v_debtor.currency, now()
        );
    elsif TG_OP = 'UPDATE' then
        update public.creditor_transactions
           set amount = NEW.amount,
               method = NEW.method,
               transaction_date = NEW.transaction_date,
               comment = NEW.comment,
               is_deleted = NEW.is_deleted
         where mirror_transaction_id = NEW.id;
    end if;

    return NEW;
end;
$$;

create or replace function public.propagate_creditor_transaction()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    v_creditor  public.creditors%rowtype;
begin
    if NEW.mirror_transaction_id is not null then
        return NEW;
    end if;

    if TG_OP = 'UPDATE'
       and NEW.amount is not distinct from OLD.amount
       and NEW.method is not distinct from OLD.method
       and NEW.transaction_date is not distinct from OLD.transaction_date
       and NEW.comment is not distinct from OLD.comment
       and NEW.is_deleted is not distinct from OLD.is_deleted then
        return NEW;
    end if;

    select * into v_creditor from public.creditors where id = NEW.creditor_id;
    if v_creditor.linked_user_id is null or v_creditor.mirror_debtor_id is null then
        return NEW;
    end if;

    if TG_OP = 'INSERT' then
        insert into public.debt_transactions (
            id, debtor_id, user_id, amount, method,
            transaction_date, comment, mirror_transaction_id, created_at, updated_at
        ) values (
            gen_random_uuid(), v_creditor.mirror_debtor_id, v_creditor.linked_user_id,
            NEW.amount, NEW.method, NEW.transaction_date, NEW.comment,
            NEW.id, now(), now()
        );

        insert into public.notifications (
            id, user_id, type, actor_user_id, actor_display_name,
            related_debtor_id, related_transaction_id, amount, currency, created_at
        ) values (
            gen_random_uuid(), v_creditor.linked_user_id, 'CREDIT_TRANSACTION_ADDED', v_creditor.user_id,
            (select coalesce(nullif(display_name, ''), email) from public.profiles where id = v_creditor.user_id),
            v_creditor.mirror_debtor_id, NEW.id, NEW.amount, v_creditor.currency, now()
        );
    elsif TG_OP = 'UPDATE' then
        update public.debt_transactions
           set amount = NEW.amount,
               method = NEW.method,
               transaction_date = NEW.transaction_date,
               comment = NEW.comment,
               is_deleted = NEW.is_deleted
         where mirror_transaction_id = NEW.id;
    end if;

    return NEW;
end;
$$;

-- -----------------------------------------------------------------------------
-- 4. propose_transaction_correction — рішення ОТРИМУВАЧА сповіщення про нову
--    операцію: створює transaction_corrections + сповіщає автора
--    ('TRANSACTION_CORRECTION'). Ідемпотентна на рівні pending-запиту.
-- 4. propose_transaction_correction — the NOTIFICATION RECIPIENT's move:
--    creates a transaction_corrections row + notifies the author
--    ('TRANSACTION_CORRECTION'). Idempotent at the pending-request level.
-- -----------------------------------------------------------------------------
create function public.propose_transaction_correction(
    p_notification_id uuid,
    p_reason          text,
    p_proposed_amount numeric
)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
    v_notification    public.notifications%rowtype;
    v_source_kind     text;
    v_correction_id   uuid;
    v_proposer_name   text;
    v_author_row_id   uuid;   -- debtor_id / creditor_id у акаунті автора / debtor_id / creditor_id in the author's account
    v_currency        text;
begin
    if p_reason not in ('wrong_amount', 'not_happened') then
        raise exception 'Invalid reason: %', p_reason;
    end if;
    if p_reason = 'wrong_amount' and (p_proposed_amount is null or p_proposed_amount <= 0) then
        raise exception 'wrong_amount requires a positive proposed amount';
    end if;

    select * into v_notification
      from public.notifications
     where id = p_notification_id and user_id = auth.uid();
    if not found then
        raise exception 'Notification not found or not yours';
    end if;
    if v_notification.type not in ('DEBT_TRANSACTION_ADDED', 'CREDIT_TRANSACTION_ADDED')
       or v_notification.related_transaction_id is null
       or v_notification.actor_user_id is null then
        raise exception 'Notification is not a correctable transaction notification';
    end if;

    v_source_kind := case v_notification.type
                         when 'DEBT_TRANSACTION_ADDED' then 'debt_transaction'
                         else 'creditor_transaction'
                     end;

    -- Джерело живе в акаунті автора (actor_user_id) — читаємо як SECURITY DEFINER.
    -- The source lives in the author's account (actor_user_id) — read as SECURITY DEFINER.
    if v_source_kind = 'debt_transaction' then
        select dt.debtor_id, d.currency into v_author_row_id, v_currency
          from public.debt_transactions dt
          join public.debtors d on d.id = dt.debtor_id
         where dt.id = v_notification.related_transaction_id
           and dt.user_id = v_notification.actor_user_id
           and dt.is_deleted = false;
    else
        select ct.creditor_id, c.currency into v_author_row_id, v_currency
          from public.creditor_transactions ct
          join public.creditors c on c.id = ct.creditor_id
         where ct.id = v_notification.related_transaction_id
           and ct.user_id = v_notification.actor_user_id
           and ct.is_deleted = false;
    end if;
    if v_author_row_id is null then
        raise exception 'Source transaction no longer exists';
    end if;

    insert into public.transaction_corrections (
        source_kind, source_transaction_id, proposer_user_id, target_user_id,
        reason, proposed_amount
    ) values (
        v_source_kind, v_notification.related_transaction_id, auth.uid(), v_notification.actor_user_id,
        p_reason, case when p_reason = 'wrong_amount' then p_proposed_amount end
    )
    on conflict (source_kind, source_transaction_id) where status = 'pending' do nothing
    returning id into v_correction_id;

    if v_correction_id is null then
        select id into v_correction_id
          from public.transaction_corrections
         where source_kind = v_source_kind
           and source_transaction_id = v_notification.related_transaction_id
           and status = 'pending';
        return v_correction_id;
    end if;

    select coalesce(nullif(display_name, ''), email) into v_proposer_name
      from public.profiles where id = auth.uid();

    insert into public.notifications (
        id, user_id, type, actor_user_id, actor_display_name,
        related_debtor_id, related_creditor_id, related_transaction_id,
        related_correction_id, amount, currency, created_at
    ) values (
        gen_random_uuid(), v_notification.actor_user_id, 'TRANSACTION_CORRECTION', auth.uid(),
        v_proposer_name,
        case when v_source_kind = 'debt_transaction' then v_author_row_id end,
        case when v_source_kind = 'creditor_transaction' then v_author_row_id end,
        v_notification.related_transaction_id,
        v_correction_id,
        case when p_reason = 'wrong_amount' then p_proposed_amount end,
        v_currency, now()
    );

    return v_correction_id;
end;
$$;

revoke all on function public.propose_transaction_correction(uuid, text, numeric) from public, anon;
grant execute on function public.propose_transaction_correction(uuid, text, numeric) to authenticated;

-- -----------------------------------------------------------------------------
-- 5. approve_transaction_correction — рішення АВТОРА: правит власну операцію
--    (тригер propagate_* дзеркалить зміну назад) і сповіщає пропонувача.
-- 5. approve_transaction_correction — the AUTHOR's decision: edits their own
--    transaction (the propagate_* trigger mirrors the change back) and
--    notifies the proposer.
-- -----------------------------------------------------------------------------
create function public.approve_transaction_correction(p_correction_id uuid)
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
     where id = p_correction_id and target_user_id = auth.uid() and status = 'pending';
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

revoke all on function public.approve_transaction_correction(uuid) from public, anon;
grant execute on function public.approve_transaction_correction(uuid) to authenticated;

-- -----------------------------------------------------------------------------
-- 6. reject_transaction_correction — автор відхиляє правку. На відміну від
--    reject_link_request, тут обидві сторони вже пов'язані спільним боргом —
--    enumeration не проблема, тож пропонувача сповіщаємо.
-- 6. reject_transaction_correction — the author rejects the correction.
--    Unlike reject_link_request, both parties already share a debt here —
--    no enumeration concern — so the proposer is notified.
-- -----------------------------------------------------------------------------
create function public.reject_transaction_correction(p_correction_id uuid)
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
     where id = p_correction_id and target_user_id = auth.uid() and status = 'pending';
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

revoke all on function public.reject_transaction_correction(uuid) from public, anon;
grant execute on function public.reject_transaction_correction(uuid) to authenticated;

commit;

-- =============================================================================
-- Кінець міграції 0014. / End of migration 0014.
-- =============================================================================
