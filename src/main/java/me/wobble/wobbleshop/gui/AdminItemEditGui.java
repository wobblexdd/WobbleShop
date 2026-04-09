package me.wobble.wobbleshop.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import me.wobble.wobbleshop.WobbleShopPlugin;
import me.wobble.wobbleshop.manager.GuiManager;
import me.wobble.wobbleshop.model.ShopCategory;
import me.wobble.wobbleshop.model.ShopItem;
import me.wobble.wobbleshop.model.ShopStatus;
import me.wobble.wobbleshop.model.StockType;
import me.wobble.wobbleshop.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public final class AdminItemEditGui extends BaseGui {

    private final int itemId;

    public AdminItemEditGui(WobbleShopPlugin plugin, GuiManager guiManager, Player player, ShopItem item) {
        super(plugin, guiManager, player, 54, plugin.getConfigManager()
                .getTitle("shop.admin-edit-title", "&cEdit Item #{id}")
                .replace("{id}", String.valueOf(item.getId())));
        this.itemId = item.getId();
    }

    @Override
    protected void render() {
        ShopItem item = currentItem();
        if (item == null) {
            guiManager.openAdminMain(player);
            return;
        }

        inventory.setItem(10, new ItemBuilder(item.getMaterial())
                .name(item.getDisplayName())
                .lore(previewLore(item))
                .hideAttributes()
                .build());
        inventory.setItem(11, button(Material.ITEM_FRAME, "&eMaterial", List.of(
                "&7Current: &f" + shopService.prettifyMaterial(item.getMaterial()),
                "&7Click: &fCycle material"
        )));
        inventory.setItem(12, button(Material.BOOK, "&eCategory", List.of(
                "&7Current: &f" + item.getCategory(),
                "&7Click: &fCycle category"
        )));
        inventory.setItem(13, button(Material.REDSTONE, "&eStatus", List.of(
                "&7Current: " + shopService.getStatusLabel(item.getStatus()),
                "&7Click: &fCycle status"
        )));
        inventory.setItem(14, button(Material.EMERALD, "&eBuy Toggle", List.of(
                "&7Current: " + (item.isBuyEnabled() ? "&aEnabled" : "&cDisabled"),
                "&7Click: &fToggle buying"
        )));
        inventory.setItem(15, button(Material.HOPPER, "&eSell Toggle", List.of(
                "&7Current: " + (item.isSellEnabled() ? "&aEnabled" : "&cDisabled"),
                "&7Click: &fToggle selling"
        )));
        inventory.setItem(16, button(Material.CHEST, "&eStock Type", List.of(
                "&7Current: " + shopService.getStockTypeDisplay(item),
                "&7Click: &fToggle static/limited"
        )));
        inventory.setItem(19, numberButton(Material.GOLD_INGOT, "&6Buy Price", item.getBuyPrice(), "&7Left: -1  &7Right: +1  &7Shift: +/-10"));
        inventory.setItem(20, numberButton(Material.IRON_INGOT, "&6Sell Price", item.getSellPrice(), "&7Left: -1  &7Right: +1  &7Shift: +/-10"));
        inventory.setItem(21, wholeNumberButton(Material.CHEST_MINECART, "&6Stock", item.getStock(), "&7Used only for limited stock. Left: -1  Right: +1  Shift: +/-16"));
        inventory.setItem(22, wholeNumberButton(Material.BARREL, "&6Max Stock", item.getMaxStock(), "&7Left: -16  &7Right: +16"));
        inventory.setItem(23, button(Material.CLOCK, "&6Restock Enabled", List.of(
                "&7Current: " + (item.isRestockEnabled() ? "&aEnabled" : "&cDisabled"),
                "&7Automatic restock only affects limited items.",
                "&7Click: &fToggle automatic restock"
        )));
        inventory.setItem(24, wholeNumberButton(Material.REPEATER, "&6Restock Interval", item.getRestockInterval(),
                "&7Seconds. Left/Right: -/+300  Shift: -/+3600"));
        inventory.setItem(25, wholeNumberButton(Material.COMPARATOR, "&6Menu Slot", item.getSlot(), "&7Left/Right: -/+1  Shift: -/+9"));
        inventory.setItem(30, button(Material.HOPPER_MINECART, "&6Restock Now", List.of(
                "&7Type: " + shopService.getStockTypeDisplay(item),
                "&7Current stock: " + shopService.getStockDetailDisplay(item),
                "&7Click: &fRefill this item to max"
        )));
        inventory.setItem(28, button(Material.NAME_TAG, "&eSync Display Name", List.of(
                "&7Set the display name to the selected material name.",
                "&7Click: &fApply automatic name"
        )));
        inventory.setItem(29, button(Material.WRITABLE_BOOK, "&eCycle Lore Style", List.of(
                "&7Rotate between clean preset descriptions.",
                "&7Click: &fChange description"
        )));
        inventory.setItem(31, button(Material.SLIME_BALL, "&aQuick Activate", List.of(
                "&7Set status active and enable selling.",
                "&7Click: &fApply live preset"
        )));
        inventory.setItem(40, button(Material.LAVA_BUCKET, "&cDelete Item", List.of(
                "&7Remove this item from the shop.",
                "&eClick to confirm"
        )));
        inventory.setItem(49, button(Material.ARROW, "&eBack", List.of(
                "&7Return to the category item list."
        )));

        fill(plugin.getConfigManager().getFillerMaterial());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        ShopItem item = currentItem();
        if (item == null) {
            guiManager.openAdminMain(player);
            return;
        }

        int slot = event.getRawSlot();
        ClickType click = event.getClick();
        switch (slot) {
            case 11 -> mutate(item, current -> {
                List<Material> cycle = shopService.getCategoryMaterialCycle(current.getCategory());
                int index = cycle.indexOf(current.getMaterial());
                int nextIndex = (index + 1) % cycle.size();
                current.setMaterial(cycle.get(nextIndex));
            });
            case 12 -> mutate(item, current -> {
                List<String> keys = shopService.getCategoryKeys();
                int index = keys.indexOf(current.getCategory());
                int nextIndex = (index + 1) % keys.size();
                current.setCategory(keys.get(nextIndex));
                current.setMaterial(shopService.getCategoryMaterialCycle(current.getCategory()).get(0));
            });
            case 13 -> mutate(item, current -> current.setStatus(current.getStatus().next()));
            case 14 -> mutate(item, current -> current.setBuyEnabled(!current.isBuyEnabled()));
            case 15 -> mutate(item, current -> current.setSellEnabled(!current.isSellEnabled()));
            case 16 -> mutate(item, current -> current.setStockType(current.getStockType().next()));
            case 19 -> mutate(item, current -> current.setBuyPrice(current.getBuyPrice() + priceDelta(click)));
            case 20 -> mutate(item, current -> current.setSellPrice(current.getSellPrice() + priceDelta(click)));
            case 21 -> mutate(item, current -> current.setStock(current.getStock() + stockDelta(click)));
            case 22 -> mutate(item, current -> current.setMaxStock(current.getMaxStock() + maxStockDelta(click)));
            case 23 -> mutate(item, current -> current.setRestockEnabled(!current.isRestockEnabled()));
            case 24 -> mutate(item, current -> current.setRestockInterval(current.getRestockInterval() + intervalDelta(click)));
            case 25 -> mutate(item, current -> current.setSlot(current.getSlot() + slotDelta(click)));
            case 28 -> mutate(item, current -> current.setDisplayName("&f" + shopService.prettifyMaterial(current.getMaterial())));
            case 29 -> mutate(item, current -> current.setLore(nextLore(current)));
            case 30 -> {
                if (item.isStaticStock()) {
                    messageManager.send(player, "invalid-action");
                    redraw();
                    return;
                }
                mutate(item, ShopItem::restockToMax);
            }
            case 31 -> mutate(item, current -> {
                current.setStatus(ShopStatus.ACTIVE);
                current.setSellEnabled(true);
                if (current.getStockType() == StockType.LIMITED && current.getStock() <= 0) {
                    current.setStock(current.getMaxStock());
                }
            });
            case 40 -> guiManager.openConfirm(player, "&7Delete this item permanently?", () -> {
                String categoryKey = item.getCategory();
                shopService.deleteItem(item.getId());
                messageManager.send(player, "admin-item-deleted");
                ShopCategory category = shopService.getCategory(categoryKey).orElse(null);
                if (category == null) {
                    guiManager.openAdminMain(player);
                } else {
                    guiManager.openAdminItemList(player, category);
                }
            }, () -> guiManager.openAdminItemEdit(player, item));
            case 49 -> {
                ShopCategory category = shopService.getCategory(item.getCategory()).orElse(null);
                if (category == null) {
                    guiManager.openAdminMain(player);
                } else {
                    guiManager.openAdminItemList(player, category);
                }
            }
            default -> {
            }
        }
    }

    private ShopItem currentItem() {
        return shopService.getItem(itemId).orElse(null);
    }

    private void mutate(ShopItem item, Consumer<ShopItem> consumer) {
        consumer.accept(item);
        shopService.saveItem(item);
        messageManager.send(player, "admin-item-saved");
        redraw();
    }

    private ItemStack button(Material material, String name, List<String> lore) {
        return new ItemBuilder(material).name(name).lore(lore).build();
    }

    private ItemStack numberButton(Material material, String name, double value, String help) {
        return new ItemBuilder(material).name(name).lore(List.of(
                "&7Current: &f" + String.format("%.2f", value),
                help
        )).build();
    }

    private ItemStack wholeNumberButton(Material material, String name, long value, String help) {
        return new ItemBuilder(material).name(name).lore(List.of(
                "&7Current: &f" + value,
                help
        )).build();
    }

    private List<String> previewLore(ShopItem item) {
        List<String> lore = new ArrayList<>(item.getLore());
        lore.add(" ");
        lore.add("&7ID: &f" + item.getId());
        lore.add("&7Category: &f" + item.getCategory());
        lore.add("&7Status: " + shopService.getDisplayStatusLabel(item));
        lore.add("&7Stock Type: " + shopService.getStockTypeDisplay(item));
        lore.add("&7Stock: " + shopService.getStockDetailDisplay(item));
        return lore;
    }

    private double priceDelta(ClickType click) {
        double amount = click.isShiftClick() ? 10.0D : 1.0D;
        return click.isRightClick() ? amount : -amount;
    }

    private int stockDelta(ClickType click) {
        int amount = click.isShiftClick() ? 16 : 1;
        return click.isRightClick() ? amount : -amount;
    }

    private int maxStockDelta(ClickType click) {
        return click.isRightClick() ? 16 : -16;
    }

    private long intervalDelta(ClickType click) {
        long amount = click.isShiftClick() ? 3600L : 300L;
        return click.isRightClick() ? amount : -amount;
    }

    private int slotDelta(ClickType click) {
        int amount = click.isShiftClick() ? 9 : 1;
        return click.isRightClick() ? amount : -amount;
    }

    private List<String> nextLore(ShopItem item) {
        String current = item.getLore().isEmpty() ? "" : item.getLore().get(0);
        if (current.contains("Market staple")) {
            return List.of("&7Crafted for flexible server economies.", "&7Balanced pricing ready for launch.");
        }
        if (current.contains("Crafted for flexible")) {
            return List.of("&7Limited-run stock controlled by admins.", "&7Great for curated progression.");
        }
        return List.of("&7Market staple for active players.", "&7Simple high-volume trade item.");
    }
}
