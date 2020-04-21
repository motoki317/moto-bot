package db.model.musicSetting;

import javax.annotation.Nullable;

public class MusicSetting {
    private final long guildId;
    private int volume;
    private String repeat;
    private boolean showNp;
    @Nullable
    private Long restrictChannel;

    public MusicSetting(long guildId, int volume, String repeat, boolean showNp, @Nullable Long restrictChannel) {
        this.guildId = guildId;
        this.volume = volume;
        this.repeat = repeat;
        this.showNp = showNp;
        this.restrictChannel = restrictChannel;
    }

    public long getGuildId() {
        return guildId;
    }

    public int getVolume() {
        return volume;
    }

    public String getRepeat() {
        return repeat;
    }

    public boolean isShowNp() {
        return showNp;
    }

    @Nullable
    public Long getRestrictChannel() {
        return restrictChannel;
    }
}
