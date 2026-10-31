# layjmod:cap — Cap (99 Coins): spawn a Charged Creeper.
title @a subtitle {"text":"(99 Coins) A charged creeper rises from the ground!","color":"gray","italic":true}
title @a title {"text":"Cap","color":"yellow","bold":true}
execute as @a at @s run summon minecraft:creeper ~1 ~ ~ {powered:1b}
