package pl.floppaac.util;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/**
 * Stale fizyki Minecrafta i wspolne exemptiony.
 * Wartosci z klienta: walk 0.21585, sprint 0.2806,
 * jump 0.42, gravity 0.08, drag 0.98.
 */
public final class MoveUtil {

    public static final double WALK_SPEED = 0.21585;
    public static final double SPRINT_SPEED = 0.2806;
    public static final double JUMP_VELOCITY = 0.42;
    public static final double STEP_HEIGHT = 0.6;

    private MoveUtil() {
    }

    public static boolean canFly(Player p) {
        return p.getAllowFlight()
            || p.isFlying()
            || p.getGameMode() == GameMode.CREATIVE
            || p.getGameMode() == GameMode.SPECTATOR
            || p.isGliding()
            || p.isRiptiding();
    }

    public static boolean inVehicle(Player p) {
        return p.isInsideVehicle();
    }

    public static boolean inLiquid(Player p) {
        Material m = p.getLocation().getBlock().getType();
        String n = m.name();
        return m == Material.WATER || m == Material.LAVA
            || n.contains("WATER") || n.contains("LAVA");
    }

    public static boolean onClimbable(Player p) {
        if (isClimbable(p.getLocation().getBlock().getType().name())) {
            return true;
        }
        // Na krawedzi drabiny stopy bywaja w powietrzu, klatka na drabinie.
        return isClimbable(p.getEyeLocation().getBlock().getType().name());
    }

    private static boolean isClimbable(String n) {
        return n.contains("LADDER") || n.contains("VINE") || n.contains("SCAFFOLDING");
    }

    public static boolean onIce(Player p) {
        String n = p.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().name();
        return n.contains("ICE");
    }

    /** Chunk niezaladowany to teren-widmo, nie grunt. */
    public static boolean inUnloadedChunk(Location loc) {
        try {
            return !loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Czy pod stopami (do okolo 1.5 bloku) jest realny blok.
     * Uzywane przez NoFall do odrzucenia fake-gruntu deklarowanego
     * pakietowo w powietrzu.
     */
    public static boolean hasSolidBelow(Location loc) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        double y = loc.getY();
        double[] probes = new double[]{0.2, 0.7, 1.2};
        for (double d : probes) {
            if (loc.getWorld().getBlockAt(x, (int) Math.floor(y - d), z).getType().isSolid()) {
                return true;
            }
        }
        return false;
    }

    public static boolean onSlime(Player p) {
        String n = p.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().name();
        return n.contains("SLIME");
    }

    /** Miod przykleja i spowalnia (drift poziomy i pionowy). */
    public static boolean onHoney(Player p) {
        String n = p.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().name();
        if (n.contains("HONEY")) {
            return true;
        }
        String in = p.getLocation().getBlock().getType().name();
        return in.contains("HONEY");
    }

    /** Puder sniezny: wolne toniecie bez obrazen od upadku. */
    public static boolean inPowderSnow(Player p) {
        String n = p.getLocation().getBlock().getType().name();
        return n.contains("POWDER_SNOW");
    }

    /**
     * Kolumna babelowa: legalny pionowy ciag w gore/dol w wodzie.
     * Stala kolumna rozpoznajemy po blokach zrodlowych pod graczem:
     * magma (ciag w dol) albo soul sand (ciag w gore).
     */
    public static boolean inBubbleColumn(Player p) {
        String n = p.getLocation().getBlock().getType().name();
        if (n.contains("BUBBLE")) {
            return true;
        }
        String below = p.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().name();
        return below.contains("MAGMA") || below.contains("SOUL_SAND");
    }

    /**
     * Piston obok gracza pcha legalnie (czesc sciezek nie generuje
     * PlayerVelocityEvent) - exemption dla checkow ruchu.
     * Skan 3x3x3 wokol stopy; wywolywany tylko przy podejrzeniu.
     */
    public static boolean nearPiston(Location loc) {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    String n = loc.getWorld().getBlockAt(x + dx, y + dy, z + dz).getType().name();
                    if (n.contains("PISTON")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Efekt Slow Falling znosi obrazenia od upadku. */
    public static boolean hasSlowFalling(Player p) {
        try {
            return p.hasPotionEffect(org.bukkit.potion.PotionEffectType.SLOW_FALLING);
        } catch (NoSuchFieldError | NoSuchMethodError e) {
            return false;
        }
    }

    public static boolean inWeb(Player p) {
        String n = p.getLocation().getBlock().getType().name();
        if (n.contains("WEB")) {
            return true;
        }
        String eye = p.getEyeLocation().getBlock().getType().name();
        return eye.contains("WEB");
    }

    public static int speedPotionLevel(Player p) {
        try {
            if (p.hasPotionEffect(PotionEffectType.SPEED)
                    && p.getPotionEffect(PotionEffectType.SPEED) != null) {
                return p.getPotionEffect(PotionEffectType.SPEED).getAmplifier() + 1;
            }
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            return 0;
        }
        return 0;
    }

    public static int jumpPotionLevel(Player p) {
        try {
            PotionEffectType type = PotionEffectType.getByName("JUMP_BOOST");
            if (type == null) {
                type = PotionEffectType.getByName("JUMP");
            }
            if (type != null && p.hasPotionEffect(type)
                    && p.getPotionEffect(type) != null) {
                return p.getPotionEffect(type).getAmplifier() + 1;
            }
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            return 0;
        }
        return 0;
    }

    /** Czy gracz ma lewitacje (Shulker unosi legalnie). */
    public static boolean hasLevitation(Player p) {
        try {
            PotionEffectType type = PotionEffectType.getByName("LEVITATION");
            if (type != null && p.hasPotionEffect(type)) {
                return true;
            }
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            return false;
        }
        return false;
    }

    /** Maksymalny poziomy dystans na tick dla gracza z bonusami. */
    public static double maxHorizontal(Player p, double baseWalk,
                                       double baseSprint, double potionBonus) {
        double base = p.isSprinting() ? baseSprint : baseWalk;
        base += speedPotionLevel(p) * potionBonus;
        if (onIce(p)) {
            base *= 1.6;
        }
        return base;
    }

    public static double applyLeniency(double value, double leniency) {
        return value * (1.0 + leniency);
    }
}
