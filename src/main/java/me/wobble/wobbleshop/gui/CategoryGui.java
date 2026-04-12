package me.wobble.wobbleshop.gui;

import java.util.ArrayList;
import java.util.List;
import me.wobble.wobbleshop.WobbleShopPlugin;
import me.wobble.wobbleshop.manager.GuiManager;
import me.wobble.wobbleshop.model.ShopCategory;
import me.wobble.wobbleshop.model.ShopItem;
import me.wobble.wobbleshop.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class CategoryGui extends BaseGui {

    private final ShopCategory category;

    public CategoryGui(WobbleShopPlugin plugin, GuiManager guiManager, Player player, ShopCategory category) {
        super(plugin, guiManager, player, 54, plugin.getConfigManager()
                .getTitle("shop.category-title", "&6Category: {category}")
                .replace("{category}", category.getDisplayName()));
        this.category = category;
    }

    @Override
    protected void render() {
        List<ShopItem> items = shopService.getItemsByCategory(category.getKey());
        if (items.isEmpty()) {
            messageManager.send(player, "category-empty");
            inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                    .name("&cNo Items Available")
                    .lore(List.of(
                            "&7There are no items in this category yet.",
                            "&7Check back later."
                    ))
                    .build());
        }

        for (ShopItem item : items) {
            inventory.setItem(item.getSlot(), new ItemBuilder(item.getMaterial())
                    .name(item.getDisplayName())
                    .lore(buildLore(item))
                    .hideAttributes()
                    .build());
        }

        inventory.setItem(45, new ItemBuilder(Material.ARROW)
                .name("&eBack")
                .lore(List.of("&7Return to the category menu."))
                .build());
        inventory.setItem(49, new ItemBuilder(category.getMaterial())
                .name(category.getDisplayName())
                .lore(List.of("&7Left click: buy x1", "&7Right click: buy stack", "&7Shift click: bulk", "&7Middle click: sell all"))
                .build());
        inventory.setItem(53, new ItemBuilder(Material.BARRIER)
                .name("&cClose")
                .lore(List.of("&7Close this menu."))
                .build());

        fill(plugin.getConfigManager().getFillerMaterial());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 45) {
            guiManager.openMain(player);
            return;
        }
        if (slot == 53) {
            player.closeInventory();
            return;
        }

        for (ShopItem item : shopService.getItemsByCategory(category.getKey())) {
            if (item.getSlot() == slot) {
                handleTradeClick(event.getClick(), item);
                redraw();
                return;
            }
        }
    }

    private void handleTradeClick(ClickType clickType, ShopItem item) {
        int bulk = plugin.getConfigManager().getBulkAmount();
        if (clickType == ClickType.LEFT) {
            guiManager.sendResult(player, shopService.buy(player, item, 1));
            return;
        }
        if (clickType == ClickType.RIGHT) {
            guiManager.sendResult(player, shopService.buy(player, item, item.getMaterial().getMaxStackSize()));
            return;
        }
        if (clickType == ClickType.MIDDLE) {
            guiManager.sendResult(player, shopService.sellAll(player, item));
            return;
        }
        if (clickType == ClickType.SHIFT_LEFT) {
            guiManager.sendResult(player, shopService.buy(player, item, bulk));
            return;
        }
        if (clickType == ClickType.SHIFT_RIGHT) {
            guiManager.sendResult(player, shopService.sell(player, item, bulk));
            return;
        }

        guiManager.openItemAction(player, category, item);
    }

    private List<String> buildLore(ShopItem item) {
        List<String> lore = new ArrayList<>(item.getLore());
        lore.add(" ");
        lore.add("&7Buy: " + shopService.getBuyDisplay(item));
        lore.add("&7Sell: " + shopService.getSellDisplay(item));
        lore.add("&7Status: " + shopService.getDisplayStatusLabel(item));
        lore.add("&7Stock Type: " + shopService.getStockTypeDisplay(item));
        lore.add("&7Stock: " + shopService.getStockDetailDisplay(item));
        lore.add(" ");
        lore.add("&eLClick buy 1 &7| &eRClick buy stack");
        lore.add("&eShift-L buy bulk &7| &eShift-R sell bulk");
        lore.add("&eMiddle click sell all");
        return lore;
    }
}
