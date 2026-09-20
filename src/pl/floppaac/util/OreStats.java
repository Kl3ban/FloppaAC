package pl.floppaac.util;

import java.util.ArrayDeque;
import java.util.Deque;

public final class OreStats {

    private OreStats() {
    }

    public static boolean isValuableOre(String materialName) {
        return materialName.equals("COAL_ORE")
            || materialName.equals("DEEPSLATE_COAL_ORE")
            || materialName.equals("IRON_ORE")
            || materialName.equals("DEEPSLATE_IRON_ORE")
            || materialName.equals("COPPER_ORE")
            || materialName.equals("DEEPSLATE_COPPER_ORE")
            || materialName.equals("GOLD_ORE")
            || materialName.equals("DEEPSLATE_GOLD_ORE")
            || materialName.equals("REDSTONE_ORE")
            || materialName.equals("DEEPSLATE_REDSTONE_ORE")
            || materialName.equals("LAPIS_ORE")
            || materialName.equals("DEEPSLATE_LAPIS_ORE")
            || materialName.equals("DIAMOND_ORE")
            || materialName.equals("DEEPSLATE_DIAMOND_ORE")
            || materialName.equals("EMERALD_ORE")
            || materialName.equals("DEEPSLATE_EMERALD_ORE")
            || materialName.equals("ANCIENT_DEBRIS")
            || materialName.equals("NETHER_GOLD_ORE")
            || materialName.equals("NETHER_QUARTZ_ORE");
    }

    public static boolean isWaste(String materialName) {
        return materialName.equals("STONE")
            || materialName.equals("DEEPSLATE")
            || materialName.equals("DIRT")
            || materialName.equals("COARSE_DIRT")
            || materialName.equals("GRANITE")
            || materialName.equals("DIORITE")
            || materialName.equals("ANDESITE")
            || materialName.equals("TUFF")
            || materialName.equals("GRAVEL")
            || materialName.equals("SAND")
            || materialName.equals("NETHERRACK")
            || materialName.equals("END_STONE")
            || materialName.equals("BLACKSTONE")
            || materialName.equals("BASALT");
    }

    public static boolean xrayRatioFlag(Deque<Long> window,
                                        int windowSize,
                                        int minOres,
                                        double maxWastePerOre) {
        if (window.size() < windowSize) {
            return false;
        }
        int waste = 0;
        int ore = 0;
        for (Long v : window) {
            if (v.longValue() == ORE) {
                ore++;
            } else if (v.longValue() == WASTE) {
                waste++;
            }
        }
        if (ore < minOres) {
            return false;
        }
        return waste < maxWastePerOre * ore;
    }

    public static final long ORE = 2L;
    public static final long WASTE = 1L;
    public static final long OTHER = 0L;

    public static void push(Deque<Long> window, long record, int cap) {
        window.addLast(Long.valueOf(record));
        while (window.size() > cap) {
            window.pollFirst();
        }
    }

    public static boolean xrayLosFlag(int streak, int threshold) {
        return streak >= threshold;
    }

    public static Deque<Long> newWindow(int cap) {
        return new ArrayDeque<Long>(cap);
    }
}
