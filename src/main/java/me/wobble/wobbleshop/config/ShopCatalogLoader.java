package me.wobble.wobbleshop.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import me.wobble.wobbleshop.WobbleShopPlugin;
import me.wobble.wobbleshop.model.ShopCategory;
import me.wobble.wobbleshop.model.ShopItem;
import me.wobble.wobbleshop.model.ShopStatus;
import me.wobble.wobbleshop.model.StockType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ShopCatalogLoader {

    private final WobbleShopPlugin plugin;

    public ShopCatalogLoader(WobbleShopPlugin plugin) {
        this.plugin = plugin;
    }

    public void ensureDefaults() {
        saveResourceIfMissing("categories.yml");
        saveResourceIfMissing("shop-items.yml");
    }

    public LoadedCatalog loadCatalog() {
        YamlConfiguration categoriesConfig = loadYaml("categories.yml");
        YamlConfiguration itemsConfig = loadYaml("shop-items.yml");

        List<ShopCategory> categories = loadCategories(categoriesConfig);
        List<ShopItem> items = loadItems(itemsConfig);
        return new LoadedCatalog(categories, items);
    }

    private List<ShopCategory> loadCategories(YamlConfiguration config) {
        List<ShopCategory> categories = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("categories");
        if (section == null) {
            return categories;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection categorySection = section.getConfigurationSection(key);
            if (categorySection == null) {
                continue;
            }

            Material material = parseMaterial(categorySection.getString("material"), Material.CHEST);
            String displayName = categorySection.getString("display-name", prettifyKey(key));
            int slot = clamp(categorySection.getInt("slot", 10), 0, 26);
            boolean enabled = categorySection.getBoolean("enabled", true);
            categories.add(new ShopCategory(key.toLowerCase(Locale.ROOT), displayName, material, slot, enabled));
        }

        return categories;
    }

    private List<ShopItem> loadItems(YamlConfiguration config) {
        List<ShopItem> items = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("items");
        if (section == null) {
            return items;
        }

        long now = System.currentTimeMillis();
        for (String itemKey : section.getKeys(false)) {
            ConfigurationSection itemSection = section.getConfigurationSection(itemKey);
            if (itemSection == null) {
                continue;
            }

            boolean hidden = itemSection.getBoolean("hidden", false);
            if (hidden) {
                continue;
            }

            String category = itemSection.getString("category", "special").toLowerCase(Locale.ROOT);
            Material material = parseMaterial(itemSection.getString("material"), Material.STONE);
            String displayName = itemSection.getString("display-name", prettifyMaterial(material));
            int slot = clamp(itemSection.getInt("slot", 10), 0, 53);
            boolean enabled = itemSection.getBoolean("enabled", true);

            ConfigurationSection buySection = itemSection.getConfigurationSection("buy");
            boolean buyEnabled = buySection != null && buySection.getBoolean("enabled", false);
            double buyPrice = buySection != null ? Math.max(0.0D, buySection.getDouble("price", 0.0D)) : 0.0D;

            ConfigurationSection sellSection = itemSection.getConfigurationSection("sell");
            boolean sellEnabled = sellSection != null && sellSection.getBoolean("enabled", false);
            double sellPrice = sellSection != null ? Math.max(0.0D, sellSection.getDouble("price", 0.0D)) : 0.0D;

            ConfigurationSection stockSection = itemSection.getConfigurationSection("stock");
            StockType stockType = parseStockType(stockSection == null ? "STATIC" : stockSection.getString("type", "STATIC"));
            int stock = stockType == StockType.STATIC ? 0 : Math.max(0, stockSection.getInt("amount", stockSection.getInt("max", 64)));
            int maxStock = Math.max(1, stockSection == null ? 256 : stockSection.getInt("max", 256));
            boolean restockEnabled = stockType == StockType.LIMITED && stockSection != null && stockSection.getBoolean("restock-enabled", false);
            long restockInterval = Math.max(60L, stockSection == null ? 3600L : stockSection.getLong("restock-interval-seconds", 3600L));

            ShopStatus status = enabled
                    ? parseStatus(itemSection.getString("status", "ACTIVE"))
                    : ShopStatus.DISABLED;

            List<String> lore = new ArrayList<>(itemSection.getStringList("lore"));
            String progression = itemSection.getString("min-progression-comment", "").trim();
            if (!progression.isEmpty()) {
                lore.add("&8Progression: " + progression);
            }

            items.add(new ShopItem(
                    0,
                    material,
                    displayName,
                    category,
                    slot,
                    buyPrice,
                    sellPrice,
                    buyEnabled,
                    sellEnabled,
                    stockType,
                    Math.min(stock, maxStock),
                    maxStock,
                    restockEnabled,
                    restockInterval,
                    now,
                    status,
                    lore
            ));
        }

        return items;
    }

    private YamlConfiguration loadYaml(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        return YamlConfiguration.loadConfiguration(file);
    }

    private void saveResourceIfMissing(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
    }

    private Material parseMaterial(String value, Material fallback) {
        if (value == null) {
            return fallback;
        }
        Material material = Material.matchMaterial(value);
        return material == null ? fallback : material;
    }

    private StockType parseStockType(String value) {
        if (value == null) {
            return StockType.STATIC;
        }
        try {
            return StockType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return StockType.STATIC;
        }
    }

    private ShopStatus parseStatus(String value) {
        if (value == null) {
            return ShopStatus.ACTIVE;
        }
        try {
            return ShopStatus.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ShopStatus.ACTIVE;
        }
    }

    private String prettifyMaterial(Material material) {
        return prettifyKey(material.name().toLowerCase(Locale.ROOT));
    }

    private String prettifyKey(String key) {
        String[] parts = key.replace('_', ' ').split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record LoadedCatalog(List<ShopCategory> categories, List<ShopItem> items) {
    }
}
