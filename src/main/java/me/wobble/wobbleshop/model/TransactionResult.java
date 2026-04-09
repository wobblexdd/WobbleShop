package me.wobble.wobbleshop.model;

import java.util.Map;

public record TransactionResult(boolean success, String messageKey, Map<String, String> placeholders) {

    public static TransactionResult success(String messageKey, Map<String, String> placeholders) {
        return new TransactionResult(true, messageKey, placeholders);
    }

    public static TransactionResult failure(String messageKey) {
        return new TransactionResult(false, messageKey, Map.of());
    }

    public static TransactionResult failure(String messageKey, Map<String, String> placeholders) {
        return new TransactionResult(false, messageKey, placeholders);
    }
}
