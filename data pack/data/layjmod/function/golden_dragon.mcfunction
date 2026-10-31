# layjmod:golden_dragon — Golden Dragon (8,000 Coins):
# 5x5 lava pit under every player + continuous lightning storm for 15s.
title @a subtitle {"text":"(8,000 Coins) Lava pit plus 15 seconds of lightning storm!","color":"gray","italic":true}
title @a title {"text":"Golden Dragon","color":"gold","bold":true}
execute as @a at @s run fill ~-2 ~-1 ~-2 ~2 ~-1 ~2 minecraft:lava
execute as @a at @s run summon minecraft:lightning_bolt ~ ~1 ~
scoreboard players set #layj_storm layj_storm 30
schedule function layjmod:storm/tick 10t append
