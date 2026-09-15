-- Drop profiles.sound_enabled: dead column. The client never synced it (ProfileDto/
-- ProfileUpdateDto in SupabaseAuthRepository.kt never included it — AppSettings.soundEnabled
-- was purely local, multiplatformSettings-backed) and the SoundPlayer feature it backed has been
-- removed from the app entirely.
alter table public.profiles drop column if exists sound_enabled;
