# layjmod:paper_crane — Paper Crane (150 Coins): spawn 3 Phantoms.
title @a subtitle {"text":"(150 Coins) Three phantoms descend upon you!","color":"gray","italic":true}
title @a title {"text":"Paper Crane","color":"gray","bold":true}
execute as @a at @s run summon minecraft:phantom ~2 ~6 ~
execute as @a at @s run summon minecraft:phantom ~-2 ~6 ~2
execute as @a at @s run summon minecraft:phantom ~ ~6 ~-2
