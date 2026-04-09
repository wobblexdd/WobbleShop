package me.wobble.wobbleshop.util;

import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class ColorUtil {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private ColorUtil() {
    }

    public static String colorize(String input) {
        return input == null ? "" : input.replace('&', '§');
    }

    public static Component component(String input) {
        return SERIALIZER.deserialize(input == null ? "" : input);
    }

    public static List<Component> components(List<String> input) {
        return input.stream().map(ColorUtil::component).collect(Collectors.toList());
    }
}
