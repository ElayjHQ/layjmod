# layjmod:private_jet — Private Jet (4,888 Coins): spawn an instantly-aggroed Warden.
title @a subtitle {"text":"(4,888 Coins) A Warden spawns - and it does NOT like you!","color":"gray","italic":true}
title @a title {"text":"Private Jet","color":"dark_gray","bold":true}
execute as @a at @s run summon minecraft:warden ~1 ~ ~
execute as @a at @s run damage @e[type=minecraft:warden,limit=1,sort=nearest] 1 minecraft:mob_attack by @s
