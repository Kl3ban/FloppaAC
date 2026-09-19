package pl.floppaac.check.movement;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.MoveUtil;

/**
 * SprintA: sprint do tylu (omnisprint).
 * Vanilla sprint wymaga biegu do przodu. Sprint ze strafem bocznym
 * albo cofaniem z pelna predkoscia to cheat. Kat miedzy kierunkiem
 * ruchu a wzrokiem powyzej 120 stopni w serii 5.
 * Odrzut po ciosie (awaitingVelocity) zwalnia, bo pcha do tylu.
 */
public class SprintSpoofCheck extends Check {

    public SprintSpoofCheck(FloppaAC plugin) {
        super(plugin, "SprintA", CheckType.MOVEMENT);
    }

    public void handle(Player player, PlayerData data, Location from, Location to,
                       double horizontal) {
        if (data.movementExempt() || MoveUtil.canFly(player) || MoveUtil.inVehicle(player)
                || MoveUtil.inLiquid(player)
                || data.velocityExempt(player, System.currentTimeMillis())) {
            // velocityExempt: odrzut legalnie pcha do tylu i na boki.
            data.sprintStreak = 0;
            return;
        }
        if (!player.isSprinting() || horizontal < 0.2) {
            data.sprintStreak = 0;
            return;
        }
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double yawRad = Math.toRadians((double) to.getYaw());
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);
        double len = Math.hypot(dx, dz);
        if (len < 0.000001) {
            data.sprintStreak = 0;
            return;
        }
        double cos = (dx * fx + dz * fz) / len;
        cos = Math.max(-1.0, Math.min(1.0, cos));
        double angle = Math.toDegrees(Math.acos(cos));
        if (angle > 120.0) {
            data.sprintStreak++;
            if (data.sprintStreak >= 5) {
                flag(player, data, String.format("angle=%.0f dist=%.2f",
                        angle, horizontal));
                data.sprintStreak = 0;
            }
        } else {
            data.sprintStreak = Math.max(0, data.sprintStreak - 1);
        }
    }

    /**
     * Stany sprintu jak u Grima, seria zamiast pierwszego ticku.
     * SprintB: sprint przy glodzie 6 lub mniej. Vanilla blokuje sprint
     * ponizej 7, wiec dluzsza seria to cheat. Seria 5.
     * SprintD: sprint w locie na elytrze. Vanilla sprint gasi. Seria 5.
     * SprintE: sprint w pelnym zanurzeniu (stopy i oczy w wodzie,
     * bez pływania powierzchniowego). Vanilla sprint gasi. Seria 8.
     */
    public void handleStates(Player player, PlayerData data) {
        if (data.movementExempt() || MoveUtil.inVehicle(player)) {
            data.hungerStreak = 0;
            data.glideSprintStreak = 0;
            data.waterSprintStreak = 0;
            return;
        }
        if (!player.isSprinting()) {
            data.hungerStreak = 0;
            data.glideSprintStreak = 0;
            data.waterSprintStreak = 0;
            return;
        }
        if (player.getFoodLevel() <= 6) {
            data.hungerStreak++;
            if (data.hungerStreak >= 5) {
                flagAs("SprintB", player, data, "glod=" + player.getFoodLevel()
                        + " x" + data.hungerStreak);
                data.hungerStreak = 0;
            }
        } else {
            data.hungerStreak = 0;
        }
        if (player.isGliding()) {
            data.glideSprintStreak++;
            if (data.glideSprintStreak >= 5) {
                flagAs("SprintD", player, data, "sprint while flying x"
                        + data.glideSprintStreak);
                data.glideSprintStreak = 0;
            }
        } else {
            data.glideSprintStreak = 0;
        }
        if (submerged(player) && !player.isSwimming() && !player.isRiptiding()) {
            data.waterSprintStreak++;
            if (data.waterSprintStreak >= 8) {
                flagAs("SprintE", player, data, "sprint underwater x"
                        + data.waterSprintStreak);
                data.waterSprintStreak = 0;
            }
        } else {
            data.waterSprintStreak = 0;
        }
    }

    /**
     * SprintC: start sprintu ze slepota (jak SprintD u Grima).
     * Legalny klient nie wysyla startu sprintu w ciemnosci.
     * Seria 2 na wypadek desyncu efektu.
     */
    public void handleSprintStart(Player player, PlayerData data) {
        if (data.movementExempt() || MoveUtil.inVehicle(player)) {
            return;
        }
        if (hasBlindness(player)) {
            data.blindStreak++;
            if (data.blindStreak >= 2) {
                flagAs("SprintC", player, data, "start ze slepota x"
                        + data.blindStreak);
                data.blindStreak = 0;
            }
        } else {
            data.blindStreak = 0;
        }
    }

    private boolean submerged(Player player) {
        String feet = player.getLocation().getBlock().getType().name();
        String eye = player.getEyeLocation().getBlock().getType().name();
        return (feet.contains("WATER") || feet.contains("LAVA"))
                && (eye.contains("WATER") || eye.contains("LAVA"));
    }

    private boolean hasBlindness(Player player) {
        try {
            org.bukkit.potion.PotionEffectType blind =
                    org.bukkit.potion.PotionEffectType.getByName("BLINDNESS");
            if (blind != null && player.hasPotionEffect(blind)) {
                return true;
            }
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            return false;
        }
        return false;
    }
}
