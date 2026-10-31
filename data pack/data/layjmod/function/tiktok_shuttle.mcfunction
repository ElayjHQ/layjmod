# layjmod:tiktok_shuttle — TikTok Shuttle (20,000 Coins): break the bedrock below the player,
# then after 2 ticks teleport them 100 blocks down into the Void.
title @a subtitle {"text":"(20,000 Coins) Bedrock below is broken - straight into the Void!","color":"gray","italic":true}
title @a title {"text":"TikTok Shuttle","color":"black","bold":true}
execute as @a at @s run fill ~ ~-1 ~ ~ ~-3 ~ minecraft:air
schedule function layjmod:shuttle/drop 2t append
