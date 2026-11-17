-- Leaderboard profiles for Expense Tracker Gamification (Phase 4.3b).
-- Run once in Supabase Dashboard -> SQL Editor. Safe to re-run.

create table if not exists public.leaderboard_profiles (
  user_id uuid primary key default auth.uid() references auth.users (id) on delete cascade,
  display_name text not null,
  username text not null,
  streak_days integer not null default 0,
  total_safe_days integer not null default 0,
  stage text not null default 'BABY',
  updated_at timestamptz not null default clock_timestamp()
);

create index if not exists leaderboard_profiles_streak_idx on public.leaderboard_profiles (streak_days desc, total_safe_days desc);

alter table public.leaderboard_profiles enable row level security;

drop policy if exists "leaderboard: read all authenticated" on public.leaderboard_profiles;
drop policy if exists "leaderboard: insert own" on public.leaderboard_profiles;
drop policy if exists "leaderboard: update own" on public.leaderboard_profiles;

create policy "leaderboard: read all authenticated" on public.leaderboard_profiles
  for select to authenticated using (true);
create policy "leaderboard: insert own" on public.leaderboard_profiles
  for insert to authenticated with check (user_id = auth.uid());
create policy "leaderboard: update own" on public.leaderboard_profiles
  for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
