package pl.floppaac.check.movement;

import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.FakeLagMath;
import pl.floppaac.util.MoveUtil;
import pl.floppaac.util.PingUtil;

/**
 * FakeLag: nowy cheat czolowy dla FloppaAC.
 *
 * Jak dziala: klient celowo wstrzymuje pakiety pozycji na 200 do 1000 ms,
 * a pakiety ataku i keepalive puszcza dalej. Serwer widzi postac w starym
 * miejscu (tam jest hitbox), a u oszusta na ekranie wszystko wyglada
 * plynnie. To NIE jest Blink (Blink blokuje wszystko i podnosi ping)
 * i NIE podnosi pingu, wiec klasyczne progi pingowe go nie lapia.
 * Potwierdzone na kodzie BleachHack (Always/Pulse) oraz MoonLight
 * (PingSpoof 200 ms z flushem przy ataku i obrazeniach).
 *
 * Fizyka rozrodniajaca legalny lag od fakelaga (1.7.7):
 *  - zamarzniecie klienta (FPS, stall) NIE generuje pakietow; po
 *    wznowieniu wchodzi JEDEN wielki skok rowny oczekiwanej drogi,
 *  - fakelag wstrzymuje pakiety WYGENEROWANE przez dzialajacy klient;
 *    flush to wiele pakietow o normalnych deltach i maly skok wjazdu.
 * Dlatego wykrycie wymaga burstu PO luce, a duzy skok wjazdu TYLKO
 * uniewaznia epizod (nie jest sygnalem fakelaga). Reszta: ekran
 * nie staje, wiec oddech/rozejrzenie sie nie miesci w oknie burstu.
 *
 * Trzy sygnaly FloppaAC, kazdy z wlasnym VL:
 * FakeLagA (choke): luka ruchu plus burst po luce przy stabilnym pingu.
 * FakeLagB (desync): atak w trakcie luki ruchu, hitbox nie tam gdzie skin.
 * FakeLagC (rozjazd): jitter ruchu wysoki przy niskim pingu / desync czasow.
 *
 * Prawdziwy lag jest wykluczony: ping powyzej 220 ms, TPS ponizej 18.5,
 * teleport, respawn, dolaczenie i pojazd kasuja stan. Serie wygasaja
 * po 60 s bez epizodu, wiec dwie odlegle czkawki nie lacza sie we flage.
 */
public class FakeLagCheck extends Check {

    /** Serie bez epizodu wygasaja po tym czasie. */
    private static final long STREAK_EXPIRY_MS = 60000L;

    public FakeLagCheck(FloppaAC plugin) {
        super(plugin, "FakeLagA", CheckType.MOVEMENT);
    }

    /** Flaga FakeLag ze znacznikiem czasu dla LagGuarda. */
    private boolean flagLag(String vlName, Player player, PlayerData data, String details) {
        data.lastFakeLagMs = System.currentTimeMillis();
        return flagAs(vlName, player, data, details);
    }

    /** Podejrzenie FakeLag ze znacznikiem czasu dla LagGuarda. */
    private void suspLag(Player player, PlayerData data, String details) {
        data.lastFakeLagMs = System.currentTimeMillis();
        suspicious(player, data, details);
    }

    /**
     * Wywolywane przy kazdym ruchu ze zmiana pozycji.
     * @param gapMs przerwa od poprzedniego ruchu pozycyjnego w ms
     */
    public void handleMove(Player player, PlayerData data, long gapMs) {
        if (data.movementExempt()) {
            // Karencja po naszym pushbacku NIE zeruje serii.
            return;
        }
        if (MoveUtil.canFly(player) || MoveUtil.inVehicle(player)) {
            data.chokeStreak = 0;
            data.shortChokeStreak = 0;
            return;
        }
        long now = System.currentTimeMillis();
        int ping = PingUtil.getPing(player, data);
        double tps = plugin.getTpsMonitor().getTps();
        if (ping > 220 || tps < 18.5) {
            data.chokeStreak = 0;
            data.shortChokeStreak = 0;
            return;
        }
        // Surowa przerwa (takze < 180 ms): baza azylu recovery dla C.
        // Stall klienta krotki niz prog choke nie zapisuje sie nigdzie
        // indziej, a powrot z niego daje mikro-odstepy wygladajace jak
        // desync czasow.
        data.lastRawGapMs = gapMs;
        data.lastRawGapAt = now;

        // Bezruch to nie choke: luka powyzej 2 s to AFK, stanie
        // w miejscu albo dlugi stall, nie wstrzymanie pakietow.
        if (gapMs > 2000L) {
            data.chokeStreak = 0;
            data.shortChokeStreak = 0;
            data.lastMoveGapMs = 0L;
        }

        // Wygasanie serii po minucie ciszy.
        if (now - data.lastChokeMs > STREAK_EXPIRY_MS) {
            data.chokeStreak = 0;
        }
        if (now - data.lastShortChokeMs > STREAK_EXPIRY_MS) {
            data.shortChokeStreak = 0;
        }

        long gapThreshold = (long) cfgInt("min-gap-ms", 300);
        int burstNeeded = cfgInt("min-burst", 6);

        // --- klasyfikacja epizodu: luka + skok wjazdu ---
        double skip = 0.0;
        boolean skipKnown = false;
        if (data.lastLoc != null && data.lastLoc.getWorld() != null
                && player.getWorld().equals(data.lastLoc.getWorld())) {
            skip = Math.hypot(
                    player.getX() - data.lastLoc.getX(),
                    player.getZ() - data.lastLoc.getZ());
            skipKnown = true;
        }
        boolean clientFreeze = skipKnown
                && FakeLagMath.isClientFreeze(skip, gapMs);

        if (gapMs >= 180L && clientFreeze) {
            // Zamarzniecie klienta: zero pakietow, powrot jednym
            // wielkim skokiem. Czysci stan epizodu; to NIE jest cheater.
            data.chokeStreak = 0;
            data.shortChokeStreak = 0;
            data.lastMoveGapMs = 0L;
            data.lastFreezeMs = now;
            return;
        }

        if (gapMs >= gapThreshold) {
            data.lastChokeMs = now;
            if (skipKnown && skip < 0.05 && data.lastHorizontalDist < 0.05) {
                // Powrot po staniu w miejscu: nie bylo ruchu,
                // wiec nie bylo czego wstrzymywac. Nie epizod.
                data.lastMoveGapMs = 0L;
            } else {
                data.lastMoveGapMs = gapMs;
                data.chokeStreak++;
                if (data.chokeStreak == 1) {
                    suspLag(player, data, String.format(
                            "move gap %dms ping=%d (observation)", gapMs, ping));
                }
            }
        } else if (gapMs >= 180L) {
            // Krotka luka w stylu MoonLight 200 ms. Osobny licznik.
            // Po staniu w miejscu nie liczy sie.
            if (!(skipKnown && skip < 0.05 && data.lastHorizontalDist < 0.05)) {
                data.lastShortChokeMs = now;
            }
        }

        // Burst po luce: ruchy z ostatnich 200 ms.
        int burst = FakeLagMath.burstInWindow(data.moveTimes, now, 200L);
        data.lastBurstSize = burst;

        // Azyl po zamarznieciu klienta: epizody z tej fazy nie licza sie
        // do flagi (burst po freeze to rowniez naturalna odpowiedz).
        boolean freezeAsylum = now - data.lastFreezeMs < 2500L;

        if (!freezeAsylum && data.lastMoveGapMs >= gapThreshold && burst >= burstNeeded) {
            if (data.chokeStreak >= cfgInt("episodes", 2)) {
                flagLag("FakeLagA", player, data, String.format(
                        "choke gap=%dms burst=%d/200ms ping=%d tps=%.1f",
                        data.lastMoveGapMs, burst, ping, tps));
                data.chokeStreak = 0;
                data.lastMoveGapMs = 0L;
            } else {
                suspLag(player, data, String.format(
                        "burst after gap %d/200ms (episode %d)", burst, data.chokeStreak));
            }
        } else if (!freezeAsylum && FakeLagMath.isShortChoke(gapMs, burst, ping)) {
            // Krotkie powtarzalne luki 180-300 ms z burstem 4+.
            data.shortChokeStreak++;
            int need = cfgInt("short-episodes", 5);
            if (data.shortChokeStreak >= need) {
                flagLag("FakeLagA", player, data, String.format(
                        "krotki choke gap=%dms burst=%d/200ms x%d ping=%d",
                        gapMs, burst, data.shortChokeStreak, ping));
                data.shortChokeStreak = 0;
            } else if (data.shortChokeStreak == 2) {
                suspLag(player, data, String.format(
                        "short gaps %dms (episode %d)", gapMs, data.shortChokeStreak));
            }
        }

        // Sygnal C: desync czasow dostarczania ruchu (plynne mikrodawki
        // zamiast tickow 50 ms) albo jitter przy pingu bliskim zero.
        long median = FakeLagMath.medianInterval(data.moveTimes, now, 1000L);
        double driftPct = 0.0;
        if (median > 0L && median < 50L) {
            driftPct = (1.0 - ((double) median / 50.0)) * 100.0;
        }
        int movesLastSec = FakeLagMath.burstInWindow(data.moveTimes, now, 1000L);
        // pingEma (EMA 0.3) zamiast probki: pojedynczy niski odczyt
        // nie otwiera okna na sygnal desyncu.
        // Azyl recovery dla C: po kazdej luce (choke, krotki choke albo
        // surowa przerwa >= 100 ms) klient goni zaleglosci
        // mikro-odstepami - legalny powrot po stallu (FPS), nie desync.
        // C oceniamy tylko na rownym strumieniu bez epizodow od 2.5 s.
        boolean recoveryAsylum = freezeAsylum
                || now - data.lastChokeMs < 2500L
                || now - data.lastShortChokeMs < 2500L
                || (data.lastRawGapMs >= 100L && now - data.lastRawGapAt < 2500L);
        if (!recoveryAsylum && FakeLagMath.isDesyncJitter(driftPct, median, movesLastSec, data.pingEma)) {
            if (now - data.lastDesyncFlagMs > 8000L) {
                data.lastDesyncFlagMs = now;
                flagLag("FakeLagC", player, data, String.format(
                        "timing-desync median=%dms moves/s=%d ping=%d drift=%.0f%%",
                        median, movesLastSec, ping, driftPct));
            } else {
                suspLag(player, data, String.format(
                        "timing-desync median=%dms drift=%.0f%%", median, driftPct));
            }
        }
    }

    /**
     * Wywolywane przy kazdym trafieniu. Atak w luce ruchu to desync hitboxa.
     * @return true gdy trafienie poszlo z desynchronizowanego hitboxa
     *         (atakujacy sie ruszal, a ruch byl wstrzymany) - listener
     *         anuluje obrazenia zamiast tylko flagowac.
     */
    public boolean handleAttack(Player player, PlayerData data) {
        if (data.movementExempt() || MoveUtil.canFly(player) || MoveUtil.inVehicle(player)) {
            return false;
        }
        long now = System.currentTimeMillis();
        long sinceMove = now - data.lastMoveMs;
        if (sinceMove < 0L) {
            return false;
        }
        int ping = PingUtil.getPing(player, data);
        double tps = plugin.getTpsMonitor().getTps();
        if (ping > 220 || tps < 18.5) {
            return false;
        }
        long attackGap = (long) cfgInt("attack-gap-ms", 250);
        if (FakeLagMath.isAttackDuringGap(sinceMove, ping) && sinceMove >= attackGap) {
            // Kampienie (stanie i bicie) to nie desync hitboxa.
            if (data.lastHorizontalDist < 0.03) {
                suspLag(player, data, String.format(
                        "attack %dms after move (camping?)", sinceMove));
                return false;
            }
            // Gracz stoi w miejscu i bije: legalne. Wymagamy wyraznego
            // ruchu wczesniej, zeby nie karac kampienia.
            int recentMoves = FakeLagMath.burstInWindow(data.moveTimes, now, 2000L);
            if (recentMoves >= 6) {
                return flagLag("FakeLagB", player, data, String.format(
                        "attack in move gap %dms ping=%d tps=%.1f",
                        sinceMove, ping, tps));
            }
            suspLag(player, data, String.format(
                    "attack %dms after move (camping?)", sinceMove));
        }
        return false;
    }
}
