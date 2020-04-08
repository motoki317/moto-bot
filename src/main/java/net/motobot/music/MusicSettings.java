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

    int getVolume() {
        return volume;
    }

    void setVolume(int volume) {
        this.volume = volume;
    }

    RepeatState getRepeat() {
        return repeat;
    }

    void setRepeat(RepeatState repeat) {
        this.repeat = repeat;
    }

    boolean isSaveQueue() {
        return saveQueue;
    }

    boolean isVoteMode() {
        return voteMode;
    }

    boolean isDJMode() {
        return DJMode;
    }

    List<Long> getDJRoleID() {
        return DJRoleID;
    }

    boolean isShowNp() {
        return showNp;
    }

    int getVoteNeeded() {
        return voteNeeded;
    }

    boolean isRetainVolume() {
        return retainVolume;
    }

    boolean isRetainRepeat() {
        return retainRepeat;
    }

    long getRestrictChannel() {
        return restrictChannel;
    }
}
