-- Cloud backup / multi-device sync for Expense Tracker.
-- Run once in Supabase Dashboard -> SQL Editor. Safe to re-run.
--
-- Mirrors the Room tables (wallet_table, categories, transactions) per user.
-- id = the row's uuid in the app. Default rows use the same fixed uuid on every device,
-- so the primary key is (user_id, id) and two users never collide.
-- updated_at = client time in epoch millis (newer edit wins).
-- synced_at = server time, set by trigger; the app pulls rows with synced_at > its last cursor.
-- Rows are never hard-deleted by the app: deleted = true is synced like any other edit.

create table if not exists public.wallets (
  user_id uuid not null default auth.uid() references auth.users (id) on delete cascade,
  id uuid not null,
  name text,
  initial_balance double precision not null default 0,
  color_hex text,
  icon_name text,
  updated_at bigint not null,
  deleted boolean not null default false,
  synced_at timestamptz not null default clock_timestamp(),
  primary key (user_id, id)
);

create table if not exists public.categories (
  user_id uuid not null default auth.uid() references auth.users (id) on delete cascade,
  id uuid not null,
  name text,
  type text,
  color_hex text,
  icon_name text,
  updated_at bigint not null,
  deleted boolean not null default false,
  synced_at timestamptz not null default clock_timestamp(),
  primary key (user_id, id)
);

-- category is the category NAME (same as the app). image_uri is not synced yet: it is a path on the phone.
create table if not exists public.transactions (
  user_id uuid not null default auth.uid() references auth.users (id) on delete cascade,
  id uuid not null,
  wallet_id uuid not null,
  amount double precision not null,
  type text not null check (type in ('EXPENSE', 'INCOME')),
  category text,
  title text,
  note text,
  occurred_at bigint,
  updated_at bigint not null,
  deleted boolean not null default false,
  synced_at timestamptz not null default clock_timestamp(),
  primary key (user_id, id),
  foreign key (user_id, wallet_id) references public.wallets (user_id, id) on delete cascade
);

create index if not exists wallets_user_synced on public.wallets (user_id, synced_at);
create index if not exists categories_user_synced on public.categories (user_id, synced_at);
create index if not exists transactions_user_synced on public.transactions (user_id, synced_at);

-- Stamp synced_at with the server clock, and keep the newer row when an older edit arrives late
-- (e.g. a phone that was offline pushes after another phone already saved a newer version).
-- clock_timestamp(), not now(): now() is the transaction start and could fall behind a cursor.
-- shortcut: a row committed slightly after a pull can still get synced_at below that pull's cursor;
-- the app should pull from (cursor - a few seconds). Upserts are idempotent, so the overlap is harmless.
create or replace function public.sync_before_write()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if tg_op = 'UPDATE' and new.updated_at < old.updated_at then
    return null; -- skip: the stored row is newer
  end if;
  new.synced_at := clock_timestamp();
  return new;
end;
$$;

drop trigger if exists wallets_sync_before_write on public.wallets;
create trigger wallets_sync_before_write
  before insert or update on public.wallets
  for each row execute function public.sync_before_write();

drop trigger if exists categories_sync_before_write on public.categories;
create trigger categories_sync_before_write
  before insert or update on public.categories
  for each row execute function public.sync_before_write();

drop trigger if exists transactions_sync_before_write on public.transactions;
create trigger transactions_sync_before_write
  before insert or update on public.transactions
  for each row execute function public.sync_before_write();

-- Each signed-in user sees and writes only their own rows. No delete policy: the app soft-deletes.
alter table public.wallets enable row level security;
alter table public.categories enable row level security;
alter table public.transactions enable row level security;

drop policy if exists "own rows: select" on public.wallets;
drop policy if exists "own rows: insert" on public.wallets;
drop policy if exists "own rows: update" on public.wallets;
create policy "own rows: select" on public.wallets for select to authenticated using (user_id = auth.uid());
create policy "own rows: insert" on public.wallets for insert to authenticated with check (user_id = auth.uid());
create policy "own rows: update" on public.wallets for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

drop policy if exists "own rows: select" on public.categories;
drop policy if exists "own rows: insert" on public.categories;
drop policy if exists "own rows: update" on public.categories;
create policy "own rows: select" on public.categories for select to authenticated using (user_id = auth.uid());
create policy "own rows: insert" on public.categories for insert to authenticated with check (user_id = auth.uid());
create policy "own rows: update" on public.categories for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

drop policy if exists "own rows: select" on public.transactions;
drop policy if exists "own rows: insert" on public.transactions;
drop policy if exists "own rows: update" on public.transactions;
create policy "own rows: select" on public.transactions for select to authenticated using (user_id = auth.uid());
create policy "own rows: insert" on public.transactions for insert to authenticated with check (user_id = auth.uid());
create policy "own rows: update" on public.transactions for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

-- Guests (anon key without a session) get no access at all.
revoke all on public.wallets, public.categories, public.transactions from anon;
