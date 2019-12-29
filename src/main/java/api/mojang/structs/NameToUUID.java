package api.mojang.structs;

public class NameToUUID {
    // trimmed uuid
    private String id;
    private String name;
    private boolean legacy;
    private boolean demo;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isLegacy() {
        return legacy;
    }

    public boolean isDemo() {
        return demo;
    }
}
