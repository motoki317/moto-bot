package db.model.musicSetting;

import music.RepeatState;

import javax.annotation.Nullable;

public class MusicSetting {
    private final long guildId;
    private int volume;
    private RepeatState repeat;
    private boolean showNp;
    @Nullable
    private Long restrictChannel;

    public MusicSetting(long guildId, int volume, RepeatState repeat, boolean showNp, @Nullable Long restrictChannel) {
        this.guildId = guildId;
        this.volume = volume;
        this.repeat = repeat;
        this.showNp = showNp;
        this.restrictChannel = restrictChannel;
    }

    public static MusicSetting getDefault(long guildId) {
        return new MusicSetting(
                guildId,
                100,
                RepeatState.OFF,
                true,
                null
        );
    }

    public long getGuildId() {
        return guildId;
    }

    public int getVolume() {
        return volume;
    }

    public RepeatState getRepeat() {
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
