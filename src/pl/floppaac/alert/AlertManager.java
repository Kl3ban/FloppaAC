package pl.floppaac.alert;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.CheckType;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Alerty FloppaAC. Trzy poziomy:
 * alert: potwierdzona flaga (VL rosnie), do ekipy i do testera,
 * verbose: podejrzenie ponizej progu, tylko dla trybu verbose,
 * info: komunikaty systemowe dla testera.
 */
public class AlertManager {

    private final FloppaAC plugin;
    private final Set<UUID> alertsOff = ConcurrentHashMap.newKeySet();
    private final Set<UUID> verboseOn = ConcurrentHashMap.newKeySet();

    public AlertManager(FloppaAC plugin) {
        this.plugin = plugin;
    }

    public boolean toggleAlerts(Player player) {
        if (!alertsOff.remove(player.getUniqueId())) {
            alertsOff.add(player.getUniqueId());
            return false;
        }
        return true;
    }

    public boolean hasAlerts(Player player) {
        return !alertsOff.contains(player.getUniqueId());
    }

    public boolean toggleVerbose(Player player) {
        if (!verboseOn.remove(player.getUniqueId())) {
            verboseOn.add(player.getUniqueId());
            return true;
        }
        return false;
    }

    public boolean hasVerbose(Player player) {
        return verboseOn.contains(player.getUniqueId());
    }

    public void alert(Player cheater, String check, CheckType type,
                      int vl, int ping, String details) {
        String format = color(plugin.getConfig().getString("alerts.format",
                "&8[&bFloppaAC&8] &c%player% &7nie przeszedl &b%check% &8(VL: &c%vl%&8) &7ping: &e%ping% &7tps: &e%tps%"));
        String tps = String.format("%.1f", plugin.getTpsMonitor().getTps());
        String msg = format.replace("%player%", cheater.getName())
                .replace("%check%", check)
                .replace("%vl%", String.valueOf(vl))
                .replace("%ping%", String.valueOf(ping))
                .replace("%tps%", tps)
                .replace("%details%", details == null ? "" : details);
        if (details != null && !details.isEmpty()) {
            msg += " §8(§7" + details + "§8)";
        }
        final String out = msg;
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("floppaac.staff") && hasAlerts(staff)) {
                staff.sendMessage(out);
            }
        }
        if (plugin.getConfig().getBoolean("alerts.send-to-cheater", true)) {
            String cheaterFormat = color(plugin.getConfig().getString("alerts.cheater-format",
                    "&8[&bFloppaAC&8] &eWykryto: &b%check% &8(VL: &c%vl%&8) &7ping: &e%ping% &7tps: &e%tps%"));
            String personal = cheaterFormat.replace("%player%", cheater.getName())
                    .replace("%check%", check)
                    .replace("%vl%", String.valueOf(vl))
                    .replace("%ping%", String.valueOf(ping))
                    .replace("%tps%", tps)
                    .replace("%details%", details == null ? "" : details);
            if (details != null && !details.isEmpty()) {
                personal += " §8(§7" + details + "§8)";
            }
            cheater.sendMessage(personal);
        }
        plugin.getLogger().info("[ALERT] " + cheater.getName() + " failed "
                + check + " VL=" + vl + " (" + details + ")");
    }

    /**
     * Podejrzenie ponizej progu flagi. Dostaje je gracz z wlaczonym
     * verbose oraz ekipa z wlaczonym verbose.
     */
    public void verbose(Player subject, String check, int ping, String details) {
        if (!plugin.getConfig().getBoolean("alerts.verbose-enabled", true)) {
            return;
        }
        String tps = String.format("%.1f", plugin.getTpsMonitor().getTps());
        String msg = "§8[§bFloppaAC§8] §7podejrzane §b" + check + " §7u §e" + subject.getName()
                + " §8(ping: §e" + ping + "§8 tps: §e" + tps + "§8)";
        if (details != null && !details.isEmpty()) {
            msg += " §8(§7" + details + "§8)";
        }
        final String out = msg;
        if (hasVerbose(subject)) {
            subject.sendMessage(out);
        }
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.equals(subject)) {
                continue;
            }
            if (staff.hasPermission("floppaac.staff") && hasVerbose(staff)) {
                staff.sendMessage(out);
            }
        }
    }

    public void info(Player player, String text) {
        player.sendMessage(color(plugin.getConfig().getString("prefix", "&8[&bFloppaAC&8]"))
                + " §7" + text);
    }

    public String prefix() {
        return color(plugin.getConfig().getString("prefix", "&8[&bFloppaAC&8]"));
    }

    public static String color(String s) {
        return s == null ? "" : s.replace('&', '§');
    }
}
