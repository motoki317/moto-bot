package api.mojang.structs;

import utils.UUID;

import javax.annotation.Nullable;

/**
 * Nullable UUID, if null, indicates that it failed to retrieve uuid even though the request was correctly made.
 */
public record NullableUUID(@Nullable UUID uuid) {
}
