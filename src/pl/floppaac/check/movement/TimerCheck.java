package pl.floppaac.check.movement;

import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.MoveUtil;

/**
 * TimerA: cheat przyspieszajacy ticki klienta.
 * Model bilansu jak w Grim: kazdy ruch doklada 50 ms kredytu.
 * Legalny klient wysyla ruch co 50 ms, wiec bilans krazy wokol
 * czasu rzeczywistego. Cheat 1.2x do 2x systematycznie wyprzedza
 * czas. Dryf 250 ms pochlania batchowanie TCP po mikrostallu,
 * a seria 3 przekroczen rozroznia pojedynczy burst od cheata.
 * TPS ponizej 18.5 blokuje flage (opoznienia serwera, nie gracza).
 */
public class TimerCheck extends Check {

    /** Najwiekszy dozwolony zapas kredytu z lagow (1 s). */
    private static final long MAX_BEHIND_NS = 1000000000L;
    /** Okno serii przekroczen w ns (2 s). */
    private static final long STREAK_WINDOW_NS = 2000000000L;

    public TimerCheck(FloppaAC plugin) {
        super(plugin, "TimerA", CheckType.MOVEMENT);
    }

    /** Ruch ze zmiana pozycji. Liczy do bilansu Timer. */
    public void handle(Player player, PlayerData data) {
        long now = System.nanoTime();
        if (data.movementExempt() || MoveUtil.inVehicle(player)) {
            data.timerBalanceNs = 0L;
            data.timerStreak = 0;
            return;
        }
        if (data.timerBalanceNs == 0L) {
            data.timerBalanceNs = now;
            return;
        }
        data.timerBalanceNs += 50000000L;
        if (data.timerBalanceNs < now - MAX_BEHIND_NS) {
            data.timerBalanceNs = now - MAX_BEHIND_NS;
        }
        long drift = (long) cfgInt("drift-ms", 250) * 1000000L;
        if (data.timerBalanceNs > now + drift) {
            data.timerBalanceNs = now + drift;
            if (now - data.timerLastExceedNs > STREAK_WINDOW_NS) {
                data.timerStreak = 0;
            }
            data.timerLastExceedNs = now;
            data.timerStreak++;
            if (data.timerStreak >= 3) {
                flag(player, data, "balance x" + data.timerStreak);
                data.timerStreak = 0;
            } else if (data.timerStreak == 2) {
                suspicious(player, data, "timer drifting (observation)");
            }
        }
    }

    /** Luka powyzej 5 s otwiera nowy bilans (AFK to nie Timer). */
    public void handleIdleGap(PlayerData data, long gapMs) {
        // AFK/teleport otwiera bilans od nowa. Krotkie zamarzniecie
        // klienta (lag FPS < 5 s) NIE resetuje: bilans po prostu schodzi
        // za luke (brak doplaty 50 ms), a max-behind ogranicza zapas
        // do 1 s, wiec powrot po lagu nie wyglada jak timer cheat.
        if (gapMs > 5000L) {
            data.timerBalanceNs = 0L;
            data.timerStreak = 0;
        }
    }
}
