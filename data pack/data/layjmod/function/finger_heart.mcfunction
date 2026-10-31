# layjmod:finger_heart — Finger Heart (5 Coins): spawn 2 creepers.
title @a subtitle {"text":"(5 Coins) Two creepers creep up on you...","color":"gray","italic":true}
title @a title {"text":"Finger Heart","color":"light_purple","bold":true}
execute as @a at @s run summon minecraft:creeper ~1 ~ ~
execute as @a at @s run summon minecraft:creeper ~-1 ~ ~
