package pl.floppaac.util;

public final class DigTime {

    private DigTime() {
    }

    public static double toolSpeed(String itemName, String blockName) {
        String item = itemName == null ? "" : itemName.toUpperCase();
        String block = blockName == null ? "" : blockName.toUpperCase();

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
