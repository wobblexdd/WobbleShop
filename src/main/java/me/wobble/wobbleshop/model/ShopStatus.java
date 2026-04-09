package me.wobble.wobbleshop.model;

public enum ShopStatus {
    ACTIVE,
    OUT_OF_STOCK,
    COMING_SOON,
    DISABLED;

    public ShopStatus next() {
        ShopStatus[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
