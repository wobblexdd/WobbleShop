package me.wobble.wobbleshop.gui;

import java.util.List;
import me.wobble.wobbleshop.WobbleShopPlugin;
import me.wobble.wobbleshop.manager.GuiManager;
import me.wobble.wobbleshop.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class ConfirmGui extends BaseGui {

    private final String prompt;
    private final Runnable confirmAction;
    private final Runnable cancelAction;

    public ConfirmGui(WobbleShopPlugin plugin, GuiManager guiManager, Player player, String prompt,
                      Runnable confirmAction, Runnable cancelAction) {
        super(plugin, guiManager, player, 27, plugin.getConfigManager().getTitle("shop.confirm-title", "&4Confirm Action"));
        this.prompt = prompt;
        this.confirmAction = confirmAction;
        this.cancelAction = cancelAction;
    }

    @Override
    protected void render() {
        inventory.setItem(11, new ItemBuilder(Material.LIME_CONCRETE)
                .name("&aConfirm")
                .lore(List.of(prompt, "&7Proceed with this action."))
                .build());
        inventory.setItem(15, new ItemBuilder(Material.RED_CONCRETE)
                .name("&cCancel")
                .lore(List.of(prompt, "&7Return without making changes."))
                .build());
        fill(plugin.getConfigManager().getFillerMaterial());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getRawSlot() == 11) {
            confirmAction.run();
            return;
        }
        if (event.getRawSlot() == 15) {
            cancelAction.run();
        }
    }
}
