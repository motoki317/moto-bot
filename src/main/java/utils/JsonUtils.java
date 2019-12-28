package utils;

import com.fasterxml.jackson.databind.JsonNode;

import javax.annotation.Nullable;

public class JsonUtils {
    @Nullable
    public static String getNullableString(@Nullable JsonNode node, String fieldName) {
        return node == null ? null : node.has(fieldName) && !node.get(fieldName).isNull() ? node.get(fieldName).asText() : null;
    }

    public static int getIntSafely(@Nullable JsonNode node, String fieldName) {
        return node == null ? 0 : node.has(fieldName) ? node.get(fieldName).asInt() : 0;
    }

    public static long getLongSafely(@Nullable JsonNode node, String fieldName) {
        return node == null ? 0L : node.has(fieldName) ? node.get(fieldName).asLong() : 0L;
    }
}
