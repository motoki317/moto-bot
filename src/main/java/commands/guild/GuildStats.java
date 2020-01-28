package commands.guild;

import api.wynn.WynnApi;
import api.wynn.structs.WynnGuild;
import app.Bot;
import commands.base.GenericCommand;
import db.model.dateFormat.CustomDateFormat;
import db.model.timezone.CustomTimeZone;
import db.model.world.World;
import db.repository.DateFormatRepository;
import db.repository.TerritoryRepository;
import db.repository.TimeZoneRepository;
import db.repository.WorldRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import update.multipage.MultipageHandler;
import update.reaction.ReactionManager;
import utils.InputChecker;
import utils.MinecraftColor;
import utils.rateLimit.RateLimitException;

import java.text.DateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GuildStats extends GenericCommand {
    private final Handler handler;

    public GuildStats(Bot bot) {
        this.handler = new Handler(bot);
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"g", "guild"}, {"s", "stats"}};
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
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        if (args.length <= 2) {
            respond(event, this.longHelp());
            return;
        }

        String guildName = String.join("\n", Arrays.copyOfRange(args, 1, args.length));
        handler.handle(event, guildName);
    }

    static class Handler {
        private final WynnApi wynnApi;
        private final ReactionManager reactionManager;
        private final GuildNameResolver guildNameResolver;
        private final DateFormatRepository dateFormatRepository;
        private final TimeZoneRepository timeZoneRepository;
        private final WorldRepository worldRepository;
        private final TerritoryRepository territoryRepository;

        Handler(Bot bot) {
            this.wynnApi = new WynnApi(bot.getLogger(), bot.getProperties().wynnTimeZone);
            this.reactionManager = bot.getReactionManager();
            this.guildNameResolver = new GuildNameResolver(
                    bot.getResponseManager(),
                    bot.getDatabase().getGuildRepository()
            );
            this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
            this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
            this.worldRepository = bot.getDatabase().getWorldRepository();
            this.territoryRepository = bot.getDatabase().getTerritoryRepository();
        }

        public void handle(@NotNull MessageReceivedEvent event, @NotNull String guildName) {
            if (!InputChecker.isValidWynncraftGuildName(guildName)) {
                respond(event,
                        new EmbedBuilder()
                                .setColor(MinecraftColor.RED.getColor())
                                .setDescription(String.format("Given guild name `%s` doesn't seem to be a valid Wynncraft guild name...",
                                        guildName))
                                .build()
                );
                return;
            }

            this.guildNameResolver.resolve(
                    guildName,
                    event.getTextChannel(),
                    event.getAuthor(),
                    (resolvedName, prefix) -> handleResolved(event, resolvedName),
                    reason -> respondError(event, reason)
            );
        }

        private void handleResolved(@NotNull MessageReceivedEvent event, @NotNull String guildName) {
            WynnGuild guild;
            try {
                guild = this.wynnApi.getGuildStatsWaitable(guildName);
            } catch (RateLimitException e) {
                respond(event,
                        new EmbedBuilder()
                        .setColor(MinecraftColor.RED.getColor())
                        .setDescription(e.getMessage())
                        .build()
                );
                return;
            }

            if (guild == null) {
                respond(event,
                        new EmbedBuilder()
                                .setColor(MinecraftColor.RED.getColor())
                                .setDescription(String.format("Failed to retrieve guild data for %s.", guildName))
                                .build()
                );
                return;
            }

            CustomDateFormat customDateFormat = this.dateFormatRepository.getDateFormat(event);
            CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(event);

            Function<Integer, Message> pageSupplier = pageSupplier(guild, customDateFormat, customTimeZone);
            respond(event, pageSupplier.apply(0), msg -> {
                MultipageHandler handler = new MultipageHandler(
                        msg, pageSupplier, () -> maxPage(guild)
                );
                this.reactionManager.addEventListener(handler);
            });
        }

        private static final int MEMBERS_PER_CONTRIBUTE_PAGE = 30;

        private int maxPage(WynnGuild guild) {
            int count = (int) guild.getMembers().stream()
                    .filter(m -> m.getContributed() != 0).count();
            int contributeExtraPages = (count - 1) / MEMBERS_PER_CONTRIBUTE_PAGE;
            // 0: main
            // 1: online players
            // 2, 3, 4, 5: chiefs, captains, recruiters, recruits
            // 6~: xp contributions
            return 6 + contributeExtraPages;
        }

        private Function<Integer, Message> pageSupplier(@NotNull WynnGuild guild,
                                                        @NotNull CustomDateFormat customDateFormat,
                                                        @NotNull CustomTimeZone customTimeZone) {
            DateFormat dateFormat = customDateFormat.getDateFormat().getSecondFormat();
            dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());

            return page -> {
                List<String> ret = new ArrayList<>();

                ret.add("```ml");
                ret.add(String.format("%s [%s]", guild.getName(), guild.getPrefix()));

                ret.add("");

                // TODO: precise xp, xp gained ranking
                /* Something along:
                Level 79 | 44.9%, 29.18B XP (#2)
                1.418B XP gained in last 1 d  0 h  0 m
                [=========-----------]
                 */
                ret.add(String.format("Level %s | %s%%", guild.getLevel(), guild.getXp()));
                int bars = (int) Math.round(Double.parseDouble(guild.getXp()) / 5.0d);
                ret.add(makeBar(bars));

                ret.add("");

                switch (page) {
                    case 0:
                        // main
                        ret.add(getMainPage(guild, customDateFormat, customTimeZone));
                        break;
                    case 1:
                        // online players
                        ret.add(getOnlinePlayers(guild));
                        break;
                    case 2:
                        // chiefs, captains, recruiters, recruits
                        ret.add(getMembers(guild, Rank.CHIEF));
                        break;
                    case 3:
                        ret.add(getMembers(guild, Rank.CAPTAIN));
                        break;
                    case 4:
                        ret.add(getMembers(guild, Rank.RECRUITER));
                        break;
                    case 5:
                        ret.add(getMembers(guild, Rank.RECRUIT));
                        break;
                    default:
                        int contributePageNum = page - 6;
                        ret.add(getContributePage(guild, contributePageNum));
                        break;
                }

                ret.add("");

                ret.add(String.format("  last guild info update: %s",
                        dateFormat.format(new Date(guild.getRequest().getTimestamp() * 1000))
                ));
                ret.add(String.format("last online stats update: %s",
                        dateFormat.format(getLastOnlinePlayerUpdate())
                ));

                ret.add("```");

                return new MessageBuilder(
                        String.join("\n", ret)
                ).build();
            };
        }

        /**
         * Make a gauge of max 20 bars.
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

        @NotNull
        private String getMainPage(@NotNull WynnGuild guild,
                                   @NotNull CustomDateFormat customDateFormat,
                                   @NotNull CustomTimeZone customTimeZone) {
            List<String> ret = new ArrayList<>();
            ret.add("---- Guild Information ----");

            ret.add("");

            ret.add("Owner: " + guild.getOwnerName());

            long onlineMembers = guild.getMembers().stream()
                    .filter(m -> this.wynnApi.findPlayer(m.getName()) != null).count();
            ret.add(String.format(
                    "Members: %s / %s (Online: %s)",
                    guild.getMembers().size(),
                    guild.getLevel() + 4,
                    onlineMembers
            ));

            DateFormat dateFormat = customDateFormat.getDateFormat().getSecondFormat();
            dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());
            ret.add(String.format(
                    "Created: %s (%s)",
                    dateFormat.format(guild.getCreated()), customTimeZone.getFormattedTime()
            ));

            int territories = this.territoryRepository.countGuildTerritories(guild.getName());
            int territoryRank = this.territoryRepository.getGuildTerritoryRankingSpecific(guild.getName());
            if (territoryRank <= 0) {
                ret.add(
                        String.format("Territory: %s", territories)
                );
            } else {
                ret.add(
                        String.format("Territory: %s (#%s)", territories, territoryRank)
                );
            }
            return String.join("\n", ret);
        }

        @NotNull
        private String getOnlinePlayers(@NotNull WynnGuild guild) {
            List<String> ret = new ArrayList<>();

            class Member {
                private String name;
                private Rank rank;
                private String server;

                private Member(String name, Rank rank, String server) {
                    this.name = name;
                    this.rank = rank;
                    this.server = server;
                }
            }

            List<Member> onlineMembers = guild.getMembers().stream()
                    .map(m -> new Member(m.getName(), Rank.valueOf(m.getRank()), this.wynnApi.findPlayer(m.getName())))
                    .filter(m -> m.server != null)
                    .sorted((m1, m2) -> m2.rank.rank - m1.rank.rank).collect(Collectors.toList());

            if (onlineMembers.size() == 1) {
                ret.add("---- Online Member (1) ----");
            } else {
                ret.add(String.format("---- Online Members (%s) ----", onlineMembers.size()));
            }

            ret.add("");

            if (onlineMembers.size() != 0) {
                int justifyName = onlineMembers.stream().mapToInt(e -> e.name.length()).max().getAsInt();
                justifyName = Math.max(justifyName, 4);

                ret.add(String.format("Name%s | Rank | Server", nCopies(" ", justifyName - 4)));
                ret.add(String.format("%s+------+--------", nCopies("-", justifyName)));

                for (Member onlineMember : onlineMembers) {
                    ret.add(String.format(
                            "%s%s | %s%s | %s",
                            onlineMember.name, nCopies(" ", justifyName - onlineMember.name.length()),
                            nCopies("*", onlineMember.rank.rank), nCopies(" ", 4 - onlineMember.rank.rank),
                            onlineMember.server
                    ));
                }
            } else {
                ret.add("There are no members online.");
            }

            return String.join("\n", ret);
        }

        @NotNull
        private String getMembers(@NotNull WynnGuild guild, Rank rank) {
            List<String> ret = new ArrayList<>();

            class Member {
                private String name;
                private String server;

                private Member(String name, String server) {
                    this.name = name;
                    this.server = server;
                }
            }
            List<Member> members = guild.getMembers().stream()
                    .filter(m -> m.getRank().equals(rank.name()))
                    .map(m -> new Member(m.getName(), this.wynnApi.findPlayer(m.getName())))
                    .sorted(Comparator.comparing(m -> m.name))
                    .collect(Collectors.toList());

            int justifyName = members.stream().mapToInt(e -> e.name.length()).max().orElse(1);

            ret.add(String.format(
                    "---- %s (%s) ----",
                    rank.readableName + (members.size() != 1 ? "s" : ""),
                    members.size()
            ));
            ret.add("");

            for (Member member : members) {
                if (member.server != null) {
                    ret.add(String.format(
                            "%s%s : %s",
                            member.name, nCopies(" ", justifyName - member.name.length()),
                            member.server
                    ));
                } else {
                    ret.add(member.name);
                }
            }

            return String.join("\n", ret);
        }

        @NotNull
        private String getContributePage(@NotNull WynnGuild guild, int pageNum) {
            List<String> ret = new ArrayList<>();

            class Member {
                private String name;
                private Rank rank;
                private String contributed;
                private String rankNum;

                private Member(String name, Rank rank, String contributed) {
                    this.name = name;
                    this.rank = rank;
                    this.contributed = contributed;
                }
            }
            List<Member> members = guild.getMembers().stream()
                    .filter(m -> m.getContributed() != 0)
                    .sorted((m1, m2) -> Long.compare(m2.getContributed(), m1.getContributed()))
                    .map(m -> new Member(
                            m.getName(), Rank.valueOf(m.getRank()), String.format("%,d xp", m.getContributed()
                    )))
                    .collect(Collectors.toList());

            int justifyName = members.stream().mapToInt(m -> m.name.length()).max().orElse(4);
            for (int i = 0; i < members.size(); i++) {
                members.get(i).rankNum = (i + 1) + ".";
            }
            int justifyRankNum = members.stream().mapToInt(m -> m.rankNum.length()).max().orElse(2);
            int justifyXp = members.stream().mapToInt(m -> m.contributed.length()).max().orElse(2);

            ret.add("---- XP Contributions ----");
            ret.add("");
            ret.add(String.format(
                    "%s Name%s | Rank | XP%s",
                    nCopies(" ", justifyRankNum), nCopies(" ", justifyName - 4),
                    nCopies(" ", justifyXp - 2)
            ));
            ret.add(String.format(
                    "%s-%s-+------+-%s",
                    nCopies("-", justifyRankNum), nCopies("-", justifyName),
                    nCopies("-", justifyXp)
            ));

            int min = pageNum * MEMBERS_PER_CONTRIBUTE_PAGE;
            int max = Math.min((pageNum + 1) * MEMBERS_PER_CONTRIBUTE_PAGE, members.size());

            for (int i = 0; i < members.size(); i++) {
                if (i < min || max <= i) continue;

                Member member = members.get(i);
                ret.add(String.format(
                        "%s%s %s%s | %s%s | %s%s",
                        member.rankNum, nCopies(" ", justifyRankNum - member.rankNum.length()),
                        member.name, nCopies(" ", justifyName - member.name.length()),
                        nCopies("*", member.rank.rank), nCopies(" ", 4 - member.rank.rank),
                        nCopies(" ", justifyXp - member.contributed.length()), member.contributed
                ));
            }

            return String.join("\n", ret);
        }
    }

    private static String nCopies(String s, int n) {
        return String.join("", Collections.nCopies(n, s));
    }

    private enum Rank {
        OWNER(4, "Owner"),
        CHIEF(3, "Chief"),
        CAPTAIN(2, "Captain"),
        RECRUITER(1, "Recruiter"),
        RECRUIT(0, "Recruit");

        private int rank;
        private String readableName;

        Rank(int rank, String readableName) {
            this.rank = rank;
            this.readableName = readableName;
        }
    }
}
