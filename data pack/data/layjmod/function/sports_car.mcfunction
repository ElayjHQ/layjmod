# layjmod:sports_car — Sports Car (7,000 Coins): seal every player in an obsidian box,
# detonate 4 End Crystals on the roof, then a short-fuse TNT inside finishes the job.
title @a subtitle {"text":"(7,000 Coins) Sealed in an obsidian box and detonated!","color":"gray","italic":true}
title @a title {"text":"Sports Car","color":"dark_red","bold":true}
execute as @a at @s run fill ~-1 ~-1 ~-1 ~1 ~3 ~1 minecraft:obsidian hollow
execute as @a at @s run summon minecraft:end_crystal ~1 ~3.5 ~1 {Tags:["layj_car_crystal"]}
execute as @a at @s run summon minecraft:end_crystal ~-1 ~3.5 ~-1 {Tags:["layj_car_crystal"]}
execute as @a at @s run summon minecraft:end_crystal ~1 ~3.5 ~-1 {Tags:["layj_car_crystal"]}
execute as @a at @s run summon minecraft:end_crystal ~-1 ~3.5 ~1 {Tags:["layj_car_crystal"]}
execute as @e[type=minecraft:end_crystal,tag=layj_car_crystal] run damage @s 1 minecraft:explosion
execute as @a at @s run summon minecraft:tnt ~ ~1 ~ {fuse:10}
