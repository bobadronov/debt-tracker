-- =============================================================================
-- DebtTracker — таблиця public.feedback: пропозиції та повідомлення про помилки,
-- які користувачі надсилають зі сторінки legal/feedback.html (GitHub Pages).
-- Заповнюється ВИКЛЮЧНО Edge Function `submit-feedback` (service role) — та сама
-- функція паралельно шле лист мейнтейнеру через Resend. Рядок тут — це просто
-- журнал на випадок, якщо лист загубиться.
-- DebtTracker — public.feedback table: suggestions and bug reports users send
-- from legal/feedback.html (GitHub Pages). Written ONLY by the `submit-feedback`
-- Edge Function (service role), which also emails the maintainer via Resend.
-- The row is just a fallback log in case the email is ever lost.
--
-- RLS увімкнено без жодної політики: anon/authenticated не можуть ні читати, ні
-- писати. service_role (Edge Function) оминає RLS і залишається єдиним, хто має
-- доступ.
-- RLS enabled with no policy: anon/authenticated can neither read nor write.
-- service_role (the Edge Function) bypasses RLS and stays the only accessor.
-- =============================================================================

begin;

create table public.feedback (
    id          uuid primary key default gen_random_uuid(),
    created_at  timestamptz not null default now(),
    category    text not null default 'other'
                    check (category in ('suggestion', 'bug', 'other')),
    name        text,
    email       text,
    message     text not null check (char_length(message) between 1 and 5000),
    context     jsonb not null default '{}'::jsonb,
    user_agent  text
);

comment on table public.feedback is
    'In-app "Send feedback" submissions (legal/feedback.html). Written only by the submit-feedback Edge Function; RLS-locked to service role.';

create index idx_feedback_created_at on public.feedback (created_at desc);

alter table public.feedback enable row level security;

revoke all on table public.feedback from anon, authenticated;

commit;

-- =============================================================================
-- Кінець міграції 0010. / End of migration 0010.
-- =============================================================================
