package pl.floppaac.check.movement;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.MoveUtil;

/**
 * FlyC: lot slizgowy w dol (glide).
 * Vanilla spadek przyspiesza do okolo 3.9 na tick. Utrzymywane
 * opadanie 0.03 do 0.6 na tick z ruchem poziomym przez 15 tickow
 * to cheat glide (tryb Vanilla z FlyGeneric leci tak w dol).
 * Zwolnienia: woda, pajeczyna, drabiny, pojazd, lot, elytra,
 * mikstura Slow Falling i Levitation, blok miodu.
 */
public class FlyGlideCheck extends Check {

    public FlyGlideCheck(FloppaAC plugin) {
        super(plugin, "FlyC", CheckType.MOVEMENT);
    }

    public void handle(Player player, PlayerData data, double dy, double horizontal) {
        if (data.movementExempt()) {
            // Karencja po naszym pushbacku NIE zeruje serii.
            return;
        }

        if (data.velocityExempt(player, System.currentTimeMillis())) {
            // Odrzut (zombie, strzala, TNT): grawitacja legalnie zaburzona
            // przez ticki odbicia - nie oceniamy ich.
            return;
        }
        if (MoveUtil.canFly(player) || MoveUtil.inVehicle(player)
                || MoveUtil.inLiquid(player) || MoveUtil.onClimbable(player)
                || MoveUtil.inWeb(player) || MoveUtil.hasLevitation(player)
                || MoveUtil.inBubbleColumn(player) || MoveUtil.inPowderSnow(player)
                || player.isGliding() || player.isRiptiding()) {
            data.glideStreak = 0;
            return;
        }
        if (player.isOnGround()) {
            data.glideStreak = 0;
            return;
        }
        if (hasSlowFall(player) || onHoney(player)) {
            data.glideStreak = 0;
            return;
        }
        if (dy < -0.03 && dy > -0.6 && horizontal > 0.1) {
            data.glideStreak++;
            if (data.glideStreak >= 10) {
                flag(player, data, String.format("glide dy=%.3f x%d",
                        dy, data.glideStreak));
                data.glideStreak = 0;
                snapToGround(player, data);
            }
        } else {
            data.glideStreak = Math.max(0, data.glideStreak - 2);
        }
    }

    private boolean hasSlowFall(Player player) {
        try {
            if (player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
                return true;
            }
            PotionEffectType lev = PotionEffectType.getByName("LEVITATION");
            if (lev != null && player.hasPotionEffect(lev)) {
                return true;
            }
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            return false;
        }
        return false;
    }

    private boolean onHoney(Player player) {
        String feet = player.getLocation().getBlock().getType().name();
        String eye = player.getEyeLocation().getBlock().getType().name();
        return feet.contains("HONEY") || eye.contains("HONEY");
    }
}
