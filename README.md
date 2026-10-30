# LayjMod

Fabric mod (MC 26.3) that runs a small HTTP API on the server so a website can
send console commands remotely. Ships together with the Node.js website in
`webapp/`.

## Layout

- `src/main/java/dev/layjmod/` — the mod
  - `LayjMod.java` — entrypoint, starts the API on server started, stops it on shutdown
  - `api/LayjConfig.java` — reads/writes `config/layjmod.properties`
  - `api/LayjApiServer.java` — the HTTP endpoints
- `webapp/` — Node.js website (Express proxy + console UI)

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

## Local test server (already set up)

- Folder: `%TEMP%\opencode-layjmcsrv` (Fabric server, port 25566, offline mode)
- `mods/` contains `layjmod0.0.1.jar` + `fabric-api.jar` (0.165.0+26.3)
- Start: `java -Xmx2G -jar fabric-server.jar nogui`
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
