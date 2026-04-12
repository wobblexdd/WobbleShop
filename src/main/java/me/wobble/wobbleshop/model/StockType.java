package me.wobble.wobbleshop.model;

public enum StockType {
    INFINITE,
    LIMITED,
    REGENERATING;

    public StockType next() {
        return switch (this) {
            case INFINITE -> LIMITED;
            case LIMITED -> REGENERATING;
            case REGENERATING -> INFINITE;
        };
    }

    public boolean isInfinite() {
        return this == INFINITE;
    }

    public boolean isLimitedType() {
        return this == LIMITED || this == REGENERATING;
    }

    public boolean isRegenerating() {
        return this == REGENERATING;
    }

    public static StockType fromStorage(String value) {
        if (value == null || value.isBlank()) {
            return INFINITE;
        }

        return switch (value.toUpperCase()) {
            case "UNLIMITED", "STATIC", "INFINITE" -> INFINITE;
            case "LIMITED" -> LIMITED;
            case "REGENERATING" -> REGENERATING;
            default -> INFINITE;
        };
    }
}
