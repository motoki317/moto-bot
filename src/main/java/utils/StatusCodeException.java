package utils;

import java.io.IOException;

/**
 * Used by {@link HttpUtils} class to indicate that one of
 */
public class StatusCodeException extends IOException {
    private final int code;

    StatusCodeException(String message, int code) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
