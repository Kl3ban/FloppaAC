package pl.floppaac.util;

/**
 * Czysta matematyka weryfikacji KillAura botem (styl Matrix).
 * Heurystyki katow i rotacji lapia tez utalentowanych graczy
 * z nienaturalnymi flickami, wiec decyzje podejmuje bot NPC:
 * niewidzialny mieszkaniec orbituje wokol podejrzanego. Legalny
 * gracz nie trafia w puste powietrze, aura bije sama.
 * Klasa czysta, testowalna bez serwera.
 */
public final class VerifyMath {

    /** Suma VL aury (A-J), od ktorej startuje weryfikacja botem. */
    public static final int SUSPICION_VL = 2;

    /** Podejrzany musi walczyc (atak nie starszy niz to). */
    public static final long ATTACK_FRESH_MS = 5000L;

    /** Odstep miedzy weryfikacjami tego samego gracza. */
    public static final long VERIFY_COOLDOWN_MS = 60000L;

    /** Trafienia w bota potrzebne do potwierdzenia (2 = brak FP). */
    public static final int HITS_TO_CONFIRM = 2;

    /** Czas zycia bota w tickach (15 s). */
    public static final int DURATION_TICKS = 300;

    /** Co ile tickow bot zmienia pozycje na orbicie. */
    public static final int ORBIT_STEP_TICKS = 5;

    /** Promien orbity bota w blokach. */
    public static final double ORBIT_RADIUS = 2.6;

    private VerifyMath() {
    }

    /**
     * Czy startowac weryfikacje botem.
     * @param auraVl suma VL KillAuraA-J podejrzanego
     * @param sinceAttackMs czas od ostatniego ataku podejrzanego
     * @param sinceVerifyMs czas od ostatniej weryfikacji (Long.MAX_VALUE gdy brak)
     */
    public static boolean shouldStart(int auraVl, long sinceAttackMs, long sinceVerifyMs) {
        if (auraVl < SUSPICION_VL) {
            return false;
        }
        if (sinceAttackMs < 0L || sinceAttackMs > ATTACK_FRESH_MS) {
            return false;
        }
        return sinceVerifyMs > VERIFY_COOLDOWN_MS;
    }

    /** Czy liczba trafien w bota potwierdza KillAure. */
    public static boolean isConfirmed(int hits) {
        return hits >= HITS_TO_CONFIRM;
    }
}
