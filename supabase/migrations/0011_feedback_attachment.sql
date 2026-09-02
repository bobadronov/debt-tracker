-- =============================================================================
-- DebtTracker — необов'язкове фото до відгуку (feedback.html → submit-feedback).
--
-- Обмеження БД: сам файл у Postgres НЕ зберігається. Він іде в Supabase Storage
-- (окреме об'єктне сховище, свій ліміт), а в рядку public.feedback лишається
-- тільки короткий шлях. Плюс жорсткі ліміти на кожному рівні: клієнт стискає
-- зображення, Edge Function відхиляє > 2.5 МБ, бакет — > 3 МБ.
--
-- DebtTracker — optional photo attached to feedback (feedback.html →
-- submit-feedback).
--
-- DB-limit note: the file itself is NOT stored in Postgres. It goes to Supabase
-- Storage (separate object store, its own quota); the public.feedback row keeps
-- only the short path. Hard caps at every layer: the client compresses the
-- image, the Edge Function rejects > 2.5 MB, the bucket rejects > 3 MB.
--
-- Бакет приватний і без policy — писати/читати може лише service role
-- (Edge Function). storage.objects уже має ввімкнений RLS.
-- The bucket is private with no policy — only the service role (Edge Function)
-- can write/read it. storage.objects already has RLS enabled.
-- =============================================================================

begin;

alter table public.feedback
    add column attachment_path text;

comment on column public.feedback.attachment_path is
    'Path in the private feedback-attachments Storage bucket, or null. The image bytes live in Storage, never in this table.';

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'feedback-attachments',
    'feedback-attachments',
    false,
    3145728, -- 3 MiB
    array['image/jpeg', 'image/png', 'image/webp']
)
on conflict (id) do nothing;

commit;

-- =============================================================================
-- Кінець міграції 0011. / End of migration 0011.
-- =============================================================================
