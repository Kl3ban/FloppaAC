package pl.floppaac.util;

/**
 * Czysty model czasu kopania vanilla. Testowalny bez serwera.
 *
 * Vanilla: obrazenia na tick = speed / hardness / 30 przy wlasciwym
 * narzedziu, ticki = ceil(30 * hardness / speed).
 * speed = baza materialu + (eff^2 + 1) za Efficiency, razy Haste
 * (1 + 0.2 za poziom), dzielone przez 5 w powietrzu i pod woda
 * bez Aqua Affinity. Zawsze zakladamy wlasciwe narzedzie, wiec
 * wynik to DOLNE oszacowanie: legalne kopanie jest wolniejsze
 * albo rowne, nigdy szybsze. Flaga tylko ponizej 45 procent.
 */
public final class DigTime {

    private DigTime() {
    }

    /** Baza predkosci materialu narzedzia trzymanego w rece. */
    public static double toolSpeed(String itemName, String blockName) {
        String item = itemName == null ? "" : itemName.toUpperCase();
        String block = blockName == null ? "" : blockName.toUpperCase();
        // Pajeczyna tnie sie mieczem 15x.
        if (block.contains("WEB") && item.contains("SWORD")) {
            return 15.0;
        }
        double tier;
        if (item.contains("NETHERITE")) {
            tier = 9.0;
        } else if (item.contains("DIAMOND")) {
            tier = 8.0;
        } else if (item.contains("GOLD")) {
            tier = 12.0;
        } else if (item.contains("IRON")) {
            tier = 6.0;
        } else if (item.contains("STONE")) {
            tier = 4.0;
        } else if (item.contains("WOOD")) {
            tier = 2.0;
        } else {
            return 1.0;
        }
        if (item.contains("PICKAXE") || item.contains("AXE")
                || item.contains("SHOVEL") || item.contains("HOE")) {
            return tier;
        }
        return 1.0;
    }

    /**
     * Oczekiwane ticki kopania (dolne oszacowanie).
     * @param hardness twardosc bloku (ujemna = niezniszczalny)
     */
    public static long expectedTicks(double hardness, double toolSpeed,
                                     int efficiency, int haste,
                                     boolean airborne, boolean water, boolean aqua) {
        if (hardness < 0.0) {
            return Long.MAX_VALUE;
        }
        double speed = toolSpeed + (double) (efficiency * efficiency + 1) * (efficiency > 0 ? 1.0 : 0.0);
        speed *= 1.0 + 0.2 * (double) Math.max(0, haste);
        if (airborne) {
            speed /= 5.0;
        }
        if (water && !aqua) {
            speed /= 5.0;
        }
        if (speed <= 0.0) {
            return Long.MAX_VALUE;
        }
        return (long) Math.ceil(30.0 * hardness / speed);
    }

    /** Czy czas jest niemozliwy (ponizej 45 procent dolnego oszacowania). */
    public static boolean isImpossible(long tookTicks, long expectedTicks) {
        if (expectedTicks == Long.MAX_VALUE) {
            return tookTicks != Long.MAX_VALUE;
        }
        if (expectedTicks <= 1L) {
            return false;
        }
        return tookTicks * 100L < expectedTicks * 45L;
    }
}
