package me.wobble.wobbleshop.config;

import me.wobble.wobbleshop.WobbleShopPlugin;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

public final class ConfigManager {

    private final WobbleShopPlugin plugin;

    public ConfigManager(WobbleShopPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
    }

    public void reload() {
        plugin.reloadConfig();
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public String getDatabaseFile() {
        return getConfig().getString("database.file", "shop.db");
    }

    public String getCurrencySymbol() {
        return getConfig().getString("shop.currency-symbol", "$");
    }

    public String getTitle(String key, String fallback) {
        return getConfig().getString(key, fallback);
    }

    public boolean areResultSoundsEnabled() {
        return getConfig().getBoolean("sounds.enabled", true);
    }

    public float getResultSoundVolume() {
        return (float) getConfig().getDouble("sounds.volume", 1.0D);
    }

    public float getResultSoundPitch() {
        return (float) getConfig().getDouble("sounds.pitch", 1.0D);
    }

    public Sound getSuccessSound() {
        return parseSound(getConfig().getString("sounds.success", "ENTITY_EXPERIENCE_ORB_PICKUP"), Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
    }

    public Sound getFailureSound() {
        return parseSound(getConfig().getString("sounds.failure", "ENTITY_VILLAGER_NO"), Sound.ENTITY_VILLAGER_NO);
    }

    public long getRestockCheckIntervalTicks() {
        long seconds = getConfig().getLong("restock.check-interval-seconds", 60L);
        return Math.max(20L, seconds * 20L);
    }

    public boolean isRestockTaskEnabled() {
        return getConfig().getBoolean("restock.task-enabled", true);
    }

    public boolean allowSellAboveBuy() {
        return getConfig().getBoolean("economy.allow-sell-above-buy", false);
    }

    public int getItemsPerPage() {
        return Math.max(1, getConfig().getInt("gui.items-per-page", 28));
    }

    public Material getFillerMaterial() {
        Material material = Material.matchMaterial(getConfig().getString("gui.filler-material", "GRAY_STAINED_GLASS_PANE"));
        return material == null ? Material.GRAY_STAINED_GLASS_PANE : material;
    }

    private Sound parseSound(String value, Sound fallback) {
        try {
            return Sound.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
