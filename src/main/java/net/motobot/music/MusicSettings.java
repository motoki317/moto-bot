package net.motobot.music;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MusicSettings implements Serializable {

    private static final long serialVersionUID = -7433857572606196803L;

    private int volume;
    private boolean retainVolume;
    private RepeatState repeat;
    private boolean retainRepeat;
    private boolean saveQueue;
    private boolean voteMode;
    private boolean DJMode;
    private List<Long> DJRoleID;
    private boolean showNp;
    private int voteNeeded; // percentage
    private long restrictChannel; // If not 0, music commands only work in this channel

    private MusicSettings() {
        this.volume = 100;
        this.retainVolume = true;
        this.repeat = RepeatState.OFF;
        this.retainRepeat = true;
        this.saveQueue = true;
        this.voteMode = false;
        DJMode = false;
        DJRoleID = new ArrayList<>();
        this.showNp = true;
        this.voteNeeded = 60;
        this.restrictChannel = 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(volume, retainVolume, repeat, retainRepeat, saveQueue, voteMode,
                DJMode, DJRoleID, showNp, voteNeeded, restrictChannel);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MusicSettings) {
            MusicSettings a = (MusicSettings) obj;

            return volume == a.volume
                    && retainVolume == a.retainVolume
                    && repeat == a.repeat
                    && retainRepeat == a.retainRepeat
                    && saveQueue == a.saveQueue
                    && voteMode == a.voteMode
                    && DJMode == a.DJMode
                    && DJRoleID.equals(a.DJRoleID)
                    && showNp == a.showNp
                    && voteNeeded == a.voteNeeded
                    && restrictChannel == a.restrictChannel;
        }
        return false;
    }

    static MusicSettings getDefaultSettings() {
        return new MusicSettings();
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public RepeatState getRepeat() {
        return repeat;
    }

    void setRepeat(RepeatState repeat) {
        this.repeat = repeat;
    }

    public boolean isSaveQueue() {
        return saveQueue;
    }

    public boolean isVoteMode() {
        return voteMode;
    }

    public boolean isDJMode() {
        return DJMode;
    }

    public List<Long> getDJRoleID() {
        return DJRoleID;
    }

    public boolean isShowNp() {
        return showNp;
    }

    public int getVoteNeeded() {
        return voteNeeded;
    }

    public boolean isRetainVolume() {
        return retainVolume;
    }

    public boolean isRetainRepeat() {
        return retainRepeat;
    }

    public long getRestrictChannel() {
        return restrictChannel;
    }
}
