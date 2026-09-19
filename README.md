# FloppaAC

**FloppaAC** to lekki, server-side anticheat dla serwerów Minecraft (Paper / Spigot),
napisany w czystej Javie wyłącznie na Bukkit API. Zero zależności zewnętrznych,
zero packet-listenerów: plugin składa się z jednego JAR-a, który wystarczy wrzucić
do `plugins/` i zrestartować serwer.

> **Stan projektu:** FloppaAC nie jest antycheatem idealnym i zawiera sporo błędów
> do poprawy. Projekt jest w aktywnej fazie rozwoju, progi detekcji są strojone
> na żywym serwerze i część checków wymaga dopracowania, a niektóre znane problemy
> (fałszywe alarmy przy niestabilnym FPS, detekcja niektórych trybów NoFall) są
> korygowane w kolejnych wydaniach.

## Charakterystyka

- **31 checków** w czterech kategoriach: movement (13), combat (8), player (9),
  bezpiecznik połączenia (1).
- **100% Bukkit API** - brak zależności od NMS, packetevents czy innych bibliotek.
  Konwersja między wersjami MC sprowadza się do kompilacji z nowszym paper-api.
- **System VL (violation level)** - każdy check zbiera punkty, VL opada z czasem
  (decay), kara dopiero po przekroczeniu progu z konfiguracji.
- **Kary stopniowane:** setback (cofnięcie na legalny grunt) → kick; progi w pełni
  konfigurowalne w `config.yml`.
- **Wbudowany tryb testera:** każda flaga trafia bezpośrednio do flagowanego gracza
  z nazwą checka, VL, pingiem, TPS i konkretnym pomiarem; komenda `/floppaac verbose`
  pokazuje podejrzenia poniżej progu flagi.
- **Ochrona legalnych graczy jako priorytet:** wstrzymanie checków movement przy
  TPS < 18.5, luzniejsze progi przy wysokim pingu, exemptiony po wejściu /
  teleportacji / respawnie, pojazdy i elytra poza pomiarem.

## Checki

| Kategoria | Checki |
|---|---|
| Movement | FlyA, FlyB, FlyGlide, GroundSpoofA, SpeedA, SpeedB, TimerA, JesusA, StepA, NoFallA, SpiderA, SprintSpoofA, PhaseA |
| FakeLag / sieć | FakeLagA (choke: luka + burst), FakeLagB (atak w luce), FakeLagC (desync przy niskim pingu), LagGuard (bezpiecznik łącza, kick za skrajnie niestabilne połączenie) |
| Combat | ReachA, KillAuraA (kąt), KillAuraJ (GCD rotacji), KillAuraBot (weryfikacja botem NPC), MultiActions, AutoClickerA, VelocityA, CriticalsA |
| Player | ScaffoldA, FastBreakA, NukerA, BadPacketsA, NoSlowA, InventoryA/B, AntiAutoWeb, AutoTrap |

## Komendy

```
/floppaac alerts    - alerty ekipy włącz/wyłącz
/floppaac verbose   - podejrzenia poniżej progu flagi
/floppaac vl        - własne VL dla każdego checka
/floppaac debug     - telemetria: ping, TPS, luka ruchu, burst, CPS
/floppaac status    - stan silnika (checki, TPS, gracze)
/floppaac clear <gracz> - wyzeruj VL (wymaga floppaac.admin)
/floppaac reload    - przeładowanie konfiguracji
```

## Budowanie

Wymagany JDK 17+ (projekt budowany na JDK 25). Biblioteki do classpath są w `lib/`.

```bat
javac -nowarn -encoding UTF-8 --release 17 ^
  -cp "lib/paper-api.jar;lib/adventure-api.jar;lib/adventure-key-4.14.0.jar;lib/examination-api-1.3.0.jar;lib/bungeecord-chat-1.16-R0.4.jar;lib/gson-2.10.1.jar;lib/guava-32.1.2-jre.jar;stub" ^
  -d classes @sources.txt

copy plugin.yml config.yml classes\
jar cf FloppaAC.jar -C classes .
```

`sources.txt` to lista wszystkich plików `.java` z `src/`. Katalog `stub/`
zawiera minimalne klasy zastępcze `org.jetbrains.annotations`, żeby kompilacja
przechodziła bez pobierania zależności.

Testy (czysta Java, bez JUnit):

```
javac -d test-classes test/FloppaTest.java
java -cp test-classes FloppaTest
```

## Znane ograniczenia

- Antycheat jest server-side: freecam i xray nie mają sygnatury w pakietach
  i nie podlegają detekcji (na własnym serwerze radzi to Paper anti-xray).
- Zasada "zero zależności" oznacza brak symulacji ruchu tick-w-tick jak w Grim;
  checki opierają się na modelach matematycznych ruchu (drag/accel), statystyce
  luk pakietowych i konwergencji rotacji.
- Cześć checków bywa zbyt czuła lub zbyt ślepa na niektóre legalne styl gry -
  priorytetem roadmapy jest dopracowanie progów zamiast dokładania nowych checków.

## Licencja

Brak - do czasu decyzji autora.
