package me.wobble.wobbleshop.gui;

import java.util.List;
import me.wobble.wobbleshop.WobbleShopPlugin;
import me.wobble.wobbleshop.manager.GuiManager;
import me.wobble.wobbleshop.model.ShopCategory;
import me.wobble.wobbleshop.model.ShopItem;
import me.wobble.wobbleshop.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class AdminItemListGui extends BaseGui {

    private final ShopCategory category;

    public AdminItemListGui(WobbleShopPlugin plugin, GuiManager guiManager, Player player, ShopCategory category) {
        super(plugin, guiManager, player, 54, plugin.getConfigManager()
                .getTitle("shop.admin-list-title", "&cEdit: {category}")
                .replace("{category}", category.getDisplayName()));
        this.category = category;
    }

    @Override
    protected void render() {
        List<ShopItem> items = shopService.getItemsByCategory(category.getKey());
        if (items.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                    .name("&cNo Items In Category")
                    .lore(List.of(
                            "&7This category does not have any shop items yet.",
                            "&7Use the create button below to add one."
                    ))
                    .build());
        }

        for (ShopItem item : items) {
            inventory.setItem(item.getSlot(), new ItemBuilder(item.getMaterial())
                    .name(item.getDisplayName())
                    .lore(List.of(
                            "&7ID: &f" + item.getId(),
                            "&7Category: &f" + item.getCategory(),
                            "&7Status: " + shopService.getDisplayStatusLabel(item),
                            "&7Stock Type: " + shopService.getStockTypeDisplay(item),
                            "&7Stock: " + shopService.getStockDetailDisplay(item),
                            "&7Sell: " + shopService.getSellDisplay(item),
                            "&7Buy: " + shopService.getBuyDisplay(item),
                            "&eClick to edit"
                    ))
                    .hideAttributes()
                    .build());
        }

        inventory.setItem(45, new ItemBuilder(Material.LIME_DYE)
                .name("&aCreate Item")
                .lore(List.of("&7Create a new item in this category."))
                .build());
        inventory.setItem(46, new ItemBuilder(Material.CLOCK)
                .name("&6Restock Category")
                .lore(List.of("&7Refill all limited items in this category.", "&eClick to restock"))
                .build());
        inventory.setItem(49, new ItemBuilder(Material.ARROW)
                .name("&eBack")
                .lore(List.of("&7Return to the admin main menu."))
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
            ShopItem item = shopService.createItem(category.getKey());
            messageManager.send(player, "admin-item-created", java.util.Map.of("category", category.getDisplayName()));
            guiManager.openAdminItemEdit(player, item);
            return;
        }
        if (slot == 46) {
            int count = shopService.restockCategory(category.getKey());
            if (count <= 0) {
                messageManager.send(player, "restock-none");
            } else {
                messageManager.send(player, "restocked-category", java.util.Map.of(
                        "count", String.valueOf(count),
                        "category", category.getDisplayName()
                ));
            }
            redraw();
            return;
        }
        if (slot == 49) {
            guiManager.openAdminMain(player);
            return;
        }
        if (slot == 53) {
            player.closeInventory();
            return;
        }

        for (ShopItem item : shopService.getItemsByCategory(category.getKey())) {
            if (item.getSlot() == slot) {
                guiManager.openAdminItemEdit(player, item);
                return;
            }
        }
    }
}
