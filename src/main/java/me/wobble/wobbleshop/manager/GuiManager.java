package me.wobble.wobbleshop.manager;

import me.wobble.wobbleshop.WobbleShopPlugin;
import me.wobble.wobbleshop.config.MessageManager;
import me.wobble.wobbleshop.gui.AdminItemEditGui;
import me.wobble.wobbleshop.gui.AdminItemListGui;
import me.wobble.wobbleshop.gui.AdminMainGui;
import me.wobble.wobbleshop.gui.BaseGui;
import me.wobble.wobbleshop.gui.CategoryGui;
import me.wobble.wobbleshop.gui.ConfirmGui;
import me.wobble.wobbleshop.gui.ItemActionGui;
import me.wobble.wobbleshop.gui.MainShopGui;
import me.wobble.wobbleshop.model.ShopCategory;
import me.wobble.wobbleshop.model.ShopItem;
import me.wobble.wobbleshop.model.TransactionResult;
import me.wobble.wobbleshop.service.ShopService;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class GuiManager {

    private final WobbleShopPlugin plugin;
    private final ShopService shopService;
    private final MessageManager messageManager;

    public GuiManager(WobbleShopPlugin plugin, ShopService shopService, MessageManager messageManager) {
        this.plugin = plugin;
        this.shopService = shopService;
        this.messageManager = messageManager;
    }

    public void openMain(Player player) {
        open(new MainShopGui(plugin, this, player));
    }

    public void openCategory(Player player, ShopCategory category) {
        open(new CategoryGui(plugin, this, player, category));
    }

    public void openItemAction(Player player, ShopCategory category, ShopItem item) {
        open(new ItemActionGui(plugin, this, player, category, item));
    }

    public void openAdminMain(Player player) {
        open(new AdminMainGui(plugin, this, player));
    }

    public void openAdminItemList(Player player, ShopCategory category) {
        open(new AdminItemListGui(plugin, this, player, category));
    }

    public void openAdminItemEdit(Player player, ShopItem item) {
        open(new AdminItemEditGui(plugin, this, player, item));
    }

    public void openConfirm(Player player, String prompt, Runnable confirmAction, Runnable cancelAction) {
        open(new ConfirmGui(plugin, this, player, prompt, confirmAction, cancelAction));
    }

    public void sendResult(Player player, TransactionResult result) {
        if (result.placeholders().isEmpty()) {
            messageManager.send(player, result.messageKey());
        } else {
            messageManager.send(player, result.messageKey(), result.placeholders());
        }
        playResultSound(player, result.success());
    }

    public void open(BaseGui gui) {
        plugin.getServer().getScheduler().runTask(plugin, gui::open);
    }

    public ShopService getShopService() {
        return shopService;
    }

    private void playResultSound(Player player, boolean success) {
        if (!plugin.getConfigManager().areResultSoundsEnabled()) {
            return;
        }

        Sound sound = success ? plugin.getConfigManager().getSuccessSound() : plugin.getConfigManager().getFailureSound();
        player.playSound(player.getLocation(), sound,
                plugin.getConfigManager().getResultSoundVolume(),
                plugin.getConfigManager().getResultSoundPitch());
    }
}
