package me.wobble.wobbleshop.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.wobble.wobbleshop.model.ShopItem;
import me.wobble.wobbleshop.model.ShopStatus;
import me.wobble.wobbleshop.model.StockType;
import me.wobble.wobbleshop.storage.Database;
import org.bukkit.Material;

public final class ShopItemRepository {

    private final Database database;

    public ShopItemRepository(Database database) {
        this.database = database;
    }

    public synchronized List<ShopItem> findAll() {
        List<ShopItem> items = new ArrayList<>();
        String sql = "SELECT * FROM shop_items ORDER BY category_key ASC, slot ASC, id ASC";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                items.add(map(resultSet));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load shop items.", exception);
        }
        return items;
    }

    public synchronized List<ShopItem> findByCategory(String categoryKey) {
        List<ShopItem> items = new ArrayList<>();
        String sql = "SELECT * FROM shop_items WHERE category_key = ? ORDER BY slot ASC, id ASC";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, categoryKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(map(resultSet));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load items for category " + categoryKey, exception);
        }
        return items;
    }

    public synchronized Optional<ShopItem> findById(int id) {
        String sql = "SELECT * FROM shop_items WHERE id = ?";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load item " + id, exception);
        }
        return Optional.empty();
    }

    public synchronized ShopItem save(ShopItem item) {
        if (item.getId() <= 0) {
            return insert(item);
        }
        update(item);
        return item;
    }

    public synchronized void delete(int id) {
        String sql = "DELETE FROM shop_items WHERE id = ?";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not delete item " + id, exception);
        }
    }


    public synchronized void deleteAll() {
        String sql = "DELETE FROM shop_items";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not clear shop items.", exception);
        }
    }

    public synchronized boolean isEmpty() {
        String sql = "SELECT COUNT(*) FROM shop_items";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() && resultSet.getInt(1) == 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not check shop item count.", exception);
        }
    }

    private ShopItem insert(ShopItem item) {
        String sql = """
                INSERT INTO shop_items (
                    material, display_name, category_key, slot, buy_price, sell_price, buy_enabled, sell_enabled,
                    stock_type, stock, max_stock, restock_enabled, restock_interval, restock_amount, last_restock, status, lore
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = database.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, item);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    item.setId(keys.getInt(1));
                }
            }
            return item;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not insert shop item.", exception);
        }
    }

    private void update(ShopItem item) {
        String sql = """
                UPDATE shop_items SET
                    material = ?, display_name = ?, category_key = ?, slot = ?, buy_price = ?, sell_price = ?,
                    buy_enabled = ?, sell_enabled = ?, stock_type = ?, stock = ?, max_stock = ?,
                    restock_enabled = ?, restock_interval = ?, restock_amount = ?, last_restock = ?, status = ?, lore = ?
                WHERE id = ?
                """;
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            bind(statement, item);
            statement.setInt(18, item.getId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not update shop item " + item.getId(), exception);
        }
    }

    private void bind(PreparedStatement statement, ShopItem item) throws SQLException {
        statement.setString(1, item.getMaterial().name());
        statement.setString(2, item.getDisplayName());
        statement.setString(3, item.getCategory());
        statement.setInt(4, item.getSlot());
        statement.setDouble(5, item.getBuyPrice());
        statement.setDouble(6, item.getSellPrice());
        statement.setInt(7, item.isBuyEnabled() ? 1 : 0);
        statement.setInt(8, item.isSellEnabled() ? 1 : 0);
        statement.setString(9, item.getStockType().name());
        statement.setInt(10, item.getStock());
        statement.setInt(11, item.getMaxStock());
        statement.setInt(12, item.isRestockEnabled() ? 1 : 0);
        statement.setLong(13, item.getRestockInterval());
        statement.setInt(14, item.getRestockAmount());
        statement.setLong(15, item.getLastRestock());
        statement.setString(16, item.getStatus().name());
        statement.setString(17, String.join("\n", item.getLore()));
    }

    private ShopItem map(ResultSet resultSet) throws SQLException {
        Material material = Material.matchMaterial(resultSet.getString("material"));
        if (material == null) {
            material = Material.STONE;
        }
        String loreData = resultSet.getString("lore");
        List<String> lore = loreData.isBlank() ? new ArrayList<>() : List.of(loreData.split("\n"));
        return new ShopItem(
                resultSet.getInt("id"),
                material,
                resultSet.getString("display_name"),
                resultSet.getString("category_key"),
                resultSet.getInt("slot"),
                resultSet.getDouble("buy_price"),
                resultSet.getDouble("sell_price"),
                resultSet.getInt("buy_enabled") == 1,
                resultSet.getInt("sell_enabled") == 1,
                StockType.fromStorage(resultSet.getString("stock_type")),
                resultSet.getInt("stock"),
                resultSet.getInt("max_stock"),
                resultSet.getInt("restock_enabled") == 1,
                resultSet.getLong("restock_interval"),
                resultSet.getInt("restock_amount"),
                resultSet.getLong("last_restock"),
                ShopStatus.valueOf(resultSet.getString("status")),
                lore
        );
    }
}
