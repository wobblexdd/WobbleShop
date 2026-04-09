package me.wobble.wobbleshop.command;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import me.wobble.wobbleshop.WobbleShopPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public final class ShopTabCompleter implements TabCompleter {

    private final WobbleShopPlugin plugin;

    public ShopTabCompleter(WobbleShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String token = args[0].toLowerCase(Locale.ROOT);
            return List.of("admin", "reload", "restock").stream()
                    .filter(value -> value.startsWith(token))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("restock")) {
            String token = args[1].toLowerCase(Locale.ROOT);
            List<String> categoryKeys = plugin.getShopService().getAllCategories().stream()
                    .map(category -> category.getKey().toLowerCase(Locale.ROOT))
                    .toList();
            List<String> itemIds = plugin.getShopService().getAllItems().stream()
                    .map(item -> String.valueOf(item.getId()))
                    .toList();
            return java.util.stream.Stream.concat(categoryKeys.stream(), itemIds.stream())
                    .filter(value -> value.startsWith(token))
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
