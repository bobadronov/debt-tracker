-- =============================================================================
-- DebtTracker — дрібний клінап після 0007/0008 (code review):
-- 1. Індекс на profiles.phone — пошук збігу в link_debtor_to_registered_user/
--    link_creditor_to_registered_user раніше йшов повним сканом за телефоном
--    (email-гілка вже мала idx_profiles_email_lower).
-- 2. Захист від зайвого запису в propagate_*_transaction() — не чіпати
--    дзеркальний рядок, якщо UPDATE на debt_transactions/creditor_transactions
--    не змінив жодного з полів, які взагалі дзеркаляться (зайвий запис +
--    блокування рядка в пов'язаному акаунті на кожен нерелевантний UPDATE).
--    Це живе всередині функції (не WHEN-умови тригера), бо WHEN не може
--    порівнювати OLD/NEW для INSERT, де OLD не існує.
-- DebtTracker — small cleanup after 0007/0008 (code review):
-- 1. Index on profiles.phone — the match in link_debtor_to_registered_user/
--    link_creditor_to_registered_user previously fell back to a sequential
--    scan on phone (the email branch already had idx_profiles_email_lower).
-- 2. Guard against a no-op write in propagate_*_transaction() — skip touching
--    the mirror row when an UPDATE on debt_transactions/creditor_transactions
--    didn't change any of the fields that actually get mirrored (an extra
--    write + row lock on the linked account's table otherwise). This lives
--    inside the function body (not a trigger WHEN clause), since WHEN can't
--    compare OLD/NEW on INSERT, where OLD doesn't exist.
-- =============================================================================

create index idx_profiles_phone on public.profiles (phone) where phone is not null;

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
            related_creditor_id, amount, currency, created_at
        ) values (
            gen_random_uuid(), v_debtor.linked_user_id, 'DEBT_TRANSACTION_ADDED', v_debtor.user_id,
            (select coalesce(nullif(display_name, ''), email) from public.profiles where id = v_debtor.user_id),
            v_debtor.mirror_creditor_id, NEW.amount, v_debtor.currency, now()
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
            related_debtor_id, amount, currency, created_at
        ) values (
            gen_random_uuid(), v_creditor.linked_user_id, 'CREDIT_TRANSACTION_ADDED', v_creditor.user_id,
            (select coalesce(nullif(display_name, ''), email) from public.profiles where id = v_creditor.user_id),
            v_creditor.mirror_debtor_id, NEW.amount, v_creditor.currency, now()
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

-- =============================================================================
-- Кінець міграції 0009. / End of migration 0009.
-- =============================================================================
