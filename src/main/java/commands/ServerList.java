package commands;

import commands.base.GenericCommand;
import db.model.world.World;
import db.repository.WorldRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import update.multipage.MultipageHandler;
import update.reaction.ReactionManager;
import utils.FormatUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ServerList extends GenericCommand {
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    private static final int WORLDS_PER_PAGE_DEFAULT = 20;

    @NotNull
    private final WorldRepository repo;

    @NotNull
    private final ReactionManager reactionManager;

    public ServerList(@NotNull WorldRepository repo, @NotNull ReactionManager reactionManager) {
        this.repo = repo;
        this.reactionManager = reactionManager;
    }

    @Override
    public String[] names() {
        return new String[]{"serverlist", "servers", "up"};
    }

    @Override
    public String syntax() {
        return "serverlist [all]";
    }

    @Override
    public String shortHelp() {
        return "Returns a list of Wynncraft servers and their respective uptime.";
    }

    @Override
    public Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                .setAuthor("Server List Command Help")
                .setDescription("This command returns a list of Wynncraft servers and their respective uptime.")
                .addField("Syntax",
                        String.join("\n",
                                "`serverlist [all|<num>]`",
                                "`[all]` optional argument will, when specified, show all worlds excluding WAR worlds: " +
                                        "not only main WC/EU servers but also lobby, GM and other servers.",
                                "`[<num>]` optional argument will, when specified, set the max number of worlds to show per page." +
                                        " The default is " + WORLDS_PER_PAGE_DEFAULT + " worlds per page."),
                        false)
                .build()
        ).build();
    }

    @Override
    public void process(MessageReceivedEvent event, String[] args) {
        boolean all = false;
        int worldsPerPage = WORLDS_PER_PAGE_DEFAULT;
        if (args.length > 1) {
            for (String arg : args) {
                if ("all".equals(arg.toLowerCase())) {
                    all = true;
                } else if (arg.matches("\\d+")) {
                    worldsPerPage = Integer.parseInt(arg);
                }
            }
        }

        if (worldsPerPage < 1 || WORLDS_PER_PAGE_DEFAULT * 2 < worldsPerPage) {
            respond(event, "Invalid number specified for worlds per page argument.\n" +
                    "Please specify a number between 1 and " + (WORLDS_PER_PAGE_DEFAULT * 2) + ".");
            return;
        }

        List<World> worlds;
        if (all) {
            List<World> allWorlds = repo.findAll();
            if (allWorlds == null) {
                this.respondError(event, "Something went wrong while retrieving data.");
                return;
            }
            worlds = allWorlds.stream().filter(w -> !w.getName().startsWith("WAR")).collect(Collectors.toList());
        } else {
            worlds = repo.findAllMainWorlds();
            if (worlds == null) {
                this.respondError(event, "Something went wrong while retrieving data.");
                return;
            }
        }

        if (worlds.isEmpty()) {
            this.respond(event, "There doesn't seem to be any " + (all ? "" : "main ") + "worlds online...");
            return;
        }

        Function<Integer, Message> pages = pageSupplier(worlds, worldsPerPage);
        int maxPage = maxPage(worlds.size(), worldsPerPage);

        if (maxPage == 0) {
            this.respond(event, pages.apply(0));
            return;
        }

        respond(event, pages.apply(0), message -> {
            MultipageHandler handler = new MultipageHandler(message, pages, () -> maxPage);
            this.reactionManager.addEventListener(handler);
        });
    }

    private static Function<Integer, Message> pageSupplier(@NotNull List<World> worlds, int worldsPerPage) {
        return (page) -> {
            int min = page * worldsPerPage;
            int max = Math.min((page + 1) * worldsPerPage, worlds.size());
            return new MessageBuilder(format(worlds, min, max)).build();
        };
    }

    private static int maxPage(int worldsSize, int worldsPerPage) {
        return (worldsSize - 1) / worldsPerPage;
    }

    @NotNull
    private static String format(@NotNull List<World> worlds, int min, int max) {
        List<String> ret = new ArrayList<>();
        ret.add("```ml");
        ret.add("---- Server List ----");
        ret.add("");

        class WorldDisplay {
            private int num;
            private String name;
            private int players;
            private String uptime;

            private WorldDisplay(int num, String name, int players, String uptime) {
                this.num = num;
                this.name = name;
                this.players = players;
                this.uptime = uptime;
            }
        }

        worlds.sort((w1, w2) -> (int) (w2.getCreatedAt().getTime() - w1.getCreatedAt().getTime()));

        long currentTimeMillis = System.currentTimeMillis();

        List<WorldDisplay> displays = new ArrayList<>();
        for (int i = min; i < max; i++) {
            World w = worlds.get(i);
            int num = i + 1;
            long uptimeSeconds = (currentTimeMillis - w.getCreatedAt().getTime()) / 1000L;
            String uptime = FormatUtils.formatReadableTime(uptimeSeconds, true, "m");

            displays.add(new WorldDisplay(num, w.getName(), w.getPlayers(), uptime));
        }

        int numLongest = 1;
        int serverNameLongest = 6;
        int playersLongest = 7;
        int uptimeLongest = 6;
        for (WorldDisplay display : displays) {
            numLongest = Math.max(numLongest, String.valueOf(display.num).length());
            serverNameLongest = Math.max(serverNameLongest, display.name.length());
            playersLongest = Math.max(playersLongest, String.valueOf(display.players).length());
            uptimeLongest = Math.max(uptimeLongest, display.uptime.length());
        }

        ret.add(nSpaces(numLongest + 2) + "Server" + nSpaces(serverNameLongest - 6) +
                " | " + "Players" + nSpaces(playersLongest - 7) +
                " | " + "Uptime");
        ret.add(nCopies("-", numLongest + 2 + serverNameLongest) +
                "-+-" + nCopies("-", playersLongest) +
                "-+-" + nCopies("-", uptimeLongest + 1));

        for (WorldDisplay display : displays) {
            String toAdd = String.format("%-" + (numLongest + 1) + "s", display.num + ".");
            toAdd += " ";
            toAdd += String.format("%-" + serverNameLongest + "s", display.name);
            toAdd += " | ";
            toAdd += String.format("%-" + playersLongest + "s", display.players);
            toAdd += " | ";
            toAdd += String.format("%" + uptimeLongest + "s", display.uptime);

            ret.add(toAdd);
        }

        ret.add("");

        Date lastUpdate = new Date(worlds.get(0).getUpdatedAt().getTime());
        ret.add("last update: " + dateFormat.format(lastUpdate));

        ret.add("```");
        return String.join("\n", ret);
    }

    private static String nSpaces(int n) {
        return String.join("", Collections.nCopies(n, " "));
    }

    private static String nCopies(String s, int n) {
        return String.join("", Collections.nCopies(n, s));
    }
}