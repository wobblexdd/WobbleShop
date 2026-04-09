package me.wobble.wobbleshop.model;

public enum StockType {
    STATIC,
    LIMITED;

    public StockType next() {
        return this == STATIC ? LIMITED : STATIC;
    }

    public boolean isStatic() {
        return this == STATIC;
    }

    public static StockType fromStorage(String value) {
        if (value == null || value.isBlank()) {
            return STATIC;
        }

        return switch (value.toUpperCase()) {
            case "UNLIMITED", "STATIC" -> STATIC;
            case "LIMITED" -> LIMITED;
            default -> STATIC;
        };
    }
}
