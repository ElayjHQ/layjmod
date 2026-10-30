# layjmod:castle — Castle (10,000 Coins): spawn 3 Wardens, 2 Withers, and a raid party (Bad Omen V).
title @a subtitle {"text":"(10,000 Coins) 3 Wardens, 2 Withers and a raid party storm you!","color":"gray","italic":true}
title @a title {"text":"Castle","color":"light_purple","bold":true}
execute as @a at @s run summon minecraft:warden ~2 ~ ~ {Tags:["layj_castle"]}
execute as @a at @s run summon minecraft:warden ~-2 ~ ~1 {Tags:["layj_castle"]}
execute as @a at @s run summon minecraft:warden ~ ~ ~-2 {Tags:["layj_castle"]}
execute as @a at @s run summon minecraft:wither ~2 ~2 ~1
execute as @a at @s run summon minecraft:wither ~-2 ~2 ~-1
execute as @a at @s run effect give @s minecraft:bad_omen 60 4 true
execute as @a at @s run damage @e[type=minecraft:warden,tag=layj_castle] 1 minecraft:mob_attack by @s
