package me.wobble.wobbleshop.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import me.wobble.wobbleshop.model.ShopCategory;
import me.wobble.wobbleshop.storage.Database;
import org.bukkit.Material;

public final class CategoryRepository {

    private final Database database;

    public CategoryRepository(Database database) {
        this.database = database;
    }

    public synchronized List<ShopCategory> findAll() {
        List<ShopCategory> categories = new ArrayList<>();
        String sql = "SELECT key, display_name, material, slot, enabled FROM categories ORDER BY slot ASC";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                categories.add(map(resultSet));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load categories.", exception);
        }
        return categories;
    }

    public synchronized void save(ShopCategory category) {
        String sql = """
                INSERT INTO categories (key, display_name, material, slot, enabled)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(key) DO UPDATE SET
                    display_name = excluded.display_name,
                    material = excluded.material,
                    slot = excluded.slot,
                    enabled = excluded.enabled
                """;
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, category.getKey());
            statement.setString(2, category.getDisplayName());
            statement.setString(3, category.getMaterial().name());
            statement.setInt(4, category.getSlot());
            statement.setInt(5, category.isEnabled() ? 1 : 0);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save category " + category.getKey(), exception);
        }
    }


    public synchronized void deleteAll() {
        String sql = "DELETE FROM categories";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not clear categories.", exception);
        }
    }

    public synchronized boolean isEmpty() {
        String sql = "SELECT COUNT(*) FROM categories";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() && resultSet.getInt(1) == 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not check category count.", exception);
        }
    }

    private ShopCategory map(ResultSet resultSet) throws SQLException {
        Material material = Material.matchMaterial(resultSet.getString("material"));
        if (material == null) {
            material = Material.CHEST;
        }
        return new ShopCategory(
                resultSet.getString("key"),
                resultSet.getString("display_name"),
                material,
                resultSet.getInt("slot"),
                resultSet.getInt("enabled") == 1
        );
    }
}
