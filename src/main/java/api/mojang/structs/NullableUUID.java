package api.mojang.structs;

import org.jetbrains.annotations.Contract;
import utils.UUID;

import javax.annotation.Nullable;

/**
 * Nullable UUID, if null, indicates that it failed to retrieve uuid even though the request was correctly made.
 */
public class NullableUUID {
    @Nullable
    private UUID uuid;

    @Contract(pure = true)
    public NullableUUID(@Nullable UUID uuid) {
        this.uuid = uuid;
    }

    @Nullable
    public UUID getUuid() {
        return uuid;
    }
}
