package me.wobble.wobbleshop.service;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.wobble.wobbleshop.WobbleShopPlugin;
import me.wobble.wobbleshop.manager.EconomyManager;
import me.wobble.wobbleshop.model.ShopCategory;
import me.wobble.wobbleshop.model.ShopItem;
import me.wobble.wobbleshop.model.ShopStatus;
import me.wobble.wobbleshop.model.StockType;
import me.wobble.wobbleshop.model.TransactionResult;
import me.wobble.wobbleshop.repository.CategoryRepository;
import me.wobble.wobbleshop.repository.ShopItemRepository;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class ShopService {

    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("0.00");

    private final WobbleShopPlugin plugin;
    private final CategoryRepository categoryRepository;
    private final ShopItemRepository shopItemRepository;
    private final EconomyManager economyManager;

    public ShopService(WobbleShopPlugin plugin, CategoryRepository categoryRepository,
                       ShopItemRepository shopItemRepository, EconomyManager economyManager) {
        this.plugin = plugin;
        this.categoryRepository = categoryRepository;
        this.shopItemRepository = shopItemRepository;
        this.economyManager = economyManager;
    }

    public synchronized void bootstrap() {
        if (categoryRepository.isEmpty()) {
            seedCategories();
        }
        if (shopItemRepository.isEmpty()) {
            seedItems();
        }
    }

    public synchronized void reload() {
        economyManager.refresh();
    }

    public synchronized List<ShopCategory> getCategories() {
        return categoryRepository.findAll().stream()
                .filter(ShopCategory::isEnabled)
                .sorted(Comparator.comparingInt(ShopCategory::getSlot))
                .toList();
    }

    public synchronized List<ShopCategory> getAllCategories() {
        return categoryRepository.findAll().stream()
                .sorted(Comparator.comparingInt(ShopCategory::getSlot))
                .toList();
    }

    public synchronized Optional<ShopCategory> getCategory(String key) {
        return getAllCategories().stream()
                .filter(category -> category.getKey().equalsIgnoreCase(key))
                .findFirst();
    }

    public synchronized List<ShopItem> getItemsByCategory(String categoryKey) {
        return shopItemRepository.findByCategory(categoryKey);
    }

    public synchronized List<ShopItem> getAllItems() {
        return shopItemRepository.findAll();
    }

    public synchronized Optional<ShopItem> getItem(int id) {
        return shopItemRepository.findById(id);
    }

    public synchronized long countLimitedStockItems() {
        return getAllItems().stream()
                .filter(ShopItem::isLimitedStock)
                .count();
    }

    public synchronized ShopItem createItem(String categoryKey) {
        int slot = nextOpenSlot(categoryKey);
        Material material = firstSuggestedMaterial(categoryKey);
        ShopItem item = ShopItem.createDefault(categoryKey, slot, material, prettifyMaterial(material));
        validateItem(item);
        shopItemRepository.save(item);
        return item;
    }

    public synchronized ShopItem saveItem(ShopItem item) {
        validateItem(item);
        return shopItemRepository.save(item);
    }

    public synchronized void deleteItem(int id) {
        shopItemRepository.delete(id);
    }

    public synchronized int restockDueItems() {
        long now = System.currentTimeMillis();
        int count = 0;
        for (ShopItem item : getAllItems()) {
            if (!item.canAutoRestock(now)) {
                continue;
            }
            item.restockToMax();
            saveItem(item);
            count++;
        }
        return count;
    }

    public synchronized int restockAll() {
        int count = 0;
        for (ShopItem item : getAllItems()) {
            if (!item.canManualRestock()) {
                continue;
            }
            item.restockToMax();
            saveItem(item);
            count++;
        }
        return count;
    }

    public synchronized int restockCategory(String categoryKey) {
        int count = 0;
        for (ShopItem item : getItemsByCategory(categoryKey)) {
            if (!item.canManualRestock()) {
                continue;
            }
            item.restockToMax();
            saveItem(item);
            count++;
        }
        return count;
    }

    public synchronized boolean restockItem(int id) {
        ShopItem item = getItem(id).orElse(null);
        if (item == null || !item.canManualRestock()) {
            return false;
        }
        item.restockToMax();
        saveItem(item);
        return true;
    }

    public int countSellableItems(Player player, ShopItem item) {
        return countMaterial(player.getInventory(), item.getMaterial());
    }

    public int getSellAllAmount(Player player, ShopItem item) {
        return countSellableItems(player, item);
    }

    public ShopStatus getDisplayStatus(ShopItem item) {
        return item.getResolvedStatus();
    }

    public boolean isBuyAvailable(ShopItem item, int amount) {
        return item.isBuyEnabled()
                && item.getBuyPrice() > 0.0D
                && getDisplayStatus(item) == ShopStatus.ACTIVE
                && item.hasStockForBuy(amount);
    }

    public boolean isSellAvailable(ShopItem item) {
        return item.isSellEnabled()
                && item.getSellPrice() > 0.0D
                && item.getStatus() != ShopStatus.DISABLED
                && item.getStatus() != ShopStatus.COMING_SOON;
    }

    public synchronized TransactionResult buy(Player player, ShopItem item, int amount) {
        if (amount <= 0) {
            return TransactionResult.failure("invalid-action");
        }

        TransactionResult validation = validateBuy(item, amount);
        if (validation != null) {
            return validation;
        }

        double totalPrice = item.getBuyPrice() * amount;
        if (!economyManager.has(player, totalPrice)) {
            return TransactionResult.failure("insufficient-funds", Map.of(
                    "amount", formatPrice(totalPrice),
                    "item", item.getDisplayName()
            ));
        }
        if (!canFit(player.getInventory(), item.getMaterial(), amount)) {
            return TransactionResult.failure("inventory-full");
        }
        if (!economyManager.withdraw(player, totalPrice).transactionSuccess()) {
            return TransactionResult.failure("shop-unavailable");
        }

        player.getInventory().addItem(new ItemStack(item.getMaterial(), amount));
        if (item.isLimitedStock()) {
            item.reduceStock(amount);
            saveItem(item);
        }

        return TransactionResult.success("bought-item", Map.of(
                "amount", String.valueOf(amount),
                "item", item.getDisplayName(),
                "price", formatPrice(totalPrice),
                "stock", item.isLimitedStock() ? String.valueOf(item.getStock()) : plugin.getMessageManager().getRaw("stock-infinite")
        ));
    }

    public synchronized TransactionResult sell(Player player, ShopItem item, int amount) {
        if (amount <= 0) {
            return TransactionResult.failure("invalid-action");
        }

        TransactionResult validation = validateSell(item);
        if (validation != null) {
            return validation;
        }
        if (countSellableItems(player, item) < amount) {
            return TransactionResult.failure("insufficient-items", Map.of(
                    "amount", String.valueOf(amount),
                    "item", item.getDisplayName()
            ));
        }

        return completeSell(player, item, amount);
    }

    public synchronized TransactionResult sellAll(Player player, ShopItem item) {
        TransactionResult validation = validateSell(item);
        if (validation != null) {
            return validation;
        }

        int sellAmount = getSellAllAmount(player, item);
        if (sellAmount <= 0) {
            return TransactionResult.failure("insufficient-items", Map.of(
                    "amount", "1",
                    "item", item.getDisplayName()
            ));
        }

        return completeSell(player, item, sellAmount);
    }

    public String formatPrice(double price) {
        String formatted = economyManager.format(price);
        if (formatted.equals(String.format("%.2f", price))) {
            return plugin.getConfigManager().getCurrencySymbol() + PRICE_FORMAT.format(price);
        }
        return formatted;
    }

    public String getStatusLabel(ShopStatus status) {
        return switch (status) {
            case ACTIVE -> plugin.getMessageManager().getRaw("status-active");
            case OUT_OF_STOCK -> plugin.getMessageManager().getRaw("status-out-of-stock");
            case COMING_SOON -> plugin.getMessageManager().getRaw("status-coming-soon");
            case DISABLED -> plugin.getMessageManager().getRaw("status-disabled");
        };
    }

    public String getDisplayStatusLabel(ShopItem item) {
        return getStatusLabel(getDisplayStatus(item));
    }

    public String getBuyDisplay(ShopItem item) {
        if (item.isBuyEnabled() && item.getBuyPrice() > 0.0D) {
            return "&a" + formatPrice(item.getBuyPrice());
        }
        return "&8Unavailable";
    }

    public String getSellDisplay(ShopItem item) {
        if (item.isSellEnabled() && item.getSellPrice() > 0.0D) {
            return "&a" + formatPrice(item.getSellPrice());
        }
        return "&8Unavailable";
    }

    public String getStockTypeDisplay(ShopItem item) {
        return item.isStaticStock()
                ? plugin.getMessageManager().getRaw("stock-type-static")
                : plugin.getMessageManager().getRaw("stock-type-limited");
    }

    public String getStockDisplay(ShopItem item) {
        if (item.isStaticStock()) {
            return plugin.getMessageManager().getRaw("stock-infinite");
        }
        return "&f" + item.getStock();
    }

    public String getStockDetailDisplay(ShopItem item) {
        if (item.isStaticStock()) {
            return plugin.getMessageManager().getRaw("stock-infinite");
        }
        return "&f" + item.getStock() + "&7/&f" + item.getMaxStock();
    }

    public List<Material> getCategoryMaterialCycle(String categoryKey) {
        return switch (categoryKey.toLowerCase()) {
            case "farming" -> List.of(Material.WHEAT, Material.CARROT, Material.POTATO, Material.BEETROOT, Material.SUGAR_CANE);
            case "mining" -> List.of(Material.COBBLESTONE, Material.COAL, Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND);
            case "mobdrops" -> List.of(Material.ROTTEN_FLESH, Material.BONE, Material.STRING, Material.GUNPOWDER, Material.SPIDER_EYE);
            default -> List.of(Material.CHEST, Material.STONE, Material.EMERALD);
        };
    }

    public List<String> getCategoryKeys() {
        return getAllCategories().stream().map(ShopCategory::getKey).toList();
    }

    public String prettifyMaterial(Material material) {
        String value = material.name().toLowerCase().replace('_', ' ');
        String[] parts = value.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private TransactionResult validateBuy(ShopItem item, int amount) {
        if (!economyManager.isAvailable()) {
            return TransactionResult.failure("shop-unavailable");
        }

        ShopStatus status = getDisplayStatus(item);
        if (status == ShopStatus.OUT_OF_STOCK) {
            return TransactionResult.failure("out-of-stock");
        }
        if (status == ShopStatus.COMING_SOON) {
            return TransactionResult.failure("coming-soon");
        }
        if (status == ShopStatus.DISABLED) {
            return TransactionResult.failure("item-disabled");
        }
        if (!item.isBuyEnabled() || item.getBuyPrice() <= 0.0D) {
            return TransactionResult.failure("buy-disabled");
        }
        if (!item.hasStockForBuy(amount)) {
            return TransactionResult.failure("insufficient-stock", Map.of(
                    "stock", String.valueOf(item.getStock()),
                    "item", item.getDisplayName()
            ));
        }
        return null;
    }

    private TransactionResult validateSell(ShopItem item) {
        if (!economyManager.isAvailable()) {
            return TransactionResult.failure("shop-unavailable");
        }
        if (item.getStatus() == ShopStatus.COMING_SOON) {
            return TransactionResult.failure("coming-soon");
        }
        if (item.getStatus() == ShopStatus.DISABLED) {
            return TransactionResult.failure("item-disabled");
        }
        if (!item.isSellEnabled() || item.getSellPrice() <= 0.0D) {
            return TransactionResult.failure("sell-disabled");
        }
        return null;
    }

    private TransactionResult completeSell(Player player, ShopItem item, int amount) {
        double totalPrice = item.getSellPrice() * amount;
        removeMaterial(player.getInventory(), item.getMaterial(), amount);
        if (!economyManager.deposit(player, totalPrice).transactionSuccess()) {
            player.getInventory().addItem(new ItemStack(item.getMaterial(), amount));
            return TransactionResult.failure("shop-unavailable");
        }

        return TransactionResult.success("sold-item", Map.of(
                "amount", String.valueOf(amount),
                "item", item.getDisplayName(),
                "price", formatPrice(totalPrice)
        ));
    }

    private void seedCategories() {
        List<ShopCategory> categories = List.of(
                new ShopCategory("farming", "&aFarming", Material.WHEAT, 11, true),
                new ShopCategory("mining", "&7Mining", Material.IRON_PICKAXE, 13, true),
                new ShopCategory("mobdrops", "&cMob Drops", Material.BONE, 15, true)
        );
        categories.forEach(categoryRepository::save);
    }

    private void seedItems() {
        List<ShopItem> items = new ArrayList<>();
        items.add(new ShopItem(0, Material.WHEAT, "&eWheat Bundle", "farming", 10, 16.0D, 8.0D, false, true,
                StockType.STATIC, 0, 256, false, 3600L, System.currentTimeMillis(), ShopStatus.ACTIVE,
                List.of("&7Basic farm produce.", "&7Stable sell value for early economy.")));
        items.add(new ShopItem(0, Material.CARROT, "&6Carrots", "farming", 12, 20.0D, 9.5D, false, true,
                StockType.STATIC, 0, 256, false, 3600L, System.currentTimeMillis(), ShopStatus.ACTIVE,
                List.of("&7Bulk carrots for fast turnover.")));
        items.add(new ShopItem(0, Material.COBBLESTONE, "&7Cobblestone", "mining", 10, 12.0D, 4.0D, false, true,
                StockType.STATIC, 0, 512, false, 3600L, System.currentTimeMillis(), ShopStatus.ACTIVE,
                List.of("&7Useful for quarry-heavy servers.")));
        items.add(new ShopItem(0, Material.COAL, "&8Coal", "mining", 12, 32.0D, 14.0D, true, true,
                StockType.LIMITED, 96, 128, true, 1800L, System.currentTimeMillis(), ShopStatus.ACTIVE,
                List.of("&7Limited stock example.", "&7Buying reduces remaining stock.")));
        items.add(new ShopItem(0, Material.ROTTEN_FLESH, "&2Rotten Flesh", "mobdrops", 10, 10.0D, 3.0D, false, true,
                StockType.STATIC, 0, 256, false, 3600L, System.currentTimeMillis(), ShopStatus.ACTIVE,
                List.of("&7Common mob drop sell item.")));
        items.add(new ShopItem(0, Material.STRING, "&fString", "mobdrops", 12, 18.0D, 7.0D, true, true,
                StockType.LIMITED, 64, 128, true, 2400L, System.currentTimeMillis(), ShopStatus.COMING_SOON,
                List.of("&7Buy path exists but item status keeps it staged.")));
        items.forEach(this::saveItem);
    }

    private int nextOpenSlot(String categoryKey) {
        List<Integer> used = getItemsByCategory(categoryKey).stream()
                .map(ShopItem::getSlot)
                .sorted()
                .toList();
        for (int slot = 10; slot <= 43; slot++) {
            if (!used.contains(slot) && slot % 9 != 8) {
                return slot;
            }
        }
        return 10;
    }

    private Material firstSuggestedMaterial(String categoryKey) {
        return getCategoryMaterialCycle(categoryKey).get(0);
    }

    private void validateItem(ShopItem item) {
        item.setBuyPrice(Math.max(0.0D, item.getBuyPrice()));
        item.setSellPrice(Math.max(0.0D, item.getSellPrice()));
        item.setSlot(Math.max(0, Math.min(53, item.getSlot())));
        item.setMaxStock(Math.max(1, item.getMaxStock()));
        item.setRestockInterval(Math.max(60L, item.getRestockInterval()));

        if (!plugin.getConfigManager().allowSellAboveBuy()
                && item.isBuyEnabled()
                && item.isSellEnabled()
                && item.getBuyPrice() > 0.0D
                && item.getSellPrice() > item.getBuyPrice()) {
            item.setSellPrice(Math.max(0.0D, item.getBuyPrice() - 0.01D));
        }

        if (item.isStaticStock()) {
            item.setStock(0);
            item.setRestockEnabled(false);
            if (item.getStatus() == ShopStatus.OUT_OF_STOCK) {
                item.setStatus(ShopStatus.ACTIVE);
            }
            return;
        }

        item.setStock(Math.max(0, Math.min(item.getStock(), item.getMaxStock())));
        if (item.getStock() > 0 && item.getStatus() == ShopStatus.OUT_OF_STOCK) {
            item.setStatus(ShopStatus.ACTIVE);
        }
    }

    private boolean canFit(PlayerInventory inventory, Material material, int amount) {
        int remaining = amount;
        for (ItemStack itemStack : inventory.getStorageContents()) {
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                remaining -= material.getMaxStackSize();
            } else if (itemStack.getType() == material) {
                remaining -= material.getMaxStackSize() - itemStack.getAmount();
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private int countMaterial(PlayerInventory inventory, Material material) {
        int amount = 0;
        for (ItemStack itemStack : inventory.getStorageContents()) {
            if (itemStack != null && itemStack.getType() == material) {
                amount += itemStack.getAmount();
            }
        }
        return amount;
    }

    private void removeMaterial(PlayerInventory inventory, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = inventory.getStorageContents();
        for (int index = 0; index < contents.length && remaining > 0; index++) {
            ItemStack itemStack = contents[index];
            if (itemStack == null || itemStack.getType() != material) {
                continue;
            }
            int taken = Math.min(remaining, itemStack.getAmount());
            itemStack.setAmount(itemStack.getAmount() - taken);
            if (itemStack.getAmount() <= 0) {
                contents[index] = null;
            }
            remaining -= taken;
        }
        inventory.setStorageContents(contents);
    }
}
