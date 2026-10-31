# layjmod:rosa — Rosa (10 Coins) [GOOD]: +1 full heart of max health.
title @a subtitle {"text":"(10 Coins) Max Health +1 full heart!","color":"gray","italic":true}
title @a title {"text":"Rosa","color":"green","bold":true}
execute as @a at @s run attribute @s minecraft:max_health modifier remove layjmod:rosa
execute as @a at @s run attribute @s minecraft:max_health modifier add layjmod:rosa 2 add_value
