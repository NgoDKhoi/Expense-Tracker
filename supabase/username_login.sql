-- Username login for Expense Tracker.
-- Run once in Supabase Dashboard -> SQL Editor. Safe to re-run.

-- One row per account: the unique username chosen at sign-up.
create table if not exists public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  username text not null unique,
  constraint username_format check (username ~ '^[a-z0-9_]{3,20}$')
);

-- No policies: the app never reads this table directly, only through the function below.
alter table public.profiles enable row level security;

-- Create the profile from the "username" the app sends in sign-up metadata.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if new.raw_user_meta_data ? 'username' then
    insert into public.profiles (id, username)
    values (new.id, lower(new.raw_user_meta_data ->> 'username'));
  end if;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- Username -> email, so the app can sign in with Supabase's email/password login.
-- Returns null when the username is free; sign-up uses that as the availability check.
-- Note: anyone who knows a username can look up its email (accepted for this project).
create or replace function public.email_for_username(p_username text)
returns text
language sql
stable
security definer
set search_path = ''
as $$
  select u.email
  from auth.users u
  join public.profiles p on p.id = u.id
  where p.username = lower(p_username);
$$;

revoke all on function public.email_for_username(text) from public;
grant execute on function public.email_for_username(text) to anon, authenticated;
