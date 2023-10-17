package commands.guild;

import api.wynn.WynnApi;
import api.wynn.structs.WynnGuild;
import app.Bot;
import commands.base.GenericCommand;
import commands.event.CommandEvent;
import commands.event.message.SentMessage;
import db.model.dateFormat.CustomDateFormat;
import db.model.guildLeaderboard.GuildLeaderboard;
import db.model.guildLeaderboard.GuildLeaderboardId;
import db.model.guildXpLeaderboard.GuildXpLeaderboard;
import db.model.timezone.CustomTimeZone;
import db.model.world.World;
import db.repository.base.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import utils.FormatUtils;
import utils.InputChecker;
import utils.TableFormatter;
import utils.rateLimit.RateLimitException;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static utils.TableFormatter.Justify.Left;
import static utils.TableFormatter.Justify.Right;

public class GuildStats extends GenericCommand {
    private final Handler handler;

    public GuildStats(Bot bot) {
        this.handler = new Handler(bot);
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"g", "guild"}, {"s", "stats", "info", "i"}};
    }

    @Override
    public @NotNull String[] slashName() {
        return new String[]{"g", "stats"};
    }

    @Override
    public @NotNull OptionData[] slashOptions() {
        return new OptionData[]{
                new OptionData(OptionType.STRING, "guild", "Name or prefix of a guild", true)
        };
    }

    @Override
    public @NotNull String syntax() {
        return "g stats <guild name>";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Shows a guild's general information.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                        .setAuthor("Guild Command Help")
                        .setDescription(this.shortHelp())
                        .addField(
                                "Syntax",
                                "`" + this.syntax() + "`",
                                false
                        )
                        .build()
        ).build();
    }

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(3);
    }

    @Override
    public void process(@NotNull CommandEvent event, @NotNull String[] args) {
        if (args.length <= 2) {
            event.reply(this.longHelp());
            return;
        }

        String guildName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        handler.handle(event, guildName);
    }

    static class Handler {
        private final WynnApi wynnApi;
        private final GuildNameResolver guildNameResolver;
        private final DateFormatRepository dateFormatRepository;
        private final TimeZoneRepository timeZoneRepository;
        private final WorldRepository worldRepository;
        private final TerritoryRepository territoryRepository;
        private final GuildLeaderboardRepository guildLeaderboardRepository;
        private final GuildXpLeaderboardRepository guildXpLeaderboardRepository;
        private final String guildBannerUrl;

        Handler(Bot bot) {
            this.wynnApi = new WynnApi(bot.getLogger());
            this.guildNameResolver = new GuildNameResolver(
                    bot.getDatabase().getGuildRepository(),
                    bot.getButtonClickManager()
            );
            this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
            this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
            this.worldRepository = bot.getDatabase().getWorldRepository();
            this.territoryRepository = bot.getDatabase().getTerritoryRepository();
            this.guildLeaderboardRepository = bot.getDatabase().getGuildLeaderboardRepository();
            this.guildXpLeaderboardRepository = bot.getDatabase().getGuildXpLeaderboardRepository();
            this.guildBannerUrl = bot.getProperties().guildBannerUrl;
        }

        public void handle(@NotNull CommandEvent event, @NotNull String guildName) {
            if (!InputChecker.isValidWynncraftGuildName(guildName)) {
                event.reply(String.format("Given guild name `%s` doesn't seem to be a valid Wynncraft guild name...",
                        guildName));
                return;
            }

            this.guildNameResolver.resolve(
                    guildName,
                    event,
                    (next, resolvedName, prefix) -> handleResolved(event, next, resolvedName)
            );
        }

        private void handleResolved(@NotNull CommandEvent event, @NotNull SentMessage next, @NotNull String guildName) {
            WynnGuild guild;
            try {
                guild = this.wynnApi.getGuildStats(guildName);
            } catch (RateLimitException e) {
                next.editException(e.getMessage());
                return;
            }

            if (guild == null) {
                next.editException(String.format("Failed to retrieve guild data for %s.", guildName));
                return;
            }

            CustomDateFormat customDateFormat = this.dateFormatRepository.getDateFormat(event);
            CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(event);

            Function<Integer, Message> pageSupplier = page -> getPage(page, guild, customDateFormat, customTimeZone);
            next.editMultiPage(event.getBot(), pageSupplier, () -> maxPage(guild));
        }

        private static final int MEMBERS_PER_CONTRIBUTE_PAGE = 30;

        private int maxPage(WynnGuild guild) {
            int count = (int) guild.getMembers().stream()
                    .filter(m -> m.contributed() != 0).count();
            int contributeExtraPages = (count - 1) / MEMBERS_PER_CONTRIBUTE_PAGE;
            // 0: main
            // 1: online players
            // 2, 3, 4, 5, 6: chiefs, strategists, captains, recruiters, recruits
            // 7~: xp contributions
            return 7 + contributeExtraPages;
        }

        private void makeGuildInfo(StringBuilder sb, WynnGuild guild) {
            String guildName = guild.getName();

            Date lastUpdatedAt = this.guildLeaderboardRepository.getNewestDate();
            if (lastUpdatedAt == null) {
                // Default view
                sb.append(String.format("Level %s | %s%%\n", guild.getLevel(), guild.getXpPercent()));
                return;
            }
            GuildLeaderboard lb = this.guildLeaderboardRepository.findOne(new GuildLeaderboardId() {
                @Override
                public String getName() {
                    return guild.getName();
                }

                @Override
                public Date getUpdatedAt() {
                    return lastUpdatedAt;
                }
            });
            int levelRank = this.guildLeaderboardRepository.getLevelRank(guildName);
            // It is possible that the lb entry exists but couldn't safely get the level rank for the guild
            String levelRankStr = levelRank != -1 ? String.format(" (#%s)", levelRank) : "";

            // xp lb entry and xp rank are both expected to be returned safely
            GuildXpLeaderboard xpLBEntry = this.guildXpLeaderboardRepository.findOne(() -> guildName);
            int xpRank = this.guildXpLeaderboardRepository.getXPRank(guildName);
            if (xpLBEntry != null && xpRank != -1 && lb != null) {
                // The guild is in the leaderboard, and has earned at least 1 xp over the last 24h
                sb.append(String.format("Level %s | %s%%, %s XP%s\n", guild.getLevel(), guild.getXpPercent(),
                        FormatUtils.truncateNumber(BigDecimal.valueOf(xpLBEntry.getXp())),
                        levelRankStr
                ));
                long seconds = (xpLBEntry.getTo().getTime() - xpLBEntry.getFrom().getTime()) / 1000L;
                sb.append(String.format("%s XP gained in last %s (#%s)\n",
                        FormatUtils.truncateNumber(BigDecimal.valueOf(xpLBEntry.getXpDiff())),
                        FormatUtils.formatReadableTime(seconds, false, "m"), xpRank));
                return;
            }

            if (lb != null) {
                // The guild is in the leaderboard, so we can get exact xp,
                // but it is NOT on the xp leaderboard i.e. hasn't earned a single xp over the last 24h
                sb.append(String.format("Level %s | %s%%, %s XP%s\n", guild.getLevel(), guild.getXpPercent(),
                        FormatUtils.truncateNumber(BigDecimal.valueOf(lb.getXp())),
                        levelRankStr
                ));
                return;
            }

            if (xpLBEntry != null && xpRank != -1) {
                // The guild is on the xp leaderboard, i.e. has earned at least 1 xp over the last 24h,
                // but it is no longer on the current leaderboard so we can-NOT get the exact xp
                sb.append(String.format("Level %s | %s%%\n", guild.getLevel(), guild.getXpPercent()));
                long seconds = (xpLBEntry.getTo().getTime() - xpLBEntry.getFrom().getTime()) / 1000L;
                // `to` time of the xp lb might not be the last updated time
                sb.append(String.format("%s XP gained in %s (#%s)\n",
                        FormatUtils.truncateNumber(BigDecimal.valueOf(xpLBEntry.getXpDiff())),
                        FormatUtils.formatReadableTime(seconds, false, "m"), xpRank));
                return;
            }

            // Default view
            sb.append(String.format("Level %s | %s%%\n", guild.getLevel(), guild.getXpPercent()));
        }

        private Message getPage(int page,
                                @NotNull WynnGuild guild,
                                @NotNull CustomDateFormat customDateFormat,
                                @NotNull CustomTimeZone customTimeZone) {
            DateFormat dateFormat = customDateFormat.getDateFormat().getSecondFormat();
            dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());

            StringBuilder sb = new StringBuilder();
            sb.append("```ml\n");
            sb.append(String.format("%s [%s]\n", guild.getName(), guild.getPrefix()));

            sb.append("\n");
            makeGuildInfo(sb, guild);
            int bars = (int) Math.round((double) guild.getXpPercent() / 5.0d);
            sb.append(makeBar(bars));

            sb.append("\n\n");
            getMainView(sb, page, guild, customDateFormat, customTimeZone);
            sb.append("\n");

            sb.append(String.format("  last guild info update: %s\n",
                    dateFormat.format(guild.getRequestedAt())
            ));
            sb.append(String.format("last online stats update: %s\n",
                    dateFormat.format(getLastOnlinePlayerUpdate())
            ));

            sb.append("```");

            MessageBuilder mb = new MessageBuilder(sb.toString());

            if (guild.getBanner() != null) {
                mb.setEmbeds(
                        new EmbedBuilder()
                                .setAuthor("Guild Banner")
                                .setDescription("Tier: " + guild.getBanner().getTier())
                                .setThumbnail(this.guildBannerUrl + guild.getName().replace(" ", "%20"))
                                .build()
                );
            }

            return mb.build();
        }

        private void getMainView(@NotNull StringBuilder sb,
                                 int page,
                                 @NotNull WynnGuild guild,
                                 @NotNull CustomDateFormat customDateFormat,
                                 @NotNull CustomTimeZone customTimeZone) {
            switch (page) {
                // main
                case 0 -> getFirstPage(sb, guild, customDateFormat, customTimeZone);
                // online players
                case 1 -> getOnlinePlayers(sb, guild);
                // chiefs, strategists, captains, recruiters, recruits
                case 2 -> getMembers(sb, guild, Rank.CHIEF);
                case 3 -> getMembers(sb, guild, Rank.STRATEGIST);
                case 4 -> getMembers(sb, guild, Rank.CAPTAIN);
                case 5 -> getMembers(sb, guild, Rank.RECRUITER);
                case 6 -> getMembers(sb, guild, Rank.RECRUIT);
                default -> {
                    int contributePageNum = page - 7;
                    getContributePage(sb, guild, contributePageNum);
                }
            }
        }

        /**
         * Make a gauge of max 20 bars.
         *
         * @param bars Number of bars.
         * @return Formatted gauge.
         */
        private String makeBar(int bars) {
            bars = Math.max(0, Math.min(20, bars));
            return String.format(
                    "[%s%s]",
                    nCopies("=", bars), nCopies("-", 20 - bars)
            );
        }

        @NotNull
        private Date getLastOnlinePlayerUpdate() {
            List<World> worlds = this.worldRepository.findAll();
            if (worlds == null || worlds.size() == 0) {
                return new Date();
            } else {
                return worlds.get(0).getUpdatedAt();
            }
        }

        private void getFirstPage(@NotNull StringBuilder sb,
                                  @NotNull WynnGuild guild,
                                  @NotNull CustomDateFormat customDateFormat,
                                  @NotNull CustomTimeZone customTimeZone) {
            sb.append("---- Guild Information ----\n");
            sb.append("\n");
            sb.append("Owner: ").append(guild.getOwnerName()).append("\n");

            long onlineMembers = guild.getMembers().stream()
                    .filter(m -> this.wynnApi.findPlayer(m.name()) != null).count();
            sb.append(String.format(
                    "Members: %s (Online: %s)\n",
                    guild.getMembers().size(),
                    onlineMembers
            ));

            DateFormat dateFormat = customDateFormat.getDateFormat().getSecondFormat();
            dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());
            sb.append(String.format(
                    "Created: %s (%s)\n",
                    dateFormat.format(guild.getCreated()), customTimeZone.getFormattedTime()
            ));

            int territories = this.territoryRepository.countGuildTerritories(guild.getName());
            int territoryRank = this.territoryRepository.getGuildTerritoryRanking(guild.getName());
            if (territoryRank <= 0) {
                sb.append(String.format("Territory: %s\n", territories));
            } else {
                sb.append(String.format("Territory: %s (#%s)\n", territories, territoryRank));
            }
        }

        private void getOnlinePlayers(@NotNull StringBuilder sb,
                                      @NotNull WynnGuild guild) {
            class Member {
                private final String name;
                private final Rank rank;
                private final String server;

                private Member(String name, Rank rank, String server) {
                    this.name = name;
                    this.rank = rank;
                    this.server = server;
                }
            }

            List<Member> onlineMembers = guild.getMembers().stream()
                    .map(m -> new Member(m.name(), Rank.valueOf(m.rank().toUpperCase()), this.wynnApi.findPlayer(m.name())))
                    .filter(m -> m.server != null)
                    .sorted((m1, m2) -> m2.rank.rank - m1.rank.rank)
                    .toList();

            if (onlineMembers.size() == 1) {
                sb.append("---- Online Member (1) ----\n");
            } else {
                sb.append(String.format("---- Online Members (%s) ----\n", onlineMembers.size()));
            }

            sb.append("\n");

            if (onlineMembers.isEmpty()) {
                sb.append("There are no members online.\n");
            } else {
                TableFormatter tf = new TableFormatter(false);
                tf.addColumn("Name", Left, Left)
                        .addColumn("Rank", Left, Left)
                        .addColumn("Server", Left, Left);
                for (Member m : onlineMembers) {
                    tf.addRow(m.name, nCopies("*", m.rank.rank), m.server);
                }
                tf.toString(sb);
            }
        }

        private void getMembers(@NotNull StringBuilder sb,
                                @NotNull WynnGuild guild,
                                @NotNull Rank rank) {
            class Member {
                private final String name;
                private final String server;

                private Member(String name, String server) {
                    this.name = name;
                    this.server = server;
                }
            }
            List<Member> members = guild.getMembers().stream()
                    .filter(m -> m.rank().equals(rank.name()))
                    .map(m -> new Member(m.name(), this.wynnApi.findPlayer(m.name())))
                    .sorted(Comparator.comparing(m -> m.name))
                    .toList();

            int justifyName = members.stream().mapToInt(e -> e.name.length()).max().orElse(1);

            sb.append(String.format(
                    "---- %s (%s) ----\n",
                    rank.readableName + (members.size() != 1 ? "s" : ""),
                    members.size()
            ));
            sb.append("\n");

            for (Member member : members) {
                if (member.server != null) {
                    sb.append(String.format(
                            "%s%s : %s\n",
                            member.name, nCopies(" ", justifyName - member.name.length()),
                            member.server
                    ));
                } else {
                    sb.append(member.name).append("\n");
                }
            }
        }

        private void getContributePage(@NotNull StringBuilder sb,
                                       @NotNull WynnGuild guild,
                                       int pageNum) {
            class Member {
                private final String name;
                private final Rank rank;
                private final String contributed;

                private Member(String name, Rank rank, String contributed) {
                    this.name = name;
                    this.rank = rank;
                    this.contributed = contributed;
                }
            }
            List<Member> members = guild.getMembers().stream()
                    .filter(m -> m.contributed() != 0)
                    .sorted((m1, m2) -> Long.compare(m2.contributed(), m1.contributed()))
                    .map(m -> new Member(
                            m.name(), Rank.valueOf(m.rank().toUpperCase()), String.format("%,d xp", m.contributed()
                    )))
                    .toList();

            sb.append("---- XP Contributions ----\n");
            sb.append("\n");

            TableFormatter tf = new TableFormatter(false);
            tf.addColumn("", Left, Left)
                    .addColumn("Name", Left, Left)
                    .addColumn("Rank", Left, Left)
                    .addColumn("XP", Left, Right);
            int min = pageNum * MEMBERS_PER_CONTRIBUTE_PAGE;
            int max = Math.min((pageNum + 1) * MEMBERS_PER_CONTRIBUTE_PAGE, members.size());
            for (int i = min; i < max; i++) {
                Member m = members.get(i);
                tf.addRow((i + 1) + ".", m.name, nCopies("*", m.rank.rank), m.contributed);
            }
            tf.toString(sb);
        }
    }

    private static String nCopies(String s, int n) {
        return String.join("", Collections.nCopies(n, s));
    }

    private enum Rank {
        OWNER(5, "Owner"),
        CHIEF(4, "Chief"),
        STRATEGIST(3, "Strategist"),
        CAPTAIN(2, "Captain"),
        RECRUITER(1, "Recruiter"),
        RECRUIT(0, "Recruit");

        private final int rank;
        private final String readableName;

        Rank(int rank, String readableName) {
            this.rank = rank;
            this.readableName = readableName;
        }
    }
}
