package me.wobble.wobbleshop.config;

import java.util.LinkedHashMap;
import java.util.Map;
import me.wobble.wobbleshop.WobbleShopPlugin;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
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

    public double getGlobalBuyMultiplier() {
        return Math.max(0.01D, getConfig().getDouble("economy.multipliers.global-buy-multiplier", 1.0D));
    }

    public double getGlobalSellMultiplier() {
        return Math.max(0.01D, getConfig().getDouble("economy.multipliers.global-sell-multiplier", 1.0D));
    }

    public double getCategoryBuyMultiplier(String categoryKey) {
        return Math.max(0.01D, getConfig().getDouble("economy.multipliers.category-buy." + categoryKey, 1.0D));
    }

    public double getCategorySellMultiplier(String categoryKey) {
        return Math.max(0.01D, getConfig().getDouble("economy.multipliers.category-sell." + categoryKey, 1.0D));
    }

    public boolean isDynamicPricingEnabled() {
        return getConfig().getBoolean("economy.dynamic-pricing.enabled", false);
    }

    public double getDynamicStep() {
        return Math.max(0.001D, getConfig().getDouble("economy.dynamic-pricing.step", 0.02D));
    }

    public double getDynamicMinMultiplier() {
        return Math.max(0.1D, getConfig().getDouble("economy.dynamic-pricing.min-multiplier", 0.6D));
    }

    public double getDynamicMaxMultiplier() {
        return Math.max(getDynamicMinMultiplier(), getConfig().getDouble("economy.dynamic-pricing.max-multiplier", 1.6D));
    }

    public boolean isDebugEnabled() {
        return getConfig().getBoolean("debug.enabled", false);
    }

    public boolean logTransactions() {
        return getConfig().getBoolean("debug.log-transactions", false);
    }

    public boolean logFailures() {
        return getConfig().getBoolean("debug.log-failures", true);
    }

    public boolean logStockIssues() {
        return getConfig().getBoolean("debug.log-stock-issues", true);
    }

    public long getCooldownMillis() {
        return Math.max(0L, getConfig().getLong("anti-abuse.cooldown-millis", 200L));
    }

    public int getMaxPerClick() {
        return Math.max(1, getConfig().getInt("anti-abuse.max-transaction-per-click", 64));
    }

    public int getBulkAmount() {
        return Math.max(1, getConfig().getInt("gui.bulk-amount", 16));
    }

    public Map<String, Double> getDiscountGroups() {
        Map<String, Double> discounts = new LinkedHashMap<>();
        ConfigurationSection section = getConfig().getConfigurationSection("economy.discounts");
        if (section == null) {
            return discounts;
        }
        for (String key : section.getKeys(false)) {
            discounts.put(key, Math.max(0.0D, Math.min(0.95D, section.getDouble(key, 0.0D))));
        }
        return discounts;
    }

    public int getDailyLimit(String itemKey) {
        return Math.max(0, getConfig().getInt("anti-abuse.daily-limits." + itemKey, 0));
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
