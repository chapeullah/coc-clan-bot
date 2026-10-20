BEGIN;

ALTER TABLE public.clans
    ADD COLUMN IF NOT EXISTS member_list_synced_at timestamp(6) with time zone;

ALTER TABLE public.members
    ADD COLUMN IF NOT EXISTS joined_after timestamp(6) with time zone;

ALTER TABLE public.raid_season_clan_member_snapshots
    ADD COLUMN IF NOT EXISTS joined_at timestamp(6) with time zone;

ALTER TABLE public.raid_season_clan_member_snapshots
    ADD COLUMN IF NOT EXISTS joined_after timestamp(6) with time zone;

ALTER TABLE public.raid_season_clan_member_snapshots
    ADD COLUMN IF NOT EXISTS role varchar(255);

ALTER TABLE public.raid_season_clan_member_snapshots
    ADD COLUMN IF NOT EXISTS clan_type varchar(255);

ALTER TABLE public.raid_season_clan_member_snapshots
    ADD COLUMN IF NOT EXISTS participation_eligible boolean;

ALTER TABLE public.raid_season_clan_member_snapshots
    ADD COLUMN IF NOT EXISTS participation_opportunity_observed boolean;

COMMIT;
