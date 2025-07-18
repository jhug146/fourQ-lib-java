package utils;

import java.util.function.Consumer;

public class StringUtils {
    public static String buildString(Consumer<StringBuilder> builderAction) {
        StringBuilder sb = new StringBuilder();
        builderAction.accept(sb);
        return sb.toString();
    }
}