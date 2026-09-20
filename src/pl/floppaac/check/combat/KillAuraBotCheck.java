package pl.floppaac.check.combat;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.VerifyMath;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class KillAuraBotCheck extends Check {

    private static final String[] AURA_VL = {
        "KillAuraA", "KillAuraB", "KillAuraC", "KillAuraD", "KillAuraE",
        "KillAuraF", "KillAuraG", "KillAuraH", "KillAuraI", "KillAuraJ"
    };

    private static final int MAX_ACTIVE = 3;

    private final Map<UUID, Verification> active =
            new ConcurrentHashMap<UUID, Verification>();
    private final Map<UUID, UUID> botToSuspect =
            new ConcurrentHashMap<UUID, UUID>();

    public KillAuraBotCheck(FloppaAC plugin) {
        super(plugin, "KillAuraBot", CheckType.COMBAT);
    }

    public static int auraVl(PlayerData data) {
        int sum = 0;
        for (String name : AURA_VL) {
            sum += data.getVl(name);
        }
        return sum;
    }

    public void maybeVerify(Player suspect, PlayerData data) {
        if (!isEnabled()) {
            return;
        }
        try {
            if (suspect.hasPermission("floppaac.bypass")) {
                return;
            }
        } catch (Exception e) {
            return;
        }
        if (suspect.getGameMode() != GameMode.SURVIVAL
                && suspect.getGameMode() != GameMode.ADVENTURE) {
            return;
        }
        if (data.movementExempt() || data.invOpen) {
            return;
        }
        if (active.containsKey(data.uuid)) {
            return;
        }
        if (active.size() >= MAX_ACTIVE) {
            return;
        }
        long now = System.currentTimeMillis();
        long sinceAttack = now - data.lastAttackMs;
        long sinceVerify = data.lastBotVerifyMs <= 0L
                ? Long.MAX_VALUE : now - data.lastBotVerifyMs;
        if (!VerifyMath.shouldStart(auraVl(data), sinceAttack, sinceVerify)) {
            return;
        }
        start(suspect, data, now);
    }

    private void start(Player suspect, PlayerData data, long now) {
        Location base;
        try {
            base = suspect.getLocation();
        } catch (Exception e) {
            return;
        }
        if (base.getWorld() == null) {
            return;
        }
        Location spawn = orbitPos(base, 0);
        Entity raw;
        try {
            raw = base.getWorld().spawnEntity(spawn, EntityType.VILLAGER);
        } catch (Exception e) {
            return;
        }
        if (!(raw instanceof Villager)) {
            try {
                raw.remove();
            } catch (Exception e) {

            }
            try {
                plugin.getLogger().warning("[Verify] Failed to spawn villager for "
                        + suspect.getName());
            } catch (Exception e) {

            }
            return;
        }
        Villager bot = (Villager) raw;
        try {
            bot.setAdult();
            bot.setAware(false);
            bot.setSilent(true);
            bot.setGravity(false);
            bot.setCanPickupItems(false);
            bot.setRemoveWhenFarAway(true);
            bot.setCustomName("FloppaAC-Verifier");
            bot.setCustomNameVisible(false);
            bot.addPotionEffect(new PotionEffect(
                    PotionEffectType.INVISIBILITY,
                    PotionEffect.INFINITE_DURATION, 0, false, false, false));
        } catch (Exception e) {
            try {
                bot.remove();
            } catch (Exception ex) {

            }
            try {
                plugin.getLogger().warning("[Verify] Bot configuration failed for "
                        + suspect.getName() + ": " + e);
            } catch (Exception ex) {

            }
            return;
        }
        data.lastBotVerifyMs = now;
        Verification v = new Verification(data.uuid, bot.getUniqueId());
        active.put(data.uuid, v);
        botToSuspect.put(bot.getUniqueId(), data.uuid);
        announceStaff(suspect, "weryfikacja KillAura: niewidzialny bot 15 s (VL aury="
                + auraVl(data) + "). Hitting the bot twice confirms.");
        try {
            v.task = Bukkit.getScheduler().runTaskTimer(
                    plugin, new OrbitRunner(v), 0L,
                    (long) VerifyMath.ORBIT_STEP_TICKS);
        } catch (Exception e) {
            endSilent(data.uuid);
        }
    }

    public boolean handleBotHit(Player attacker, LivingEntity botEntity) {
        UUID suspect = botToSuspect.get(botEntity.getUniqueId());
        if (suspect == null) {
            return false;
        }
        if (!attacker.getUniqueId().equals(suspect)) {

            return true;
        }
        Verification v = active.get(suspect);
        if (v == null) {
            return true;
        }
        v.hits++;
        PlayerData data = plugin.getDataManager().get(attacker);
        if (VerifyMath.isConfirmed(v.hits)) {
            Player online = Bukkit.getPlayer(suspect);
            if (online != null) {
                flagAs("KillAuraBot", online, data,
                        "trafil niewidzialnego bota x" + v.hits);
                kickNow(online, "KillAuraBot");
            }
            endSilent(suspect);
        } else {
            suspicious(attacker, data, "bot hit (1/2)");
        }
        return true;
    }

    public boolean isBot(UUID entityUuid) {
        return botToSuspect.containsKey(entityUuid);
    }

    public void cancel(UUID suspect) {
        endSilent(suspect);
    }

    private void announceStaff(Player suspect, String text) {
        try {
            plugin.getLogger().info("[Verify] " + suspect.getName() + ": " + text);
        } catch (Exception e) {

        }
        try {
            for (Player p : Bukkit.getOnlinePlayers()) {
                try {
                    if (p.hasPermission("floppaac.staff")) {
                        plugin.getAlertManager().info(p,
                                suspect.getName() + ": " + text);
                    }
                } catch (Exception e) {

                }
            }
        } catch (Exception e) {

        }
    }

    private void endSilent(UUID suspect) {
        Verification v = active.remove(suspect);
        if (v == null) {
            return;
        }
        try {
            if (v.task != null) {
                v.task.cancel();
            }
        } catch (Exception e) {

        }
        for (Map.Entry<UUID, UUID> e : botToSuspect.entrySet()) {
            if (e.getValue().equals(suspect)) {
                botToSuspect.remove(e.getKey());
            }
        }

        try {
            for (org.bukkit.World w : Bukkit.getWorlds()) {
                for (Entity ent : w.getEntities()) {
                    if (ent.getUniqueId().equals(v.botUuid)) {
                        ent.remove();
                    }
                }
            }
        } catch (Exception e) {

        }
    }

    private void expire(UUID suspect) {
        Verification v = active.get(suspect);
        PlayerData data = null;
        try {
            data = plugin.getDataManager().find(suspect);
        } catch (Exception e) {
            data = null;
        }
        int hits = v != null ? v.hits : 0;
        endSilent(suspect);
        try {
            Player online = Bukkit.getPlayer(suspect);
            if (online != null) {
                announceStaff(online, "verification clean (bot hits: "
                        + hits + "), VL aury -2.");
            } else {
                plugin.getLogger().info("[Verify] Verification clean, player offline.");
            }
        } catch (Exception e) {

        }
        if (data != null) {
            for (String name : AURA_VL) {
                Integer cur = data.vl.get(name);
                if (cur != null && cur.intValue() > 0) {
                    data.vl.put(name, Integer.valueOf(
                            Math.max(0, cur.intValue() - 2)));
                }
            }
        }
    }

    private static Location orbitPos(Location base, int step) {
        double angle = Math.toRadians((double) (step * 45));
        Location pos = base.clone().add(
                Math.cos(angle) * VerifyMath.ORBIT_RADIUS,
                1.3,
                Math.sin(angle) * VerifyMath.ORBIT_RADIUS);

        for (int i = 0; i < 2; i++) {
            try {
                if (pos.getBlock().getType().isSolid()
                        && pos.clone().add(0.0, 1.0, 0.0)
                                .getBlock().getType().isSolid()) {
                    pos.add(0.0, 1.0, 0.0);
                } else {
                    break;
                }
            } catch (Exception e) {
                break;
            }
        }
        return pos;
    }

    private static final class Verification {
        final UUID suspect;
        final UUID botUuid;
        int hits;
        int step;
        BukkitTask task;

        Verification(UUID suspect, UUID botUuid) {
            this.suspect = suspect;
            this.botUuid = botUuid;
        }
    }

    private final class OrbitRunner implements Runnable {
        private final Verification v;

        OrbitRunner(Verification v) {
            this.v = v;
        }

        @Override
        public void run() {
            Player suspect = Bukkit.getPlayer(v.suspect);
            if (suspect == null || !suspect.isOnline() || suspect.isDead()) {
                endSilent(v.suspect);
                return;
            }
            try {
                if (suspect.getGameMode() != GameMode.SURVIVAL
                        && suspect.getGameMode() != GameMode.ADVENTURE) {
                    endSilent(v.suspect);
                    return;
                }
            } catch (Exception e) {
                endSilent(v.suspect);
                return;
            }
            if (v.step * VerifyMath.ORBIT_STEP_TICKS >= VerifyMath.DURATION_TICKS) {
                expire(v.suspect);
                return;
            }
            Entity bot = null;
            try {
                for (Entity ent : suspect.getWorld().getEntities()) {
                    if (ent.getUniqueId().equals(v.botUuid)) {
                        bot = ent;
                        break;
                    }
                }
            } catch (Exception e) {
                bot = null;
            }
            if (bot == null || bot.isDead() || !bot.isValid()) {
                endSilent(v.suspect);
                return;
            }
            v.step++;
            try {
                bot.teleport(orbitPos(suspect.getLocation(), v.step));
            } catch (Exception e) {

            }
        }
    }
}
