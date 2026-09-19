package pl.floppaac.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.ClickStats;
import pl.floppaac.util.FakeLagMath;
import pl.floppaac.util.PingUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * /floppaac alerts | verbose | vl [gracz] | clear &lt;gracz&gt; | reload | status | debug [gracz].
 * Narzedzia testera: verbose pokazuje podejrzenia ponizej progu,
 * status pokazuje stan silnika, debug pokazuje telemetrie gracza.
 */
public class FloppaCommand implements CommandExecutor, TabCompleter {

    private final FloppaAC plugin;

    public FloppaCommand(FloppaAC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String pfx = plugin.getAlertManager().prefix();
        if (args.length == 0) {
            sender.sendMessage(pfx + " §7FloppaAC " + FloppaAC.VERSION + " - uzyj:");
            sender.sendMessage("§b/" + label + " alerts §7- wlacz/wylacz alerty ekipy");
            sender.sendMessage("§b/" + label + " verbose §7- podejrzenia ponizej progu");
            sender.sendMessage("§b/" + label + " vl [gracz] §7- aktualne VL");
            sender.sendMessage("§b/" + label + " clear <gracz> §7- wyzeruj VL");
            sender.sendMessage("§b/" + label + " status §7- stan silnika");
            sender.sendMessage("§b/" + label + " debug [gracz] §7- telemetria");
            sender.sendMessage("§b/" + label + " reload §7- przeladuj config");
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("alerts")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(pfx + " §7tylko w grze.");
                return true;
            }
            Player p = (Player) sender;
            boolean on = plugin.getAlertManager().toggleAlerts(p);
            sender.sendMessage(pfx + (on ? " §aAlerty wlaczone." : " §cAlerty wylaczone."));
            return true;
        }
        if (sub.equals("verbose")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(pfx + " §7tylko w grze.");
                return true;
            }
            Player p = (Player) sender;
            boolean on = plugin.getAlertManager().toggleVerbose(p);
            sender.sendMessage(pfx + (on
                    ? " §aVerbose wlaczony. Bedziesz widzial podejrzenia."
                    : " §cVerbose wylaczony."));
            return true;
        }
        if (sub.equals("vl")) {
            Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1])
                    : (sender instanceof Player ? (Player) sender : null);
            if (target == null) {
                sender.sendMessage(pfx + " §cGracz offline.");
                return true;
            }
            PlayerData data = plugin.getDataManager().get(target);
            sender.sendMessage(pfx + " §7VL gracza §e" + target.getName() + "§7:");
            boolean any = false;
            for (Check c : plugin.getCheckManager().all()) {
                int vl = data.getVl(c.name());
                if (vl > 0) {
                    sender.sendMessage(" §8- §b" + c.name() + "§8: §c" + vl);
                    any = true;
                }
            }
            for (String extra : new String[]{"FakeLagB", "FakeLagC", "KillAuraB", "KillAuraC", "KillAuraD", "KillAuraE", "KillAuraF", "KillAuraG", "KillAuraH", "KillAuraI", "MultiA", "MultiB", "WebB", "AutoTrapA", "NoFallB", "InventoryB", "InventoryC", "FastBreakB", "PlaceFaceA", "PlaceWallA", "AirPlaceA", "SprintB", "SprintC", "SprintD", "SprintE"}) {
                int vl = data.getVl(extra);
                if (vl > 0) {
                    sender.sendMessage(" §8- §b" + extra + "§8: §c" + vl);
                    any = true;
                }
            }
            if (!any) {
                sender.sendMessage(" §aCzysto - brak naruszen.");
            }
            sender.sendMessage(" §7Podejrzen bez VL: §e" + data.suspiciousCount);
            return true;
        }
        if (sub.equals("clear")) {
            if (!sender.hasPermission("floppaac.admin")) {
                sender.sendMessage(pfx + " §cBrak uprawnien (floppaac.admin).");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(pfx + " §7Uzycie: /" + label + " clear <gracz>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(pfx + " §cGracz offline.");
                return true;
            }
            plugin.getDataManager().get(target).vl.clear();
            sender.sendMessage(pfx + " §aWyzerowano VL gracza " + target.getName() + ".");
            return true;
        }
        if (sub.equals("status")) {
            sender.sendMessage(pfx + " §7FloppaAC " + FloppaAC.VERSION
                    + " checkow: §b" + plugin.getCheckManager().size()
                    + " §7tps: §e" + String.format("%.1f", plugin.getTpsMonitor().getTps())
                    + " §7graczy: §e" + plugin.getDataManager().tracked());
            sender.sendMessage(" §7Tryb: §ekicki zamiast banow §7(setback przy movement).");
            return true;
        }
        if (sub.equals("debug")) {
            Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1])
                    : (sender instanceof Player ? (Player) sender : null);
            if (target == null) {
                sender.sendMessage(pfx + " §cGracz offline.");
                return true;
            }
            PlayerData data = plugin.getDataManager().get(target);
            long now = System.currentTimeMillis();
            int ping = PingUtil.getPing(target, data);
            double cps = ClickStats.cps(data.hitTimes, 1000L);
            double std = ClickStats.stddev(data.clickGaps);
            long sinceMove = now - data.lastMoveMs;
            int burst = FakeLagMath.burstInWindow(data.moveTimes, now, 200L);
            sender.sendMessage(pfx + " §7Telemetria §e" + target.getName() + "§7:");
            sender.sendMessage(" §7ping: §e" + ping
                    + " §7tps: §e" + String.format("%.1f", plugin.getTpsMonitor().getTps()));
            sender.sendMessage(" §7od ruchu: §e" + sinceMove + "ms"
                    + " §7burst200ms: §e" + burst
                    + " §7luka: §e" + data.lastMoveGapMs + "ms");
            sender.sendMessage(" §7cps: §e" + String.format("%.1f", cps)
                    + " §7std: §e" + String.format("%.1f", std) + "ms");
            sender.sendMessage(" §7air: §e" + data.airTicks
                    + " §7choke: §e" + data.chokeStreak
                    + " §7podejrzen: §e" + data.suspiciousCount);
            return true;
        }
        if (sub.equals("reload")) {
            if (!sender.hasPermission("floppaac.admin")) {
                sender.sendMessage(pfx + " §cBrak uprawnien (floppaac.admin).");
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage(pfx + " §aConfig przeladowany.");
            return true;
        }
        sender.sendMessage(pfx + " §cNieznana podkomenda.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd,
                                      String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList(
                    "alerts", "verbose", "vl", "clear", "status", "debug", "reload"), args[0]);
        }
        return new ArrayList<String>();
    }

    private List<String> filter(List<String> options, String prefix) {
        List<String> out = new ArrayList<String>();
        for (String o : options) {
            if (o.startsWith(prefix.toLowerCase())) {
                out.add(o);
            }
        }
        return out;
    }
}
