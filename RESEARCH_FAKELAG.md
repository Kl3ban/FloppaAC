# RESEARCH: FakeLag in open-source clients (2026-09-08)

## BleachHack (BleachDev/BleachHack, 795 stars)

`org.bleachhack.module.mods.FakeLag`: queues ALL `PlayerMoveC2SPacket`
(position, rotation and ground) and drops them. On release it sends
only packets with position data (look-only packets are discarded).
Attacks, keepalives and transactions flow normally the whole time.
Modes: Always (holds up to a second limit, by default no limit)
and Pulse (release every X seconds, by default every 1 s).

Server-side signature: full movement gap, stable ping (keepalive
answered), attacks during the gap, then a position burst.

## MoonLight (randomguy3725/MoonLight)

`features/modules/impl/exploit/FakeLag.java`: PingSpoof with Min/Max MS
delay (default 200/200 ms, range up to 5000 ms).
Flushes immediately on attack and on receiving damage (hurtTime > 0).

Signature: shorter gaps around 200 ms, attacks always instant.
Conclusion: the 300 ms gap threshold in FakeLagA MISSES the default
MoonLight setup. A counter of repeated 180-250 ms gaps with bursts
is needed.

## Remaining implementations to check

- LiquidbouncePlus-Reborn: `modules/player/FakeLag.kt`
- LuminaClient: `module/impl/movement/FakeLag.java`
- NightX-Client: `module/impl/exploit/FakeLag.kt`
- LWK (CS cheat, different context, skip)

## Conclusions for FloppaAC

1. FakeLagB (attack inside a gap at stable ping) is the most reliable
   signal, because every variant lets attacks through instantly.
2. FakeLagA needs a second threshold for short repeated gaps
   (180-250 ms, at least 4-5 episodes per window) to catch
   MoonLight at 200 ms.
3. LagGuard (kick at 6 gaps of 900 ms per minute) can kick the cheater
   with a lag message instead of a cheat flag. When FakeLag VL grows,
   LagGuard should back off.
4. Pulse every 1 s gives 60 gaps per minute. The episode counter must
   survive that without false positives on legit play (ping and TPS
   as gates).
