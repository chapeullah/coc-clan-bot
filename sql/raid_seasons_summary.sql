SELECT 
	id, 
	capital_total_loot, 
	defensive_reward, 
	offensive_reward,
	enemy_districts_destroyed AS disctrict_destroyed, 
	raids_completed, 
	state, 
	total_attacks AS attacks
FROM raid_seasons;