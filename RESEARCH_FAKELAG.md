# RESEARCH: FakeLag w open-source klientach (2026-09-08)

## BleachHack (BleachDev/BleachHack, 795 gwiazdek)

`org.bleachhack.module.mods.FakeLag`: kolejkuje WSZYSTKIE
`PlayerMoveC2SPacket` (pozycja, obrot i ground) i je kasuje.
Przy zwolnieniu wysyla tylko pakiety z pozycja (look-only odrzuca).
Ataki, keepalive i transakcje ida normalnie przez caly czas.
Tryby: Always (trzyma do limitu sekund, domyslnie bez limitu)
oraz Pulse (zwolnij co X sekund, domyslnie co 1 s).

Sygnatura server-side: pelna luka ruchu, stabilny ping (keepalive
odpowiada), ataki w trakcie luki, potem burst pozycji.

## MoonLight (randomguy3725/MoonLight)

`features/modules/impl/exploit/FakeLag.java`: PingSpoof z opoznieniem
Min/Max MS (domyslnie 200/200 ms, zakres do 5000 ms).
Flush natychmiast przy ataku i przy otrzymaniu obrazen (hurtTime > 0).

Sygnatura: krotsze luki okolo 200 ms, ataki zawsze natychmiastowe.
Wniosek: prog luki 300 ms w FakeLagA PRZEGAPIA domyslny MoonLight.
Potrzebny licznik powtarzalnych luk 180-250 ms z burstami.

## Pozostale implementacje do sprawdzenia

- LiquidbouncePlus-Reborn: `modules/player/FakeLag.kt`
- LuminaClient: `module/impl/movement/FakeLag.java`
- NightX-Client: `module/impl/exploit/FakeLag.kt`
- LWK (cheat CS, inny kontekst, pominac)

## Wnioski dla FloppaAC

1. FakeLagB (atak w luce przy stabilnym pingu) to najpewniejszy sygnal,
   bo wszystkie warianty przepuszczaja ataki natychmiast.
2. FakeLagA potrzebuje drugiego progu dla krotkich powtarzalnych luk
   (180-250 ms, minimum 4-5 epizodow w oknie), zeby lapac MoonLight 200 ms.
3. LagGuard (kick przy 6 lukach 900 ms na minute) moze kopac cheatera
   z komunikatem o lagi zamiast flaga cheata. Gdy VL FakeLag rosnie,
   LagGuard powinien ustapic.
4. Pulse co 1 s daje 60 luk na minute. Licznik epizodow musi to wytrzymac
   bez falszywych flag na legalnej grze (ping i TPS jako bramki).
