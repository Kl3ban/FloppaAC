# FloppaAC ROADMAP (post-1.1.0)

## Lesson from Grim (GrimAnticheat/Grim, check structure)

Grim splits checks into: combat (Reach), groundspoof, movement,
prediction (OffsetHandler, tick-by-tick movement simulation),
scaffolding, velocity. Grim's strength is packet prediction:
it simulates player movement and compares it against packets
instead of plain thresholds.

## What makes sense for FloppaAC without breaking the zero-dependency rule

1. GroundSpoofA: flag onGround from the packet vs real ground under the feet.
   Bukkit gives player.isOnGround() from the client plus the block below.
   A mismatch in a long streak is classic groundspoof. Low cost.
2. SprintSpoof / OmniSprint: sprinting sideways and backwards without
   slowdown. The server sees movement direction and the sprint flag.
   Rule: sprint + strafe or backwards movement at full speed in a streak.
3. Better Reach: account for target interpolation (target speed adds
   leniency only when the target is running) instead of a flat threshold.
4. FakeLag statistics: a per-player gap histogram in the debug command,
   so the tester can see the distribution and tune thresholds.

## What we do NOT do without a conscious decision

- PacketEvents and transaction tracking: gives packet ping and ordering,
  but breaks the zero-dependency rule and easy installation. Possibly
  a separate FloppaAC-Packets module, never the core.
- Decoy bots and telemetry-driven setbacks: risk of false positives and
  player deaths; only with tester approval after an observation phase.
