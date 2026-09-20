# FloppaAC

FloppaAC is a lightweight server-side anticheat for Paper and Spigot. It is written in plain Java against the Bukkit API. It has no external dependencies and no packet listeners. The plugin ships as a single JAR: drop it into `plugins/` and restart the server.

## Status

The project is under active development on a live server. Thresholds are tuned against real cheat clients. Known defects: sensitivity of connection checks to unstable frame rates and partial blindness of some NoFall modes. Both are being corrected in subsequent releases.

## Properties

- 34 checks: movement 15, combat 8, player 11.
- Pure Bukkit API. No NMS and no packet libraries. A newer Minecraft version requires only a recompile against the matching paper-api.
- Violation level system. Every check collects points, VL decays over time and punishment starts only above the configured threshold.
- Graduated punishment: setback to legal ground, then kick. Thresholds live in `config.yml`.
- Tester mode: every flag is echoed to the flagged player with check name, VL, ping, TPS and the measurement behind the flag. `/floppaac verbose` shows suspicions below the flag threshold.
- Legal play first: movement checks pause under TPS 18.5, thresholds widen at high ping, joins, teleports and respawns grant short exemptions, vehicles and elytra are excluded from measurement.

## Checks

| Category | Checks |
|---|---|
| Movement | FlyA, FlyB, FlyGlide, GroundSpoofA, SpeedA, SpeedB, TimerA, JesusA, StepA, NoFallA, SpiderA, SprintSpoofA, PhaseA |
| FakeLag and network | FakeLagA (choke: gap plus burst), FakeLagB (attack inside a gap), FakeLagC (desync at low ping), LagGuard (connection fuse) |
| Combat | ReachA, KillAuraA (angle), KillAuraJ (rotation GCD), KillAuraBot (NPC verifier), MultiActions, AutoClickerA, VelocityA, CriticalsA |
| Player | ScaffoldA, FastBreakA, NukerA, BadPacketsA, NoSlowA, InventoryA, AntiAutoWeb, AutoTrap, XrayA (ore ratio and line of sight), DigReachA (dig range) |

## Commands

```
/floppaac alerts           toggle staff alerts
/floppaac verbose          suspicions below the flag threshold
/floppaac vl               your own VL per check
/floppaac debug            telemetry: ping, TPS, movement gap, burst, CPS
/floppaac status           engine state: checks, TPS, players
/floppaac clear <player>   reset VL, requires floppaac.admin
/floppaac reload           reload configuration
```

## Building

JDK 17 or newer is required. The build compiles against paper-api, the net.kyori adventure jars, bungeecord-chat, gson and guava, in the same versions the target server ships.

```bat
javac -nowarn -encoding UTF-8 --release 17 ^
  -cp "stub;paper-api.jar;adventure-*.jar;bungeecord-chat.jar;gson.jar;guava.jar" ^
  -d classes @sources.txt

copy plugin.yml config.yml classes\
jar cf FloppaAC.jar -C classes .
```

`sources.txt` lists every `.java` file under `src/`. The `stub/` directory provides minimal `org.jetbrains.annotations` replacements so the build passes without downloading dependencies.

Tests are plain Java, no JUnit:

```
javac -d test-classes test/FloppaTest.java
java -cp test-classes;classes;guava.jar FloppaTest
```

## Xray

Server-side code cannot observe what a client renders, so xray and freecam leave no signature in packets. Remove the advantage instead of detecting it: enable Paper anti-xray in `config/paper-world-defaults.yml` with engine-mode 2 and a hidden block list covering every valuable ore, including nether blocks. Clients must rejoin after the change because chunk caches persist across server restarts.

## License

MIT, see [LICENSE](LICENSE).
