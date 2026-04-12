package me.wobble.wobbleshop.service;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import me.wobble.wobbleshop.WobbleShopPlugin;
import me.wobble.wobbleshop.config.ShopCatalogLoader;
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
    private final ShopCatalogLoader catalogLoader;

    private final Map<Integer, PriceState> dynamicState = new HashMap<>();
    private final Map<String, Long> cooldownTracker = new HashMap<>();
    private final Map<String, Integer> dailyTracker = new HashMap<>();

    public ShopService(WobbleShopPlugin plugin, CategoryRepository categoryRepository,
                       ShopItemRepository shopItemRepository, EconomyManager economyManager,
                       ShopCatalogLoader catalogLoader) {
        this.plugin = plugin;
        this.categoryRepository = categoryRepository;
        this.shopItemRepository = shopItemRepository;
        this.economyManager = economyManager;
        this.catalogLoader = catalogLoader;
    }

    public synchronized void bootstrap() {
        syncCatalogFromConfig();
    }

    public synchronized void reload() {
        economyManager.refresh();
        syncCatalogFromConfig();
    }

    public synchronized List<ShopCategory> getCategories() {
        return categoryRepository.findAll().stream()
                .filter(ShopCategory::isEnabled)
                .sorted(Comparator.comparingInt(ShopCategory::getSlot))
                .toList();
    }

    public synchronized List<ShopCategory> getVisibleCategories(Player player) {
        return getCategories().stream().filter(category -> canAccessCategory(player, category)).toList();
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
            if (item.isRegeneratingStock()) {
                item.restockIncrement();
            } else {
                item.restockToMax();
            }
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
                && getEffectiveBuyPrice(item) > 0.0D
                && getDisplayStatus(item) == ShopStatus.ACTIVE
                && item.hasStockForBuy(amount);
    }

    public boolean isSellAvailable(ShopItem item) {
        return item.isSellEnabled()
                && getEffectiveSellPrice(item) > 0.0D
                && item.getStatus() != ShopStatus.DISABLED
                && item.getStatus() != ShopStatus.COMING_SOON;
    }

    public synchronized TransactionResult buy(Player player, ShopItem item, int requestedAmount) {
        int amount = Math.min(requestedAmount, plugin.getConfigManager().getMaxPerClick());
        if (amount <= 0) {
            return TransactionResult.failure("invalid-action");
        }

        TransactionResult throttle = checkThrottle(player, item, amount, true);
        if (throttle != null) {
            return throttle;
        }

        TransactionResult validation = validateBuy(player, item, amount);
        if (validation != null) {
            debugFailure("buy", player, item, validation.messageKey());
            return validation;
        }

        double totalPrice = getEffectiveBuyPriceFor(player, item) * amount;
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

        recordDemand(item, amount);
        logTransaction("BUY", player, item, amount, totalPrice);

        return TransactionResult.success("bought-item", Map.of(
                "amount", String.valueOf(amount),
                "item", item.getDisplayName(),
                "price", formatPrice(totalPrice),
                "stock", item.isLimitedStock() ? String.valueOf(item.getStock()) : plugin.getMessageManager().getRaw("stock-infinite")
        ));
    }

    public synchronized TransactionResult sell(Player player, ShopItem item, int requestedAmount) {
        int amount = Math.min(requestedAmount, plugin.getConfigManager().getMaxPerClick());
        if (amount <= 0) {
            return TransactionResult.failure("invalid-action");
        }

        TransactionResult throttle = checkThrottle(player, item, amount, false);
        if (throttle != null) {
            return throttle;
        }

        TransactionResult validation = validateSell(item);
        if (validation != null) {
            debugFailure("sell", player, item, validation.messageKey());
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

        int capped = Math.min(sellAmount, plugin.getConfigManager().getMaxPerClick());
        return completeSell(player, item, capped);
    }

    public double getEffectiveBuyPrice(ShopItem item) {
        return applyDynamic(item, item.getBuyPrice());
    }

    public double getEffectiveBuyPriceFor(Player player, ShopItem item) {
        double price = getEffectiveBuyPrice(item);
        double discount = getPlayerDiscount(player);
        return price * (1.0D - discount);
    }

    public double getEffectiveSellPrice(ShopItem item) {
        return applyDynamic(item, item.getSellPrice());
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
            return "&a" + formatPrice(getEffectiveBuyPrice(item));
        }
        return "&8Unavailable";
    }

    public String getSellDisplay(ShopItem item) {
        if (item.isSellEnabled() && item.getSellPrice() > 0.0D) {
            return "&a" + formatPrice(getEffectiveSellPrice(item));
        }
        return "&8Unavailable";
    }

    public String getStockTypeDisplay(ShopItem item) {
        if (item.getStockType() == StockType.REGENERATING) {
            return "&bRegenerating";
        }
        return item.isStaticStock()
                ? plugin.getMessageManager().getRaw("stock-type-static")
                : plugin.getMessageManager().getRaw("stock-type-limited");
    }

    public String getStockDetailDisplay(ShopItem item) {
        if (item.isStaticStock()) {
            return plugin.getMessageManager().getRaw("stock-infinite");
        }
        return "&f" + item.getStock() + "&7/&f" + item.getMaxStock();
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

    private TransactionResult validateBuy(Player player, ShopItem item, int amount) {
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
        if (!item.isBuyEnabled() || getEffectiveBuyPrice(item) <= 0.0D) {
            return TransactionResult.failure("buy-disabled");
        }
        if (!player.hasPermission("shop.bypass.stock") && !item.hasStockForBuy(amount)) {
            if (plugin.getConfigManager().logStockIssues()) {
                plugin.getLogger().info("Stock blocked buy for " + item.getId() + " remaining=" + item.getStock());
            }
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
        if (!item.isSellEnabled() || getEffectiveSellPrice(item) <= 0.0D) {
            return TransactionResult.failure("sell-disabled");
        }
        return null;
    }

    private TransactionResult completeSell(Player player, ShopItem item, int amount) {
        double totalPrice = getEffectiveSellPrice(item) * amount;
        removeMaterial(player.getInventory(), item.getMaterial(), amount);
        if (!economyManager.deposit(player, totalPrice).transactionSuccess()) {
            player.getInventory().addItem(new ItemStack(item.getMaterial(), amount));
            return TransactionResult.failure("shop-unavailable");
        }

        recordSupply(item, amount);
        logTransaction("SELL", player, item, amount, totalPrice);

        return TransactionResult.success("sold-item", Map.of(
                "amount", String.valueOf(amount),
                "item", item.getDisplayName(),
                "price", formatPrice(totalPrice)
        ));
    }

    private void syncCatalogFromConfig() {
        ShopCatalogLoader.LoadedCatalog loadedCatalog = catalogLoader.loadCatalog();
        shopItemRepository.deleteAll();
        categoryRepository.deleteAll();
        dynamicState.clear();

        for (ShopCategory category : loadedCatalog.categories()) {
            categoryRepository.save(category);
        }

        for (ShopItem item : loadedCatalog.items()) {
            if (getCategory(item.getCategory()).isEmpty()) {
                continue;
            }
            saveItem(item);
        }
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
        return switch (categoryKey.toLowerCase(Locale.ROOT)) {
            case "blocks" -> Material.STONE;
            case "food" -> Material.BREAD;
            case "farming" -> Material.WHEAT;
            case "mobdrops" -> Material.BONE;
            case "nether" -> Material.NETHERRACK;
            case "end" -> Material.END_STONE;
            case "utility" -> Material.TORCH;
            case "combat" -> Material.ARROW;
            default -> Material.CHEST;
        };
    }

    private void validateItem(ShopItem item) {
        item.setBuyPrice(Math.max(0.0D, item.getBuyPrice()));
        item.setSellPrice(Math.max(0.0D, item.getSellPrice()));
        item.setSlot(Math.max(0, Math.min(53, item.getSlot())));
        item.setMaxStock(Math.max(1, item.getMaxStock()));
        item.setRestockInterval(Math.max(60L, item.getRestockInterval()));
        item.setRestockAmount(Math.max(1, item.getRestockAmount()));

        if (item.isBuyEnabled() && item.isSellEnabled() && item.getBuyPrice() > 0.0D && item.getSellPrice() >= item.getBuyPrice()) {
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

    private boolean canAccessCategory(Player player, ShopCategory category) {
        if (category.isAdminOnly() && !player.hasPermission("wobble.shop.admin")) {
            return false;
        }
        if (category.getPermission() != null && !category.getPermission().isBlank() && !player.hasPermission(category.getPermission())) {
            return false;
        }
        return player.hasPermission("shop.category." + category.getKey()) || !plugin.getConfigManager().getConfig().getBoolean("categories.require-specific-permission", false);
    }

    private double getPlayerDiscount(Player player) {
        double best = 0.0D;
        for (Map.Entry<String, Double> entry : plugin.getConfigManager().getDiscountGroups().entrySet()) {
            if (player.hasPermission("shop.discount." + entry.getKey())) {
                best = Math.max(best, entry.getValue());
            }
        }
        return best;
    }

    private String key(Player player, ShopItem item) {
        return player.getUniqueId() + ":" + item.getId();
    }

    private TransactionResult checkThrottle(Player player, ShopItem item, int amount, boolean buying) {
        String key = key(player, item);
        long now = System.currentTimeMillis();
        long last = cooldownTracker.getOrDefault(key, 0L);
        if (now - last < plugin.getConfigManager().getCooldownMillis()) {
            return TransactionResult.failure("invalid-action");
        }
        cooldownTracker.put(key, now);

        int limit = plugin.getConfigManager().getDailyLimit(String.valueOf(item.getId()));
        if (limit <= 0) {
            return null;
        }

        String dayKey = LocalDate.now() + ":" + key + ":" + (buying ? "buy" : "sell");
        int used = dailyTracker.getOrDefault(dayKey, 0);
        if (used + amount > limit) {
            return TransactionResult.failure("invalid-action");
        }
        dailyTracker.put(dayKey, used + amount);
        return null;
    }

    private void recordDemand(ShopItem item, int amount) {
        PriceState state = dynamicState.computeIfAbsent(item.getId(), id -> new PriceState());
        state.bought += amount;
    }

    private void recordSupply(ShopItem item, int amount) {
        PriceState state = dynamicState.computeIfAbsent(item.getId(), id -> new PriceState());
        state.sold += amount;
    }

    private double applyDynamic(ShopItem item, double basePrice) {
        if (!plugin.getConfigManager().isDynamicPricingEnabled() || basePrice <= 0.0D) {
            return basePrice;
        }
        PriceState state = dynamicState.computeIfAbsent(item.getId(), id -> new PriceState());
        double delta = (state.bought - state.sold) * plugin.getConfigManager().getDynamicStep() * 0.01D;
        double multiplier = Math.max(plugin.getConfigManager().getDynamicMinMultiplier(),
                Math.min(plugin.getConfigManager().getDynamicMaxMultiplier(), 1.0D + delta));
        return basePrice * multiplier;
    }

    private void logTransaction(String type, Player player, ShopItem item, int amount, double price) {
        if (plugin.getConfigManager().logTransactions()) {
            plugin.getLogger().info("[ShopTx] " + type + " player=" + player.getName() + " item=" + item.getId()
                    + " amount=" + amount + " total=" + PRICE_FORMAT.format(price));
        }
    }

    private void debugFailure(String action, Player player, ShopItem item, String reason) {
        if (plugin.getConfigManager().logFailures()) {
            plugin.getLogger().info("[ShopFail] action=" + action + " player=" + player.getName() + " item=" + item.getId() + " reason=" + reason);
        }
    }

    private static final class PriceState {
        int bought;
        int sold;
    }
}