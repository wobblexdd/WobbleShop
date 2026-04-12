package me.wobble.wobbleshop.gui;

import java.util.ArrayList;
import java.util.List;
import me.wobble.wobbleshop.WobbleShopPlugin;
import me.wobble.wobbleshop.manager.GuiManager;
import me.wobble.wobbleshop.model.ShopCategory;
import me.wobble.wobbleshop.model.ShopItem;
import me.wobble.wobbleshop.model.ShopStatus;
import me.wobble.wobbleshop.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public final class ItemActionGui extends BaseGui {

    private static final int BUY_ONE_SLOT = 10;
    private static final int BUY_SIXTEEN_SLOT = 11;
    private static final int BUY_SIXTY_FOUR_SLOT = 12;
    private static final int SELL_ONE_SLOT = 28;
    private static final int SELL_SIXTEEN_SLOT = 29;
    private static final int SELL_SIXTY_FOUR_SLOT = 30;
    private static final int SELL_ALL_SLOT = 31;
    private static final int BACK_SLOT = 36;
    private static final int CLOSE_SLOT = 44;

    private final ShopCategory category;
    private final int itemId;

    public ItemActionGui(WobbleShopPlugin plugin, GuiManager guiManager, Player player,
                         ShopCategory category, ShopItem item) {
        super(plugin, guiManager, player, 45, plugin.getConfigManager().getTitle("shop.action-title", "&6Item Actions"));
        this.category = category;
        this.itemId = item.getId();
    }

    @Override
    protected void render() {
        ShopItem item = currentItem();
        if (item == null) {
            guiManager.openCategory(player, category);
            return;
        }

        inventory.setItem(13, new ItemBuilder(item.getMaterial())
                .name(item.getDisplayName())
                .lore(itemLore(item))
                .hideAttributes()
                .build());

        inventory.setItem(BUY_ONE_SLOT, buyButton(item, 1));
        inventory.setItem(BUY_SIXTEEN_SLOT, buyButton(item, 16));
        inventory.setItem(BUY_SIXTY_FOUR_SLOT, buyButton(item, 64));
        inventory.setItem(15, new ItemBuilder(Material.KNOWLEDGE_BOOK)
                .name("&eBuy Status")
                .lore(List.of(
                        "&7Current state: " + shopService.getDisplayStatusLabel(item),
                        "&7Stock type: " + shopService.getStockTypeDisplay(item),
                        "&7Stock: " + shopService.getStockDetailDisplay(item)
                ))
                .build());

        inventory.setItem(24, new ItemBuilder(Material.HOPPER)
                .name("&aSell Options")
                .lore(List.of(
                        "&7Owned: &f" + shopService.countSellableItems(player, item),
                        "&7Sell price: " + shopService.getSellDisplay(item),
                        "&7Selling ignores shop stock in v0.4."
                ))
                .build());
        inventory.setItem(SELL_ONE_SLOT, sellButton(item, 1));
        inventory.setItem(SELL_SIXTEEN_SLOT, sellButton(item, 16));
        inventory.setItem(SELL_SIXTY_FOUR_SLOT, sellButton(item, 64));
        inventory.setItem(SELL_ALL_SLOT, sellAllButton(item));
        inventory.setItem(BACK_SLOT, new ItemBuilder(Material.ARROW)
                .name("&eBack")
                .lore(List.of("&7Return to the category view."))
                .build());
        inventory.setItem(CLOSE_SLOT, new ItemBuilder(Material.BARRIER)
                .name("&cClose")
                .lore(List.of("&7Close this menu."))
                .build());

        fill(plugin.getConfigManager().getFillerMaterial());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        ShopItem item = currentItem();
        if (item == null) {
            guiManager.openCategory(player, category);
            return;
        }

        switch (event.getRawSlot()) {
            case BUY_ONE_SLOT -> handleBuy(item, 1);
            case BUY_SIXTEEN_SLOT -> handleBuy(item, 16);
            case BUY_SIXTY_FOUR_SLOT -> handleBuy(item, 64);
            case SELL_ONE_SLOT -> handleSell(item, 1);
            case SELL_SIXTEEN_SLOT -> handleSell(item, 16);
            case SELL_SIXTY_FOUR_SLOT -> handleSell(item, 64);
            case SELL_ALL_SLOT -> {
                guiManager.sendResult(player, shopService.sellAll(player, item));
                redraw();
            }
            case BACK_SLOT -> guiManager.openCategory(player, category);
            case CLOSE_SLOT -> player.closeInventory();
            default -> {
            }
        }
    }

    private ShopItem currentItem() {
        return shopService.getItem(itemId).orElse(null);
    }

    private void handleBuy(ShopItem item, int amount) {
        guiManager.sendResult(player, shopService.buy(player, item, amount));
        redraw();
    }

    private void handleSell(ShopItem item, int amount) {
        guiManager.sendResult(player, shopService.sell(player, item, amount));
        redraw();
    }

    private ItemStack buyButton(ShopItem item, int amount) {
        boolean available = shopService.isBuyAvailable(item, amount);
        ShopStatus status = shopService.getDisplayStatus(item);
        String name;
        if (available) {
            name = "&aBuy x" + amount;
        } else if (status == ShopStatus.OUT_OF_STOCK) {
            name = "&cOut Of Stock";
        } else if (status == ShopStatus.COMING_SOON) {
            name = "&eComing Soon";
        } else if (status == ShopStatus.DISABLED) {
            name = "&8Disabled";
        } else if (item.isLimitedStock() && !item.hasStockForBuy(amount)) {
            name = "&cInsufficient Stock";
        } else {
            name = "&8Buy Disabled";
        }
        return new ItemBuilder(Material.EMERALD)
                .name(name)
                .lore(List.of(
                        "&7Price: &f" + shopService.formatPrice(shopService.getEffectiveBuyPriceFor(player, item) * amount),
                        "&7Stock type: " + shopService.getStockTypeDisplay(item),
                        "&7Stock: " + shopService.getStockDetailDisplay(item),
                        "&7State: " + shopService.getDisplayStatusLabel(item),
                        available ? "&eClick to buy" : "&7Buying is currently unavailable"
                ))
                .build();
    }

    private ItemStack sellButton(ShopItem item, int amount) {
        boolean available = shopService.isSellAvailable(item);
        return new ItemBuilder(Material.GOLD_INGOT)
                .name(available ? "&aSell x" + amount : "&8Sell Unavailable")
                .lore(List.of(
                        "&7Owned: &f" + shopService.countSellableItems(player, item),
                        "&7Payout: &f" + shopService.formatPrice(shopService.getEffectiveSellPrice(item) * amount),
                        "&7State: " + shopService.getDisplayStatusLabel(item),
                        available ? "&eClick to sell" : "&7Selling is currently unavailable"
                ))
                .build();
    }

    private ItemStack sellAllButton(ShopItem item) {
        int amount = shopService.getSellAllAmount(player, item);
        String payout = amount > 0
                ? shopService.formatPrice(shopService.getEffectiveSellPrice(item) * amount)
                : shopService.formatPrice(0.0D);
        return new ItemBuilder(Material.CHEST)
                .name("&6Sell All")
                .lore(List.of(
                        "&7Owned: &f" + shopService.countSellableItems(player, item),
                        "&7Will sell: &f" + amount,
                        "&7Total payout: &f" + payout,
                        "&eClick to sell everything possible"
                ))
                .build();
    }

    private List<String> itemLore(ShopItem item) {
        List<String> lore = new ArrayList<>(item.getLore());
        lore.add(" ");
        lore.add("&7Category: &f" + category.getDisplayName());
        lore.add("&7Status: " + shopService.getDisplayStatusLabel(item));
        lore.add("&7Buy: " + shopService.getBuyDisplay(item));
        lore.add("&7Sell: " + shopService.getSellDisplay(item));
        lore.add("&7Stock Type: " + shopService.getStockTypeDisplay(item));
        lore.add("&7Stock: " + shopService.getStockDetailDisplay(item));
        lore.add("&7You own: &f" + shopService.countSellableItems(player, item));
        return lore;
    }
}
