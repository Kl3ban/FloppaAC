package pl.floppaac.check.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.PingUtil;

/**
 * LagGuard: ochrona przed naduzyciem FakeLag przez zalewanie.
 * Gdy lacze gracza gubi ruch na ponad 900 ms wiele razy w minute,
 * a serwer jest zdrowy, gracz dostaje kick za niestabilne polaczenie.
 * To NIE jest flaga cheata, tylko bezpiecznik jakosci lacza.
 * Domyslnie wlaczony, progi lzejsze niz wykrywanie FakeLag.
 */
public class LagGuardCheck extends Check {

    public LagGuardCheck(FloppaAC plugin) {
        super(plugin, "LagGuard", CheckType.PLAYER);
    }

    public void handleGap(Player player, PlayerData data, long gapMs) {
        if (!isEnabled()) {
            return;
        }
        if (data.movementExempt()) {
            return;
        }
        if (player.hasPermission("floppaac.bypass")) {
            return;
        }
        // Jazda przez niezaladowane chunki rwie ruch legalnie.
        if (player.isInsideVehicle()) {
            return;
        }
        long threshold = (long) cfgInt("gap-ms", 900);
        int maxGaps = cfgInt("max-gaps-per-minute", 6);
        if (gapMs < threshold) {
            return;
        }
        // Cheater z rosnacym VL FakeLag idzie sciezka cheata, nie lagi.
        // Takze swieze wykrycie (30 s) wylacza bezpiecznik: luka to choke.
        if (data.getVl("FakeLagA") + data.getVl("FakeLagB")
                + data.getVl("FakeLagC") >= 2
                || System.currentTimeMillis() - data.lastFakeLagMs < 30000L) {
            return;
        }
        double tps = plugin.getTpsMonitor().getTps();
        if (tps < 18.5) {
            return;
        }
        int ping = PingUtil.getPing(player, data);
        if (ping > 400) {
            // Bardzo wysoki ping to juz inna historia, nie karzemy podwojnie.
            return;
        }
        long now = System.currentTimeMillis();
        PlayerData.pushCapped(data.bigGaps, now, 20);
        while (!data.bigGaps.isEmpty() && now - data.bigGaps.peekFirst() > 60000L) {
            data.bigGaps.pollFirst();
        }
        if (data.bigGaps.size() >= maxGaps) {
            String cmd = plugin.getConfig().getString("lagguard.kick-command",
                    "kick %player% FloppaAC: unstable connection (lag)");
            String c = cmd.replace("%player%", player.getName());
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c);
                }
            });
            plugin.getLogger().info("[LagGuard] " + player.getName()
                    + " kicked for " + data.bigGaps.size() + " gaps in 60 s");
            data.bigGaps.clear();
        } else if (data.bigGaps.size() >= maxGaps - 2) {
            suspicious(player, data, "unstable connection "
                    + data.bigGaps.size() + "/" + maxGaps + " luk");
        }
    }
}
