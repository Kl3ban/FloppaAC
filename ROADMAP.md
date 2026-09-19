# ROADMAP FloppaAC (po 1.1.0)

## Lekcja z Grima (GrimAnticheat/Grim, struktura checkow)

Grim dzieli checki na: combat (Reach), groundspoof, movement,
prediction (OffsetHandler, symulacja ruchu tick w tick),
scaffolding, velocity. Sila Grima to predykcja pakietowa:
symuluje ruch gracza i porownuje z pakietami, zamiast samych progow.

## Co ma sens dla FloppaAC bez lamania zasady zero zaleznosci

1. GroundSpoofA: flaga onGround z pakietu vs realny grunt pod stopami.
   Bukkit daje player.isOnGround() z klienta oraz blok pod stopami.
   Rozjazd w dlugiej serii to klasyczny groundspoof. Niski koszt.
2. SprintSpoof / OmniSprint: sprint bokiem i do tylu bez spowolnienia.
   Serwer widzi kierunek ruchu i flage sprintu. Regula: sprint + strafe
   lub cofanie z pelna predkoscia w serii.
3. Lepszy Reach: uwzglednic interpolacje celu (predkosc celu dodaje luzu
   tylko gdy cel biegnie), zamiast plaskiego progu.
4. Statystyka FakeLag: histogram luk na gracza w debug, zeby tester
   widzial rozklad i dobieral progi pod siebie.

## Czego NIE robimy bez swiadomej decyzji

- PacketEvents i sledzenie transakcji: daje ping pakietowy i kolejnosc,
  ale lamie zasade zero zaleznosci i latwosc instalacji. Ewentualnie
  jako osobny modul FloppaAC-Packets, nigdy rdzen.
- Decoy boty i setbacki z telemetrii: ryzyko FP i smierci graczy,
  tylko za zgoda testera po fazie obserwacji.
