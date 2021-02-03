package music.handlers;

public enum SearchSite {
    YouTube("ytsearch: "),
    SoundCloud("scsearch: ");

    private final String prefix;

    SearchSite(String prefix) {
        this.prefix = prefix;
    }

    String getPrefix() {
        return prefix;
    }
}
