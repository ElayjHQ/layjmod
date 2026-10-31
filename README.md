# LayjMod

Fabric mod (MC 26.3) that runs a small HTTP API on the server so a website can
send console commands remotely. Ships together with the Node.js website in
`webapp/` and a Minecraft data pack (TikTok Live gift actions) in `data pack/`.

## Layout

- `src/main/java/dev/layjmod/` — the mod
  - `LayjMod.java` — entrypoint, starts the API on server started, stops it on shutdown
  - `api/LayjConfig.java` — reads/writes `config/layjmod.properties`
  - `api/LayjApiServer.java` — the HTTP endpoints
- `webapp/` — Node.js website (Express proxy + console UI)
- `data pack/` — Minecraft 26.3 data pack (`layjmod` gift functions), detailed below
- `data pack.zip` — same pack as a zip with `pack.mcmeta` at the zip root, ready for drag & drop import

## Mod config (auto-created on first server start)

`config/layjmod.properties`:

    port=8081
    bind-address=0.0.0.0
    token=<random UUID, regenerate by deleting the line and restarting>
    allow-origin=*

## API

Every call needs the header `Authorization: Bearer <token>`.

- `GET /api/status` -> `{ok, motd, version, uptimeSec, online, max, tps, players[]}`
- `POST /api/command` body `{"command": "list"}` (raw text body also accepted)
  -> `{ok, command, output}`; parse errors come back in `output`
- 401 wrong/no token, 404 unknown endpoint, 429 >120 requests/min per IP
- CORS: echoes the browser Origin (for the website)

Examples:

    curl http://127.0.0.1:8081/api/status -H "Authorization: Bearer <token>"
    curl -X POST http://127.0.0.1:8081/api/command -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d "{\"command\":\"list\"}"

Note: `say` broadcasts to players but returns an empty console output by design
(the broadcast path bypasses command sources in vanilla 26.3).

## Building the jar

    set JAVA_HOME=<JDK 25>
    gradlew.bat build

Result: `build/libs/layjmod0.0.1.jar` (no remapping needed on 26.3).

## TikTok gift data pack (`data pack/`)

Namespace `layjmod`, pack format `121.0` (MC 26.3). Every gift is its own
`.mcfunction` under `data pack/data/layjmod/function/`. Triggering a gift is a
one-liner through the API:

    curl -X POST http://127.0.0.1:8081/api/command -H "Authorization: Bearer <token>" --data-raw "function layjmod:rose"

Rules baked into every function: all gameplay commands run
`execute as @a at @s run ...` (every online player is hit), and each function
first sends a gray italic subtitle with the coin cost, then a bold colored main
title (subtitle first, then title).

| function | coins | effect |
| --- | --- | --- |
| `rose` | 1 | 3 zombies wearing golden helmets |
| `finger_heart` | 5 | 2 creepers |
| `rosa` | 10 | GOOD: max health +1 heart |
| `gold_boxing_gloves` | 10 | EVIL: max health -1 heart |
| `doughnut` | 30 | frozen in place 10s (Slowness 255) |
| `ice_cream` | 49 | Slowness II + Blindness for 15s |
| `cap` | 99 | charged creeper |
| `confetti` | 100 | main-hand tool destroyed |
| `hand_heart` | 100 | GOOD: totem of undying |
| `paper_crane` | 150 | 3 phantoms |
| `corgi` | 299 | 5 furious speed-boosted wolves |
| `money_gun` | 500 | equipped armor stripped |
| `galaxy` | 1,000 | wither spawns on the player |
| `private_jet` | 4,888 | instantly aggrod warden |
| `sports_car` | 7,000 | obsidian box + 4 end crystals detonated + short-fuse TNT inside |
| `golden_dragon` | 8,000 | 5x5 lava pit + 15s lightning storm |
| `castle` | 10,000 | 3 wardens + 2 withers + Bad Omen V |
| `tiktok_shuttle` | 20,000 | floor broken below player, then teleported to y=-100 (Void) |
| `lion` | 29,999 | grid of 100 primed TNT |
| `tiktok_universe` | 34,999 | full clear incl. ender chest + respawn anchor deleted + kill |

Extras in the pack:

- `layjmod:load` (tagged `minecraft:load`) — adds the `layj_storm` scoreboard and
  greets on load
- `layjmod:storm/tick` — helper chain that keeps the `golden_dragon` storm running 15s
- `layjmod:shuttle/drop` — helper that does the y=-100 teleport after 2 ticks
- `layjmod:shatter` (`item_modifier/shatter.json`) — set-count-0 modifier used to
  destroy a slot (confetti, money gun, and the universe wipe)

Installing into a world: copy the `data pack/` folder (or `data pack.zip`) into
`<world>/datapacks/`, then run `/reload`. Mind 26.3 quirks that already bit us:

- `item_modifier` files must have a **JSON object root** (registry elements); an
  array aborts the whole pack load. Multiple functions go inside
  `minecraft:sequence`.
- `/damage` requires a **single-entity** target; use
  `execute as @e[...] run damage @s ... by <single player>` instead.
- Directory names stay singular: `function/`, `loot_table/`, `item_modifier/`,
  `tags/function/`.

## Local test server (already set up)

- Folder: `%TEMP%\opencode-layjmcsrv` (Fabric server, port 25566, offline mode)
- `mods/` contains `layjmod0.0.1.jar` + `fabric-api.jar` (0.165.0+26.3)
- Start with the bundled JDK (no system Java is installed):
  `%TEMP%\opencode\jdk\jdk-25.0.5+8\bin\java.exe -Xmx2G -jar fabric-server.jar nogui`
- The gift data pack is already installed in `world/datapacks/data pack`
  (a copy of this repo's `data pack/`); after repo changes copy it over and send
  `reload` via the API
- The Fabric server command console is the API itself + the server log

## Website

    cd webapp
    run_website.cmd          (or: npm install; node server.js)

Opens `http://localhost:8080`. Proxy routes: `/api/targets`, `/api/status/:target`,
`/api/command/:target`. Tokens stay server-side in `webapp/servers.json`
(gitignored; created from `servers.example.json` on first run).

## Deploying to Ultra Servers

1. `gradlew.bat build` -> take `build/libs/layjmod0.0.1.jar`
2. Upload to the server's `mods/` folder (Ultra file manager / SFTP), plus the
   matching `fabric-api-0.165.0+26.3.jar` if the server does not have Fabric API yet
3. Open/allocate an extra TCP port in the Ultra panel (e.g. 8081)
4. Start the server once; it creates `config/layjmod.properties` and prints the token
5. Edit the config: set `port` to the port from step 3; keep/download the token
6. Restart the server; confirm the log shows `layjmod: API server listening on ...`
7. On the local PC: put `http://<ultra-ip>:<port>` + token into `webapp/servers.json`
   under the `ultra` target, then start the website and pick `ultra` in the dropdown

## Security notes (v1)

- Plain HTTP: the token travels unencrypted - fine for personal use; for remote
  deployment keep the token long and private
- The API has engine-level op permissions (level 4) - ANY token holder can run
  ANY command, including `stop`. Do not share the token.
