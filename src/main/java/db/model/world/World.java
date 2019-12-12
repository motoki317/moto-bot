package db.model.world;

import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;

public class World implements WorldId {
    private String name;

    private int players;

    private Timestamp createdAt;

    private Timestamp updatedAt;

    public World(String name, int players) {
        this.name = name;
        this.players = players;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public int getPlayers() {
        return players;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
