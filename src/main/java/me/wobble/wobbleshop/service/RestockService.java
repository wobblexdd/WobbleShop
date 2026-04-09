package me.wobble.wobbleshop.service;

import me.wobble.wobbleshop.WobbleShopPlugin;
import me.wobble.wobbleshop.config.ConfigManager;
import org.bukkit.scheduler.BukkitTask;

public final class RestockService {

    private final WobbleShopPlugin plugin;
    private final ShopService shopService;
    private final ConfigManager configManager;
    private BukkitTask task;

    public RestockService(WobbleShopPlugin plugin, ShopService shopService, ConfigManager configManager) {
        this.plugin = plugin;
        this.shopService = shopService;
        this.configManager = configManager;
    }

    public void start() {
        stop();
        if (!configManager.isRestockTaskEnabled()) {
            return;
        }

        long interval = configManager.getRestockCheckIntervalTicks();
        task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                int restocked = shopService.restockDueItems();
                if (restocked > 0) {
                    plugin.getLogger().info("Automatic restock refreshed " + restocked + " shop items.");
                }
            } catch (Exception exception) {
                plugin.getLogger().warning("Automatic restock failed: " + exception.getMessage());
            }
        }, interval, interval);
    }

    public void restart() {
        start();
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
