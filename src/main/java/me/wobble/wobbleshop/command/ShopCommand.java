package me.wobble.wobbleshop.command;

import me.wobble.wobbleshop.WobbleShopPlugin;
import me.wobble.wobbleshop.model.ShopCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ShopCommand implements CommandExecutor {

    private final WobbleShopPlugin plugin;

    public ShopCommand(WobbleShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                plugin.getMessageManager().send(sender, "player-only");
                return true;
            }
            if (!player.hasPermission("wobble.shop.use")) {
                plugin.getMessageManager().send(player, "no-permission");
                return true;
            }
            plugin.getMessageManager().send(player, "shop-opened");
            plugin.getGuiManager().openMain(player);
            return true;
        }

        String subcommand = args[0].toLowerCase();
        switch (subcommand) {
            case "admin" -> {
                if (!(sender instanceof Player player)) {
                    plugin.getMessageManager().send(sender, "player-only");
                    return true;
                }
                if (!player.hasPermission("wobble.shop.admin")) {
                    plugin.getMessageManager().send(player, "no-permission");
                    return true;
                }
                plugin.getGuiManager().openAdminMain(player);
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("wobble.shop.reload")) {
                    plugin.getMessageManager().send(sender, "no-permission");
                    return true;
                }
                plugin.reloadPlugin();
                plugin.getMessageManager().send(sender, "reloaded");
                return true;
            }
            case "restock" -> {
                if (!sender.hasPermission("wobble.shop.restock")) {
                    plugin.getMessageManager().send(sender, "no-permission");
                    return true;
                }
                if (args.length == 1) {
                    int count = plugin.getShopService().restockAll();
                    sendRestockResult(sender, count, "restocked-all", java.util.Map.of("count", String.valueOf(count)));
                    return true;
                }

                String target = args[1];
                ShopCategory category = plugin.getShopService().getCategory(target).orElse(null);
                if (category != null) {
                    int count = plugin.getShopService().restockCategory(category.getKey());
                    sendRestockResult(sender, count, "restocked-category", java.util.Map.of(
                            "count", String.valueOf(count),
                            "category", category.getDisplayName()
                    ));
                    return true;
                }

                try {
                    int itemId = Integer.parseInt(target);
                    boolean restocked = plugin.getShopService().restockItem(itemId);
                    if (restocked) {
                        plugin.getMessageManager().send(sender, "restocked-item", java.util.Map.of("id", String.valueOf(itemId)));
                    } else {
                        plugin.getMessageManager().send(sender, "restock-none");
                    }
                    return true;
                } catch (NumberFormatException ignored) {
                    plugin.getMessageManager().send(sender, "restock-target-missing", java.util.Map.of("target", target));
                    return true;
                }
            }
            default -> {
                if (sender instanceof Player player && player.hasPermission("wobble.shop.use")) {
                    plugin.getMessageManager().send(player, "shop-opened");
                    plugin.getGuiManager().openMain(player);
                    return true;
                }
                plugin.getMessageManager().send(sender, "no-permission");
                return true;
            }
        }
    }

    private void sendRestockResult(CommandSender sender, int count, String successKey, java.util.Map<String, String> placeholders) {
        if (count <= 0) {
            plugin.getMessageManager().send(sender, "restock-none");
            return;
        }
        plugin.getMessageManager().send(sender, successKey, placeholders);
    }
}
