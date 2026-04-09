package me.wobble.wobbleshop.model;

import org.bukkit.Material;

public final class ShopCategory {

    private final String key;
    private final String displayName;
    private final Material material;
    private final int slot;
    private final boolean enabled;

    public ShopCategory(String key, String displayName, Material material, int slot, boolean enabled) {
        this.key = key;
        this.displayName = displayName;
        this.material = material;
        this.slot = slot;
        this.enabled = enabled;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getMaterial() {
        return material;
    }

    public int getSlot() {
        return slot;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
