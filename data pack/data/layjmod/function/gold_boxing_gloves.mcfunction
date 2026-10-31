# layjmod:gold_boxing_gloves — Gold Boxing Gloves (10 Coins) [EVIL]: -1 full heart of max health.
title @a subtitle {"text":"(10 Coins) Max Health -1 full heart!","color":"gray","italic":true}
title @a title {"text":"Gold Boxing Gloves","color":"dark_red","bold":true}
execute as @a at @s run attribute @s minecraft:max_health modifier remove layjmod:gold_boxing_gloves
execute as @a at @s run attribute @s minecraft:max_health modifier add layjmod:gold_boxing_gloves -2 add_value
