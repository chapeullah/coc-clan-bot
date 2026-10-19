SELECT 
	id,
	state,
	clan_name,
	team_size,
	attacks_per_member AS apm,
	clan_attacks AS attacks,
	clan_stars AS stars,
	opponent_clan_stars AS opponent_stars,
	clan_destruction_percentage AS destr_percentage
FROM wars;