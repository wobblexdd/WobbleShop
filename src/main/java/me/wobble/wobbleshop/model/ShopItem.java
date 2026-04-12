package me.wobble.wobbleshop.model;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;

public final class ShopItem {

    private int id;
    private Material material;
    private String displayName;
    private String category;
    private int slot;
    private double buyPrice;
    private double sellPrice;
    private boolean buyEnabled;
    private boolean sellEnabled;
    private StockType stockType;
    private int stock;
    private int maxStock;
    private boolean restockEnabled;
    private long restockInterval;
    private int restockAmount;
    private long lastRestock;
    private ShopStatus status;
    private List<String> lore;

    public ShopItem(int id, Material material, String displayName, String category, int slot, double buyPrice,
                    double sellPrice, boolean buyEnabled, boolean sellEnabled, StockType stockType, int stock,
                    int maxStock, boolean restockEnabled, long restockInterval, int restockAmount, long lastRestock,
                    ShopStatus status, List<String> lore) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.category = category;
        this.slot = slot;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.buyEnabled = buyEnabled;
        this.sellEnabled = sellEnabled;
        this.stockType = stockType;
        this.stock = stock;
        this.maxStock = maxStock;
        this.restockEnabled = restockEnabled;
        this.restockInterval = restockInterval;
        this.restockAmount = restockAmount;
        this.lastRestock = lastRestock;
        this.status = status;
        this.lore = new ArrayList<>(lore);
    }

    public static ShopItem createDefault(String category, int slot, Material material, String displayName) {
        long now = System.currentTimeMillis();
        return new ShopItem(
                0,
                material,
                displayName,
                category,
                slot,
                20.0D,
                10.0D,
                false,
                true,
                StockType.INFINITE,
                0,
                256,
                true,
                3600L,
                64,
                now,
                ShopStatus.ACTIVE,
                List.of("&7Freshly added to the shop.", "&7Configure this item in the admin GUI.")
        );
    }

    public boolean canBuy() {
        return buyEnabled && buyPrice > 0.0D && getResolvedStatus() == ShopStatus.ACTIVE && hasStockForBuy(1);
    }

    public boolean canSell() {
        return sellEnabled && sellPrice > 0.0D && status != ShopStatus.DISABLED && status != ShopStatus.COMING_SOON;
    }

    public boolean hasStockForBuy(int amount) {
        if (amount <= 0) {
            return false;
        }
        return isStaticStock() || stock >= amount;
    }

    public boolean canManualRestock() {
        return isLimitedStock() && stock < maxStock;
    }

    public boolean canAutoRestock(long now) {
        if (!isLimitedStock() || !restockEnabled || restockInterval <= 0L || stock >= maxStock) {
            return false;
        }
        return now - lastRestock >= restockInterval * 1000L;
    }

    public void reduceStock(int amount) {
        if (isLimitedStock()) {
            stock = Math.max(0, stock - amount);
        }
    }

    public void restockToMax() {
        if (isLimitedStock()) {
            stock = maxStock;
            lastRestock = System.currentTimeMillis();
        }
    }

    public void restockIncrement() {
        if (isLimitedStock()) {
            stock = Math.min(maxStock, stock + Math.max(1, restockAmount));
            lastRestock = System.currentTimeMillis();
        }
    }

    public ShopStatus getResolvedStatus() {
        if (status == ShopStatus.ACTIVE && isLimitedStock() && stock <= 0) {
            return ShopStatus.OUT_OF_STOCK;
        }
        return status;
    }

    public boolean isStaticStock() {
        return stockType == StockType.INFINITE;
    }

    public boolean isLimitedStock() {
        return stockType == StockType.LIMITED || stockType == StockType.REGENERATING;
    }

    public boolean isRegeneratingStock() {
        return stockType == StockType.REGENERATING;
    }

    public int getRemainingCapacity() {
        return Math.max(0, maxStock - stock);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public boolean isBuyEnabled() {
        return buyEnabled;
    }

    public void setBuyEnabled(boolean buyEnabled) {
        this.buyEnabled = buyEnabled;
    }

    public boolean isSellEnabled() {
        return sellEnabled;
    }

    public void setSellEnabled(boolean sellEnabled) {
        this.sellEnabled = sellEnabled;
    }

    public StockType getStockType() {
        return stockType;
    }

    public void setStockType(StockType stockType) {
        this.stockType = stockType;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getMaxStock() {
        return maxStock;
    }

    public void setMaxStock(int maxStock) {
        this.maxStock = maxStock;
    }

    public boolean isRestockEnabled() {
        return restockEnabled;
    }

    public void setRestockEnabled(boolean restockEnabled) {
        this.restockEnabled = restockEnabled;
    }

    public long getRestockInterval() {
        return restockInterval;
    }

    public void setRestockInterval(long restockInterval) {
        this.restockInterval = restockInterval;
    }

    public int getRestockAmount() {
        return restockAmount;
    }

    public void setRestockAmount(int restockAmount) {
        this.restockAmount = restockAmount;
    }

    public long getLastRestock() {
        return lastRestock;
    }

    public void setLastRestock(long lastRestock) {
        this.lastRestock = lastRestock;
    }

    public ShopStatus getStatus() {
        return status;
    }

    public void setStatus(ShopStatus status) {
        this.status = status;
    }

    public List<String> getLore() {
        return new ArrayList<>(lore);
    }

    public void setLore(List<String> lore) {
        this.lore = new ArrayList<>(lore);
    }
}
