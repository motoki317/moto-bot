package db.model.commandLog;

public class CommandLog implements CommandLogId {
    private int id;
    private String kind;
    private String full;
    private long userId;
    private boolean dm;

    public CommandLog(String kind, String full, long userId, boolean dm) {
        this.kind = kind;
        this.full = full;
        this.userId = userId;
        this.dm = dm;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return this.id;
    }

    public String getKind() {
        return kind;
    }

    public String getFull() {
        return full;
    }

    public long getUserId() {
        return userId;
    }

    public boolean isDm() {
        return dm;
    }
}
