package pl.floppaac.check.combat;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;

/**
 * Multitool (jak MultiActions u Grima).
 * MultiA: cios mieczem z podniesiona reka (tarcza w gorze, jedzenie,
 * celowanie z luku). Legalny klient najpierw opuszcza reke, potem
 * bije. Kazde trafienie z podniesiona reka to ghost (obrazenia
 * anulowane od pierwszego ciosu), flaga VL w serii 3.
 * MultiB: machniecie z podniesiona reka. Machniecie tez wymaga
 * opuszczenia reki. Wyjatek 500 ms po wyrzuceniu przedmiotu
 * (drop z animacja machniecia). Seria 3.
 *
 * Poprawka 1.8.0 (flick multitool): cheat puszcza pakiet release
 * na milisekundy przed atakiem i wraca do bloku, wiec probkowanie
 * isHandRaised tylko w chwili ataku widzi opuszczona reke.
 * Dlatego stan reki probkujemy na kazdym ruchu (handleMove) i atak
 * traktujemy jako multitool takze gdy ostatni zaobserwowany stan
 * to podniesiona reka bez przerwy na opuszczona. Legalne
 * puszczenie-bloku-then-atak zawsze przechodzi przez obserwowalny
 * stan opuszczony (czlowiek potrzebuje ponad 100 ms), wiec nie
 * jest kasowane.
 */
public class MultiActionsCheck extends Check {

    /** Jak dlugo ciagly stan podniesiony dowodzi multitoola (ms). */
    private static final long RAISED_WINDOW_MS = 250L;

    public MultiActionsCheck(FloppaAC plugin) {
        super(plugin, "MultiA", CheckType.COMBAT);
    }

    /**
     * Probkowanie stanu reki na kazdym ruchu (takze sam obrot glowy).
     * Wywolywane z MovementListener, zeby flick pomiedzy ruchami
     * nie znikal z historii.
     */
    public void handleMove(Player player, PlayerData data) {
        long now = System.currentTimeMillis();
        boolean raised;
        try {
            raised = player.isHandRaised();
        } catch (NoSuchMethodError e) {
            return;
        }
        if (raised) {
            data.lastRaiseSeenMs = now;
        } else {
            data.lastUnraiseSeenMs = now;
        }
    }

    /**
     * Cios mieczem z podniesiona reka (multitool). Zwraca true, gdy
     * trafienie poszlo z podniesiona reka - listener anuluje obrazenia
     * (ghost trafienia), bo legalny klient najpierw opuszcza reke.
     * Anulowanie od pierwszego ciosu, flaga VL w serii 3.
     */
    public boolean handleAttack(Player attacker, PlayerData data) {
        if (data.movementExempt()) {
            data.multiAStreak = 0;
            return false;
        }
        // Atak z otwartym GUI liczy InventoryCheck (InventoryC), nie Multi.
        if (data.invOpen) {
            data.multiAStreak = 0;
            return false;
        }
        if (attacker.getGameMode() == GameMode.CREATIVE
                || attacker.getGameMode() == GameMode.SPECTATOR) {
            data.multiAStreak = 0;
            return false;
        }
        boolean raisedNow;
        try {
            raisedNow = attacker.isHandRaised();
        } catch (NoSuchMethodError e) {
            return false;
        }
        long now = System.currentTimeMillis();
        boolean continuousRaised = now - data.lastRaiseSeenMs < RAISED_WINDOW_MS
                && data.lastRaiseSeenMs >= data.lastUnraiseSeenMs;
        boolean detected = raisedNow || continuousRaised;
        // Pojedynczy tick desyncu po legalnym puszczeniu: serwer widzi
        // jeszcze podniesiona, ale historia ma swiezy stan opuszczony
        // PO ostatnim podniesionym. Taki atak przepuszczamy raz.
        if (detected && !raisedNow && data.lastUnraiseSeenMs > data.lastRaiseSeenMs) {
            detected = false;
        }
        if (detected) {
            data.multiAStreak++;
            if (data.multiAStreak >= 3) {
                flagAs("MultiA", attacker, data, "hit with raised hand x"
                        + data.multiAStreak);
                data.multiAStreak = 0;
            } else {
                suspicious(attacker, data, "hit with raised hand ("
                        + data.multiAStreak + "/3, ghost)");
            }
            // Ghost od pierwszego ciosu: multitool nie zadaje obrazen.
            return true;
        }
        data.multiAStreak = 0;
        return false;
    }

    public void handleSwing(Player player, PlayerData data) {
        if (data.movementExempt()) {
            data.multiBStreak = 0;
            return;
        }
        if (data.invOpen) {
            data.multiBStreak = 0;
            return;
        }
        if (System.currentTimeMillis() - data.lastDropMs < 500L) {
            return;
        }
        boolean raised;
        try {
            raised = player.isHandRaised();
        } catch (NoSuchMethodError e) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean continuousRaised = now - data.lastRaiseSeenMs < RAISED_WINDOW_MS
                && data.lastRaiseSeenMs >= data.lastUnraiseSeenMs;
        if (raised || continuousRaised) {
            data.multiBStreak++;
            if (data.multiBStreak >= 3) {
                flagAs("MultiB", player, data, "swing with raised hand x"
                        + data.multiBStreak);
                data.multiBStreak = 0;
            }
        } else {
            data.multiBStreak = 0;
        }
    }
}
