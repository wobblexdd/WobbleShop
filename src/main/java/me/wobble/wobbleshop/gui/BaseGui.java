package me.wobble.wobbleshop.gui;

import me.wobble.wobbleshop.WobbleShopPlugin;
import me.wobble.wobbleshop.config.MessageManager;
import me.wobble.wobbleshop.manager.GuiManager;
import me.wobble.wobbleshop.service.ShopService;
import me.wobble.wobbleshop.util.ColorUtil;
import me.wobble.wobbleshop.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public abstract class BaseGui implements InventoryHolder {

    protected final WobbleShopPlugin plugin;
    protected final GuiManager guiManager;
    protected final ShopService shopService;
    protected final MessageManager messageManager;
    protected final Player player;
    private final int size;
    private final String title;
    protected Inventory inventory;

    protected BaseGui(WobbleShopPlugin plugin, GuiManager guiManager, Player player, int size, String title) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.shopService = plugin.getShopService();
        this.messageManager = plugin.getMessageManager();
        this.player = player;
        this.size = size;
        this.title = title;
    }

    public void open() {
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, size, ColorUtil.colorize(title));
        }
        redraw();
        player.openInventory(inventory);
    }

    public void redraw() {
        inventory.clear();
        render();
    }

    protected abstract void render();

    public abstract void handleClick(InventoryClickEvent event);

    public void handleClose(InventoryCloseEvent event) {
    }

    protected void fill(Material material) {
        ItemStack filler = new ItemBuilder(material).name(" ").build();
        for (int index = 0; index < inventory.getSize(); index++) {
            if (inventory.getItem(index) == null) {
                inventory.setItem(index, filler);
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
