# layjmod:rose — Rose (1 Coin): spawn 3 zombies wearing golden helmets.
title @a subtitle {"text":"(1 Coin) Zombies in golden helmets emerge at your feet!","color":"gray","italic":true}
title @a title {"text":"Rose","color":"red","bold":true}
execute as @a at @s run summon minecraft:zombie ~1 ~ ~ {equipment:{head:{id:"minecraft:golden_helmet",count:1}},drop_chances:{head:1.0f}}
execute as @a at @s run summon minecraft:zombie ~-1 ~ ~ {equipment:{head:{id:"minecraft:golden_helmet",count:1}},drop_chances:{head:1.0f}}
execute as @a at @s run summon minecraft:zombie ~ ~ ~1 {equipment:{head:{id:"minecraft:golden_helmet",count:1}},drop_chances:{head:1.0f}}
