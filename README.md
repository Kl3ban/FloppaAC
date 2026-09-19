# FloppaAC

**FloppaAC** is a lightweight, server-side anticheat for Minecraft servers (Paper / Spigot),
written in pure Java against the Bukkit API only. Zero external dependencies,
zero packet listeners: the plugin ships as a single JAR you drop into `plugins/`
and restart the server.

> **Project status:** FloppaAC is not a perfect anticheat — it still has a fair
> number of bugs to fix. The project is under active development, detection
> thresholds are tuned on a live server, and some checks need more work. Known
> issues (false positives on unstable FPS, detection of some NoFall modes) are
> being corrected in subsequent releases.

## Features

- **31 checks** in four categories: movement (13), combat (8), player (9),
  connection fuse (1).
- **100% Bukkit API** — no NMS, packetevents or other library dependencies.
  Porting between MC versions comes down to compiling against a newer paper-api.
- **VL (violation level) system** — every check collects points, VL decays over
  time; punishment only after the configured threshold is exceeded.
- **Graduated punishments:** setback (return to legal ground) → kick; thresholds
  fully configurable in `config.yml`.
- **Built-in tester mode:** every flag is sent directly to the flagged player
  with the check name, VL, ping, TPS and the concrete measurement;
  `/floppaac verbose` shows suspicions below the flag threshold.
- **Legal players first:** movement checks pause at TPS < 18.5, thresholds relax
  at high ping, exemptions after join / teleport / respawn; vehicles and elytra
  are excluded from measurement.

## Checks

| Category | Checks |
|---|---|
| Movement | FlyA, FlyB, FlyGlide, GroundSpoofA, SpeedA, SpeedB, TimerA, JesusA, StepA, NoFallA, SpiderA, SprintSpoofA, PhaseA |
| FakeLag / network | FakeLagA (choke: gap + burst), FakeLagB (attack in gap), FakeLagC (desync at low ping), LagGuard (connection fuse, kick for extremely unstable links) |
| Combat | ReachA, KillAuraA (angle), KillAuraJ (rotation GCD), KillAuraBot (NPC verifier), MultiActions, AutoClickerA, VelocityA, CriticalsA |
| Player | ScaffoldA, FastBreakA, NukerA, BadPacketsA, NoSlowA, InventoryA/B, AntiAutoWeb, AutoTrap |

## Commands

```
/floppaac alerts    - toggle team alerts
/floppaac verbose   - suspicions below the flag threshold
/floppaac vl        - your own VL per check
/floppaac debug     - telemetry: ping, TPS, movement gap, burst, CPS
/floppaac status    - engine state (checks, TPS, players)
/floppaac clear <player> - reset VL (requires floppaac.admin)
/floppaac reload    - reload configuration
```

## Building

Requires JDK 17+ (the project is built on JDK 25). You need the matching
dependency JARs on the classpath: paper-api, all `net.kyori` (adventure) jars,
bungeecord-chat, gson and guava — the same versions the target server ships.

```bat
javac -nowarn -encoding UTF-8 --release 17 ^
  -cp "stub;paper-api.jar;adventure-*.jar;bungeecord-chat.jar;gson.jar;guava.jar" ^
  -d classes @sources.txt

copy plugin.yml config.yml classes\
jar cf FloppaAC.jar -C classes .
```

`sources.txt` is the list of all `.java` files under `src/`. The `stub/`
directory contains minimal `org.jetbrains.annotations` replacement classes so
the build passes without downloading dependencies.

Tests (plain Java, no JUnit):

```
javac -d test-classes test/FloppaTest.java
java -cp test-classes;classes;guava.jar FloppaTest
```

## Known limitations

- The anticheat is server-side: freecam and xray leave no signature in packets
  and cannot be detected (on your own server Paper's anti-xray removes the
  advantage instead).
- The "zero dependencies" rule means no tick-by-tick movement simulation as in
  Grim; checks rely on mathematical movement models (drag/accel), packet-gap
  statistics and rotation convergence.
- Some checks are too sensitive or too blind to certain legal playstyles —
  the roadmap priority is tuning thresholds rather than adding new checks.

## License

MIT — see [LICENSE](LICENSE).
