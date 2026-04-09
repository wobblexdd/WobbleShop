package me.wobble.wobbleshop.config;

import java.io.File;
import java.util.Map;
import me.wobble.wobbleshop.WobbleShopPlugin;
import me.wobble.wobbleshop.util.ColorUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public final class MessageManager {

    private final WobbleShopPlugin plugin;
    private FileConfiguration configuration;

    public MessageManager(WobbleShopPlugin plugin) {
        this.plugin = plugin;
        saveDefault();
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        this.configuration = YamlConfiguration.loadConfiguration(file);
    }

    public String getRaw(String path) {
        String value = configuration.getString(path, path);
        return value.replace("{prefix}", configuration.getString("prefix", ""));
    }

    public String get(String path) {
        return ColorUtil.colorize(getRaw(path));
    }

    public String get(String path, Map<String, String> placeholders) {
        String text = getRaw(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return ColorUtil.colorize(text);
    }

    public void send(CommandSender sender, String path) {
        sender.sendMessage(get(path));
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(get(path, placeholders));
    }

    private void saveDefault() {
        File folder = plugin.getDataFolder();
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File file = new File(folder, "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
    }
}
