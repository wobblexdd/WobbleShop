package me.wobble.wobbleshop.gui;

import java.util.List;
import me.wobble.wobbleshop.WobbleShopPlugin;
import me.wobble.wobbleshop.manager.GuiManager;
import me.wobble.wobbleshop.model.ShopCategory;
import me.wobble.wobbleshop.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class AdminMainGui extends BaseGui {

    public AdminMainGui(WobbleShopPlugin plugin, GuiManager guiManager, Player player) {
        super(plugin, guiManager, player, 27, plugin.getConfigManager().getTitle("shop.admin-main-title", "&cWobbleShop Admin"));
    }

    @Override
    protected void render() {
        for (ShopCategory category : shopService.getAllCategories()) {
            inventory.setItem(category.getSlot(), new ItemBuilder(category.getMaterial())
                    .name("&cManage " + category.getDisplayName())
                    .lore(List.of(
                            "&7Open item list for this category.",
                            "&eClick to manage"
                    ))
                    .hideAttributes()
                    .build());
        }

        inventory.setItem(20, new ItemBuilder(Material.COMPASS)
                .name("&eOpen Player Shop")
                .lore(List.of("&7Switch back to the player-facing menu."))
                .build());
        inventory.setItem(21, new ItemBuilder(Material.REPEATER)
                .name("&bReload Plugin")
                .lore(List.of("&7Reload config, messages, and restock scheduling.", "&eClick to reload"))
                .build());
        inventory.setItem(22, new ItemBuilder(Material.CLOCK)
                .name("&6Restock All")
                .lore(List.of("&7Trigger a full manual restock.", "&eClick to confirm"))
                .build());
        inventory.setItem(18, new ItemBuilder(Material.BOOK)
                .name("&eAdmin Overview")
                .lore(List.of(
                        "&7Categories: &f" + shopService.getAllCategories().size(),
                        "&7Items: &f" + shopService.getAllItems().size(),
                        "&7Limited items: &f" + shopService.countLimitedStockItems()
                ))
                .build());
        inventory.setItem(24, new ItemBuilder(Material.BARRIER)
                .name("&cClose")
                .lore(List.of("&7Close this menu."))
                .build());

        fill(plugin.getConfigManager().getFillerMaterial());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 20) {
            guiManager.openMain(player);
            return;
        }
        if (slot == 21) {
            plugin.reloadPlugin();
            messageManager.send(player, "reloaded");
            guiManager.openAdminMain(player);
            return;
        }
        if (slot == 22) {
            guiManager.openConfirm(player, "&7Run a full manual restock?", () -> {
                int count = shopService.restockAll();
                if (count <= 0) {
                    messageManager.send(player, "restock-none");
                } else {
                    messageManager.send(player, "restocked-all", java.util.Map.of("count", String.valueOf(count)));
                }
                guiManager.openAdminMain(player);
            }, () -> guiManager.openAdminMain(player));
            return;
        }
        if (slot == 24) {
            player.closeInventory();
            return;
        }

        for (ShopCategory category : shopService.getAllCategories()) {
            if (category.getSlot() == slot) {
                guiManager.openAdminItemList(player, category);
                return;
            }
        }
    }
}
