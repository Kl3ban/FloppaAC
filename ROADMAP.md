# FloppaAC Roadmap

## Reference structure

GrimAnticheat separates combat, groundspoof, movement, prediction and scaffolding. Its strength is packet prediction: it simulates player movement and compares the result with the packets instead of applying plain thresholds.

## Planned work inside the zero dependency rule

1. GroundSpoofA hardening: compare the packet ground flag with the real block under the feet. A long mismatch streak is classic groundspoof at low cost.
2. Sprint spoof: sprinting sideways or backwards at full speed without slowdown. The server sees movement direction and the sprint flag, a streak is the signature.
3. Reach refinement: account for target interpolation. Target speed adds leniency only when the target is running, instead of a flat threshold.
4. FakeLag statistics: a per player gap histogram in the debug command so the tester can see the distribution and tune thresholds.

## Rejected without a conscious decision

- packetevents and transaction tracking: packet ping and ordering, at the cost of the zero dependency rule and simple installation. A separate FloppaAC-Packets module is possible, never the core.
- decoy bots and telemetry driven setbacks: false positive risk and player deaths. Only with tester approval after an observation phase.
