package pl.floppaac.check;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.PingUtil;

public abstract class Check {

    protected final FloppaAC plugin;
    protected final String name;
    protected final CheckType type;

    private static final long FLAG_COOLDOWN_MS = 500L;

    protected Check(FloppaAC plugin, String name, CheckType type) {
        this.plugin = plugin;
        this.name = name;
        this.type = type;
    }

    public String name() {
        return name;
    }

    public CheckType type() {
        return type;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("checks." + name + ".enabled", true);
    }

    public double cfgDouble(String key, double def) {
        return plugin.getConfig().getDouble("checks." + name + "." + key, def);
    }

    public int cfgInt(String key, int def) {
        return plugin.getConfig().getInt("checks." + name + "." + key, def);
    }

    protected boolean flag(Player player, PlayerData data, String details) {
        return flagAs(name, player, data, details);
    }

    protected boolean flagAs(String vlName, Player player, PlayerData data, String details) {
        if (!plugin.getConfig().getBoolean("checks." + vlName + ".enabled", true)) {

            if (!isEnabled()) {
                return false;
            }
        }
        if (player.hasPermission("floppaac.bypass")) {
            return false;
        }
        if (type == CheckType.MOVEMENT
                && plugin.getTpsMonitor().getTps()
                    < plugin.getConfig().getDouble("min-tps", 18.5)) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long last = data.lastFlag.get(vlName);
        long cooldown = FLAG_COOLDOWN_MS;
        if (vlName.startsWith("FakeLag")) {
            cooldown = 1000L;
        }
        if (last != null && now - last < cooldown) {

            data.suspiciousCount++;
            punish(player, data, vlName, data.getVl(vlName));
            return false;
        }
        data.lastFlag.put(vlName, now);
        data.addVl(vlName);
        int vl = data.getVl(vlName);

        int ping = PingUtil.getPing(player, data);
        plugin.getAlertManager().alert(player, vlName, type, vl, ping, details);

        punish(player, data, vlName, vl);
        return true;
    }

    protected boolean signalAs(String vlName, Player player, PlayerData data, String details) {
        if (!plugin.getConfig().getBoolean("checks." + vlName + ".enabled", true)) {
            if (!isEnabled()) {
                return false;
            }
        }
        if (player.hasPermission("floppaac.bypass")) {
            return false;
        }
        if (type == CheckType.MOVEMENT
                && plugin.getTpsMonitor().getTps()
                    < plugin.getConfig().getDouble("min-tps", 18.5)) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long last = data.lastFlag.get(vlName);
        if (last != null && now - last < FLAG_COOLDOWN_MS) {
            data.suspiciousCount++;
            return false;
        }
        data.lastFlag.put(vlName, now);
        data.addVl(vlName);
        int ping = PingUtil.getPing(player, data);
        plugin.getAlertManager().verbose(player, vlName, ping, details);
        return true;
    }

    protected void suspicious(Player player, PlayerData data, String details) {
        if (player.hasPermission("floppaac.bypass")) {
            return;
        }
        data.suspiciousCount++;
        int ping = PingUtil.getPing(player, data);
        plugin.getAlertManager().verbose(player, name, ping, details);
    }

    protected void snapToGround(Player player, PlayerData data) {
        if (data.lastSafeLoc == null) {
            return;
        }
        try {
            Location sb = data.lastSafeLoc.clone();
            sb.setYaw(player.getLocation().getYaw());
            sb.setPitch(player.getLocation().getPitch());
            player.teleport(sb);
            player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            ownTeleport(data);
        } catch (Exception e) {

        }
    }

    protected void perTickPushback(Player player, PlayerData data, Location back) {
        try {
            Location sb = back.clone();
            sb.setYaw(player.getLocation().getYaw());
            sb.setPitch(player.getLocation().getPitch());
            player.teleport(sb);
            player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            ownTeleport(data);
        } catch (Exception e) {

        }
    }

    private void ownTeleport(PlayerData data) {
        long now = System.currentTimeMillis();
        data.lastTeleport = now - 950L;
        data.setbackGraceMs = now;
    }

    private void punish(Player player, PlayerData data, String vlName, int vl) {
        String k = type.key();

        int effectiveVl = vl;
        if (vlName.startsWith("FakeLag")) {
            effectiveVl = data.getVl("FakeLagA") + data.getVl("FakeLagB")
                    + data.getVl("FakeLagC");
        }
        int setbackVl = plugin.getConfig().getInt("punishments.movement-setback-vl", 2);
        int kickVl = plugin.getConfig().getInt("punishments." + k + "-kick-vl", 10);
        int banVl = plugin.getConfig().getInt("punishments." + k + "-ban-vl", 999999);

        if (vlName.startsWith("FakeLag")) {
            kickVl = plugin.getConfig().getInt("punishments.fakelag-kick-vl", 6);
        }

        if (type == CheckType.MOVEMENT && effectiveVl >= setbackVl) {
            Location target = data.lastSafeLoc != null ? data.lastSafeLoc : data.setbackLoc;
            if (target != null) {
                Location sb = target.clone();
                sb.setYaw(player.getLocation().getYaw());
                sb.setPitch(player.getLocation().getPitch());
                player.teleport(sb);
                try {
                    player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                } catch (Exception e) {

                }

                ownTeleport(data);
                data.airTicks = 0;
                data.hoverTicks = 0;
            }
        }
        if (effectiveVl >= banVl) {
            String cmd = plugin.getConfig().getString("punishments.ban-command",
                    "ban %player% FloppaAC: cheat (%check%)");
            dispatch(cmd, player, vlName);
        } else if (effectiveVl >= kickVl) {
            String cmd = plugin.getConfig().getString("punishments.kick-command",
                    "kick %player% FloppaAC: cheat detected (%check%)");
            dispatch(cmd, player, vlName);
        }
    }

    protected void kickNow(Player player, String vlName) {
        String cmd = plugin.getConfig().getString("punishments.kick-command",
                "kick %player% FloppaAC: cheat detected (%check%)");
        dispatch(cmd, player, vlName);
    }

    private void dispatch(String cmd, Player player, String vlName) {
        String c = cmd.replace("%player%", player.getName()).replace("%check%", vlName);
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c);
            }
        });
    }
}
