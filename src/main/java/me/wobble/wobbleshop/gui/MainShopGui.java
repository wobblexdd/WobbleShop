package me.wobble.wobbleshop.gui;

import java.util.List;
import me.wobble.wobbleshop.WobbleShopPlugin;
import me.wobble.wobbleshop.manager.GuiManager;
import me.wobble.wobbleshop.model.ShopCategory;
import me.wobble.wobbleshop.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class MainShopGui extends BaseGui {

    public MainShopGui(WobbleShopPlugin plugin, GuiManager guiManager, Player player) {
        super(plugin, guiManager, player, 27, plugin.getConfigManager().getTitle("shop.main-title", "&6WobbleShop"));
    }

    @Override
    protected void render() {
        List<ShopCategory> categories = shopService.getVisibleCategories(player);
        if (categories.isEmpty()) {
            inventory.setItem(13, new ItemBuilder(Material.BARRIER)
                    .name("&cNo Categories Available")
                    .lore(List.of("&7The shop has no visible categories right now."))
                    .build());
        }

        inventory.setItem(18, new ItemBuilder(Material.BOOK)
                .name("&eShop Help")
                .lore(List.of(
                        "&7Buy items with Vault currency.",
                        "&7Sell items back from the action menu.",
                        "&7Limited stock items restock over time.",
                        "&7Static items never run out."
                ))
                .build());

        for (ShopCategory category : categories) {
            inventory.setItem(category.getSlot(), new ItemBuilder(category.getMaterial())
                    .name(category.getDisplayName())
                    .lore(List.of(
                            "&7Open the " + category.getDisplayName() + "&7 category.",
                            "&7Items: &f" + shopService.getItemsByCategory(category.getKey()).size(),
                            "&7Limited items: &f" + shopService.getItemsByCategory(category.getKey()).stream()
                                    .filter(item -> item.isLimitedStock()).count(),
                            "&eClick to browse"
                    ))
                    .hideAttributes()
                    .build());
        }

        inventory.setItem(26, new ItemBuilder(Material.CHEST_MINECART)
                .name("&6Stock Overview")
                .lore(List.of(
                        "&7Limited listings: &f" + shopService.countLimitedStockItems(),
                        "&7All categories support buy and sell.",
                        "&7Open any category to trade."
                ))
                .build());

        inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                .name("&cClose")
                .lore(List.of("&7Close the shop."))
                .build());

        fill(plugin.getConfigManager().getFillerMaterial());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 22) {
            player.closeInventory();
            return;
        }
        if (slot == 18 || slot == 26) {
            redraw();
            return;
        }

        for (ShopCategory category : shopService.getVisibleCategories(player)) {
            if (category.getSlot() == slot) {
                guiManager.openCategory(player, category);
                return;
            }
        }
    }
}
