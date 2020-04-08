package net.motobot.DiscordBot;

import net.motobot.music.MusicSettings;
import net.motobot.wrapper.CustomTimeFormat;
import net.motobot.wrapper.TrackEntry;

import java.io.Serializable;
import java.util.*;

public class PlayerData implements Serializable {
    // player data and bot data

    private static final long serialVersionUID = 1L;
    public long maxPlayersOnline;
    public long maxPlayerTime;

    public long minPlayersOnline;
    public long minPlayerTime;

    public Set<TrackEntry> trackings;

    public Map<Long, Integer> guildTimeZones;
    public Map<Long, String> guildPrefixes;
    public Map<Long, CustomTimeFormat> guildTimeFormats;

    public Map<Long, Integer> channelTimeZones;
    public Map<Long, String> channelPrefixes;
    public Map<Long, CustomTimeFormat> channelTimeFormats;
    // channel id, offset / prefix

    public Map<Long, Integer> userTimeZones;
    public Map<Long, String> userPrefixes;
    public Map<Long, CustomTimeFormat> userTimeFormats;
    // user offset (overrides channel for active command, or user input command)

    // public Map<String, Long> friendList; // removed field
    public Map<String, List<Long>> friendsList;
    // player name, channel id list

    public Map<String, List<Map<Long, Long>>> notifyList;
    // player name, channel id/ user id list

    public Map<String, Long> upServer;
    // list of current up servers, for each up servers, the timestamp of them when they launched
    // migrated from TerritoryHistory.java

    public Set<Long> ignoredChannels;
    // if received message was contained in this list, ignore it

    public long botLastUpTime;
    // stores last bot's uptime

    public boolean manualGCOn;
    // if on, enables manual-auto (?) GC every 10 minutes inside AutoSave.java

    public long botLastReboot;
    // stores last bot's reboot time in epoch millis
//
//    public Map<Long, Permissions> adminGuilds;
//    public Map<Long, Permissions> adminChannels;
//    public Map<Long, Permissions> adminUsers;
//    // if guilds/channels/users are contained in this, they're given admin perms
//    // just mod perms for now
//    public Map<Long, Map<Long, Permissions>> rolePermissions;

    public Map<Long, Map<String, List<String>>> customTerrList;
    // user ID -> Custom List name, Territory name list
    public Map<Long, Map<String, List<String>>> customGuildList;
    // user ID -> Custom List name, Guild name list

//    public Map<String, LinkedHashMap<Long, GuildLvAndXp>> guildXpTransition;
    // String -> Map Epoch millis, Lv and XP at the time

    public Map<Long, Long> serverLogChannels;
    // for server mod log (similar to audit log, or tatsu/dyno's server log)
    // guild id, channel id

    // public Map<Long, Map.Entry<Long, Map.Entry<Set<Map.Entry<String, Long>>, RepeatState>>> musicQueueCache;
    // public Map<Long, GuildMusicCache> musicCache;
    // ↑ Messed up with serialization ID of GuildMusicCache class (did not implement id)
//    public Map<Long, GuildMusicCache> newMusicCache;
    // For music command, save data when auto save & when they leave channel with queue remaining
    // Guild id, <last save date (epoch millis), actual queue <URI, user id> and repeat state>
    // Clear queue cache after a certain amount of date passed?

//    public Set<Map.Entry<Long, Map.Entry<Map.Entry<Long, Long>, Map.Entry<Integer, RepeatState>>>> interruptedMusic;
    // Save current music playing guild / ch, vc to this
    // Join these Guild ID / Channel ID, Voice Channel ID on bot restart

    public Map<Long, MusicSettings> musicSettings;

    public Map<String, Long> modLastOnline;
    // Mod name, last online epoch millis
    // Can also be used to look up current mod (+ admin) list

    // ---------------
    // Metrics
    // ---------------

    // Random metrics of the player, will load random one currently online player every minute from HeartBeat
    // is meant to reset after 10,000 players scanned
//    public PlayerMetrics playerMetrics;

    // Random leaderboard of the player, this one is meant to stay forever
//    public PlayerLeaderboards playerLeaderboards;

    public PlayerData () {
        trackings = new HashSet<>();

        guildPrefixes = new HashMap<>();
        guildTimeFormats = new HashMap<>();
        guildTimeZones = new HashMap<>();

        channelTimeZones = new HashMap<>();
        channelPrefixes = new HashMap<>();
        channelTimeFormats = new HashMap<>();

        userPrefixes = new HashMap<>();
        userTimeZones = new HashMap<>();
        userTimeFormats = new HashMap<>();

        friendsList = new HashMap<>();
        notifyList = new HashMap<>();
        upServer = new HashMap<>();
        ignoredChannels = new HashSet<>();
        manualGCOn = false;

//        adminGuilds = new HashMap<>();
//        adminChannels = new HashMap<>();
//        adminUsers = new HashMap<>();
//
//        rolePermissions = new HashMap<>();

        customTerrList = new HashMap<>();
        customGuildList = new HashMap<>();

//        guildXpTransition = new HashMap<>();

        serverLogChannels = new HashMap<>();

        // musicCache = new HashMap<>();
//        newMusicCache = new HashMap<>();
//        interruptedMusic = new HashSet<>();
//        musicSettings = new HashMap<>();

        modLastOnline = new HashMap<>();
//        Ref.moderators.forEach(mod -> modLastOnline.put(mod, 0L));
    }
}
