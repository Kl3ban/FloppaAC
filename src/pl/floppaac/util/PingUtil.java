package pl.floppaac.util;

import org.bukkit.entity.Player;
import pl.floppaac.data.PlayerData;

import java.lang.reflect.Method;

/**
 * Odczyt pingu gracza przez refleksje (bez NMS w kodzie).
 * Dziala na Spigot, Paper i Purpur. Fallback 0 gdy brak metody.
 * Wynik cache 2 s aby nie wywolywac refleksji co tick.
 */
public final class PingUtil {

    private static Method getPingMethod;

    static {
        try {
            getPingMethod = Player.class.getMethod("getPing");
        } catch (NoSuchMethodException e) {
            getPingMethod = null;
        }
    }

    private PingUtil() {
    }

    public static int getPing(Player player, PlayerData data) {
        long now = System.currentTimeMillis();
        if (data != null && now - data.pingCacheTime < 2000L) {
            return data.pingCache;
        }
        int ping = 0;
        if (getPingMethod != null) {
            try {
                Object o = getPingMethod.invoke(player);
                if (o instanceof Number) {
                    ping = ((Number) o).intValue();
                }
            } catch (Exception ignored) {
                ping = 0;
            }
        }
        if (data != null) {
            data.pingCache = ping;
            data.pingCacheTime = now;
            data.pingEma = (int) (data.pingEma * 0.7 + ping * 0.3);
        }
        return ping;
    }

    /** Luz progow jako ulamek 0.0 do 1.0. Krzywa schodkowa, bo desync
     *  3 tickow istnieje takze przy niskim pingu. */
    public static double leniency(org.bukkit.configuration.file.FileConfiguration cfg, int ping) {
        if (ping >= cfg.getInt("high-ping", 220)) {
            return cfg.getDouble("high-ping-leniency", 0.35);
        }
        if (ping >= 180) {
            return 0.15;
        }
        if (ping >= 100) {
            return 0.08;
        }
        return 0.0;
    }
}
