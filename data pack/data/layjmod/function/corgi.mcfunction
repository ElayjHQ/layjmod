# layjmod:corgi — Corgi (299 Coins): spawn 5 furious, speed-buffed wolves that turn on the players.
title @a subtitle {"text":"(299 Coins) Five furious speed-wolves charge!","color":"gray","italic":true}
title @a title {"text":"Corgi","color":"blue","bold":true}
execute as @a at @s run summon minecraft:wolf ~1 ~ ~1 {Tags:["layj_corgi"]}
execute as @a at @s run summon minecraft:wolf ~-1 ~ ~1 {Tags:["layj_corgi"]}
execute as @a at @s run summon minecraft:wolf ~1 ~ ~-1 {Tags:["layj_corgi"]}
execute as @a at @s run summon minecraft:wolf ~-1 ~ ~-1 {Tags:["layj_corgi"]}
execute as @a at @s run summon minecraft:wolf ~ ~ ~2 {Tags:["layj_corgi"]}
execute as @a at @s run effect give @e[type=minecraft:wolf,tag=layj_corgi] minecraft:speed infinite 2 true
execute as @e[type=minecraft:wolf,tag=layj_corgi] run damage @s 1 minecraft:mob_attack by @p[limit=1,sort=nearest]
