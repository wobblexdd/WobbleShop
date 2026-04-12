package me.wobble.wobbleshop.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import me.wobble.wobbleshop.WobbleShopPlugin;
import me.wobble.wobbleshop.config.ConfigManager;

public final class SQLiteDatabase implements Database {

    private final WobbleShopPlugin plugin;
    private final ConfigManager configManager;
    private Connection connection;

    public SQLiteDatabase(WobbleShopPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public void initialize() {
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                throw new IllegalStateException("Could not create plugin data folder.");
            }

            File databaseFile = new File(plugin.getDataFolder(), configManager.getDatabaseFile());
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS categories (
                            key TEXT PRIMARY KEY,
                            display_name TEXT NOT NULL,
                            material TEXT NOT NULL,
                            slot INTEGER NOT NULL,
                            enabled INTEGER NOT NULL,
                            permission TEXT NOT NULL DEFAULT '',
                            admin_only INTEGER NOT NULL DEFAULT 0
                        )
                        """);
                statement.executeUpdate("ALTER TABLE categories ADD COLUMN permission TEXT NOT NULL DEFAULT ''");
            } catch (SQLException ignored) {
            }
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE categories ADD COLUMN admin_only INTEGER NOT NULL DEFAULT 0");
            } catch (SQLException ignored) {
            }
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS shop_items (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            material TEXT NOT NULL,
                            display_name TEXT NOT NULL,
                            category_key TEXT NOT NULL,
                            slot INTEGER NOT NULL,
                            buy_price REAL NOT NULL,
                            sell_price REAL NOT NULL,
                            buy_enabled INTEGER NOT NULL,
                            sell_enabled INTEGER NOT NULL,
                            stock_type TEXT NOT NULL,
                            stock INTEGER NOT NULL,
                            max_stock INTEGER NOT NULL,
                            restock_enabled INTEGER NOT NULL,
                            restock_interval INTEGER NOT NULL,
                            restock_amount INTEGER NOT NULL DEFAULT 64,
                            last_restock INTEGER NOT NULL,
                            status TEXT NOT NULL,
                            lore TEXT NOT NULL
                        )
                        """);
            }
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE shop_items ADD COLUMN restock_amount INTEGER NOT NULL DEFAULT 64");
            } catch (SQLException ignored) {
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not initialize SQLite database.", exception);
        }
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public void close() {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Failed to close SQLite connection: " + exception.getMessage());
        }
    }
}
