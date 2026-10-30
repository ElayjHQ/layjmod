# layjmod:tiktok_universe — TikTok Universe (34,999 Coins): the end times.
# Wipe the entire inventory (armor slots included), purge the ender chest,
# erase the bed/respawn anchor, then kill the player.
title @a subtitle {"text":"(34,999 Coins) Inventory wiped, respawn deleted, death!","color":"gray","italic":true}
title @a title {"text":"TikTok Universe","color":"dark_purple","bold":true}
execute as @a at @s run clear @s
execute as @a at @s run item modify entity @s weapon.mainhand layjmod:shatter
execute as @a at @s run item modify entity @s weapon.offhand layjmod:shatter
execute as @a at @s run item modify entity @s armor.head layjmod:shatter
execute as @a at @s run item modify entity @s armor.chest layjmod:shatter
execute as @a at @s run item modify entity @s armor.legs layjmod:shatter
execute as @a at @s run item modify entity @s armor.feet layjmod:shatter
execute as @a at @s run data merge entity @s {EnderItems:[]}
execute as @a at @s run data remove entity @s respawn
execute as @a at @s run kill @s
