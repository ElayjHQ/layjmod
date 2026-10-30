# layjmod:money_gun — Money Gun (500 Coins): strip all equipped armor off every player.
title @a subtitle {"text":"(500 Coins) Your equipped armor is shot off!","color":"gray","italic":true}
title @a title {"text":"Money Gun","color":"dark_purple","bold":true}
execute as @a at @s run item modify entity @s armor.head layjmod:shatter
execute as @a at @s run item modify entity @s armor.chest layjmod:shatter
execute as @a at @s run item modify entity @s armor.legs layjmod:shatter
execute as @a at @s run item modify entity @s armor.feet layjmod:shatter
