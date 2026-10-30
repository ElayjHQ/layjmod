# layjmod:confetti — Confetti (100 Coins): break the current main-hand tool.
title @a subtitle {"text":"(100 Coins) Your main-hand tool shatters into confetti!","color":"gray","italic":true}
title @a title {"text":"Confetti","color":"white","bold":true}
execute as @a at @s run item modify entity @s weapon.mainhand layjmod:shatter
