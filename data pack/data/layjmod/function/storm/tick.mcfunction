# layjmod:storm/tick — helper for layjmod:golden_dragon lightning storm.
# Runs every 10 ticks, 30 times (15s). Ends itself when the counter runs dry.
execute if score #layj_storm layj_storm matches 1.. as @a at @s run summon minecraft:lightning_bolt ~-1 ~1 ~
execute if score #layj_storm layj_storm matches 1.. as @a at @s run summon minecraft:lightning_bolt ~1 ~1 ~-1
execute if score #layj_storm layj_storm matches 1.. run scoreboard players remove #layj_storm layj_storm 1
execute if score #layj_storm layj_storm matches 1.. run schedule function layjmod:storm/tick 10t append
