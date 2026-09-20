# FakeLag research in open source clients

Date: 2026-09-08. Sources: public repositories.

## BleachHack (BleachDev/BleachHack)

`org.bleachhack.module.mods.FakeLag` queues every `PlayerMoveC2SPacket` (position, rotation, ground) and drops it. On release only packets with position data are sent, look only packets are discarded. Attacks, keepalives and transactions flow normally. Modes: Always, holds packets up to a configurable limit, default unlimited, and Pulse, releases on an interval, default 1 s.

Server signature: full movement gap, stable ping, attacks during the gap, position burst after the gap.

## MoonLight (randomguy3725/MoonLight)

`features/modules/impl/exploit/FakeLag.java` implements ping spoof with a minimum and maximum delay, default 200 ms, range up to 5000 ms. The queue flushes on attack and on damage.

Signature: repeated gaps around 200 ms, attacks always instant. The 300 ms gap threshold of FakeLagA misses this setup. A counter for repeated 180 to 250 ms gaps with bursts is required.

## Remaining implementations to inspect

- LiquidbouncePlus-Reborn, `modules/player/FakeLag.kt`
- LuminaClient, `module/impl/movement/FakeLag.java`
- NightX-Client, `module/impl/exploit/FakeLag.kt`
- LWK, CS context, skipped

## Conclusions for FloppaAC

1. FakeLagB, attack inside a gap at stable ping, is the most reliable signal. Every inspected variant lets attacks through instantly.
2. FakeLagA needs a second threshold for short repeated gaps, 180 to 250 ms, at least 4 episodes per window, to catch MoonLight at 200 ms.
3. LagGuard kicks at 6 gaps of 900 ms per minute and can remove a cheater with a lag message instead of a cheat flag. FakeLag VL must make LagGuard back off.
4. Pulse every second produces 60 gaps per minute. The episode counter has to survive that without false positives on legal play, gated by ping and TPS.
