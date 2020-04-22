package db.model.musicSetting;

import music.RepeatState;

import javax.annotation.Nullable;
import java.util.Objects;

public class MusicSetting implements MusicSettingId {
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

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public void setRepeat(RepeatState repeat) {
        this.repeat = repeat;
    }

    public void setShowNp(boolean showNp) {
        this.showNp = showNp;
    }

    public void setRestrictChannel(@Nullable Long restrictChannel) {
        this.restrictChannel = restrictChannel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MusicSetting that = (MusicSetting) o;
        return guildId == that.guildId &&
                volume == that.volume &&
                showNp == that.showNp &&
                repeat == that.repeat &&
                Objects.equals(restrictChannel, that.restrictChannel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(guildId, volume, repeat, showNp, restrictChannel);
    }
}
