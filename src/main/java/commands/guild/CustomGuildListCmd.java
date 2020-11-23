package commands.guild;

import app.Bot;
import commands.base.GenericCommand;
import db.model.guild.Guild;
import db.model.guildList.GuildListEntry;
import db.repository.base.GuildListRepository;
import db.repository.base.GuildRepository;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomGuildListCmd extends GenericCommand {
    private final GuildListRepository guildListRepository;
    private final GuildNameResolver guildNameResolver;
    private final GuildPrefixesResolver guildPrefixesResolver;

    public CustomGuildListCmd(Bot bot) {
        GuildRepository guildRepository = bot.getDatabase().getGuildRepository();
        this.guildListRepository = bot.getDatabase().getGuildListRepository();
        this.guildNameResolver = new GuildNameResolver(bot.getResponseManager(), guildRepository);
        this.guildPrefixesResolver = new GuildPrefixesResolver(guildRepository);
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"g", "guild"}, {
            "customGuildList", "customList", "cl"
        }};
    }

    @Override
    public @NotNull String syntax() {
        return "g customGuildList";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Customize your own custom guild list. \">g customList help\" for more.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                String.join("\n", new String[]{
                        "This command allows you to make a custom list of guilds which you can use them in other commands.",
                        "Note that every custom list is saved per user.",
                        "Lists will be created or removed automatically when you add or remove guilds to / from the list.",
                        "Usage:",
                        "`>g customList` : Shows your custom lists.",
                        "`>g customList <list name>` : View the list.",
                        "`>g customList add <list name> <guild1>, <guild2>, ...` : Add guilds to the list.",
                        "`>g customList remove <list name> <guild1>, <guild2>, ...` : Remove guilds from your list.",
                        "`>g customList remove <list name>` : Removes the list."
                })
        ).build();
    }

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(1);
    }

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        if (args.length <= 2) {
            viewUserLists(event);
            return;
        }

        switch (args[2].toLowerCase()) {
            case "add":
                addToList(event, args);
                return;
            case "remove":
                removeFromList(event, args);
                return;
        }

        viewList(event, args[2]);
    }

    private void viewUserLists(MessageReceivedEvent event) {
        Map<String, Integer> lists = this.guildListRepository.getUserLists(event.getAuthor().getIdLong());
        if (lists == null) {
            respondError(event, "Something went wrong while retrieving data...");
            return;
        }
        if (lists.isEmpty()) {
            respond(event, "You do not have any custom guild lists configured. `>g customList help` for more.");
            return;
        }

        List<String> ret = new ArrayList<>();
        ret.add(String.format("Your have `%s` custom guild list%s.", lists.size(), lists.size() == 1 ? "" : "s"));
        ret.add("");
        for (Map.Entry<String, Integer> e : lists.entrySet()) {
            ret.add(String.format("`%s` : `%s` guild%s",
                    e.getKey(), e.getValue(), e.getValue() == 1 ? "" : "s"));
        }

        respond(event, String.join("\n", ret));
    }

    private void viewList(MessageReceivedEvent event, String listName) {
        List<GuildListEntry> list = this.guildListRepository.getList(event.getAuthor().getIdLong(), listName);
        if (list == null) {
            respondError(event, "Something went wrong while retrieving data...");
            return;
        }
        if (list.isEmpty()) {
            respond(event, String.format("You don't seem to have any guilds in a list named `%s`. " +
                    "Check your spelling or try adding guilds first.", listName));
            return;
        }

        List<String> ret = new ArrayList<>();
        ret.add(String.format("List: `%s` (%s guilds)", listName, list.size()));
        ret.add("");

        Map<String, String> prefixMap = this.guildPrefixesResolver.resolveGuildPrefixes(
                list.stream().map(GuildListEntry::getGuildName).collect(Collectors.toList())
        );
        for (GuildListEntry e : list) {
            ret.add(String.format("`[%s]` `%s`",
                    prefixMap.getOrDefault(e.getGuildName(), "???"),
                    e.getGuildName()));
        }

        respond(event, String.join("\n", ret));
    }

    private List<Guild> resolveGuilds(List<String> guildNames) {
        // For each input, resolve one or zero guild
        return guildNames.stream().flatMap(g -> {
            List<Guild> resolved = this.guildNameResolver.findGuilds(g);
            return resolved != null && resolved.size() == 1 ? Stream.of(resolved.get(0)) : Stream.empty();
        }).collect(Collectors.toList());
    }

    private void addToList(MessageReceivedEvent event, String[] args) {
        if (args.length <= 4) {
            respond(event, "Please specify a list name and at least one guild name to add.");
            return;
        }

        String listName = args[3];
        List<String> guildNames = Arrays.stream(String.join(" ", Arrays.copyOfRange(args, 4, args.length))
                .split(",")).map(String::trim).collect(Collectors.toList());
        List<Guild> guilds = resolveGuilds(guildNames);

        if (guilds.isEmpty()) {
            respond(event, "No guilds found with the given names. Make sure the guild exists and give the exact name(s).");
            return;
        }

        for (Guild g : guilds) {
            GuildListEntry e = new GuildListEntry(
                    event.getAuthor().getIdLong(),
                    listName,
                    g.getName()
            );
            if (this.guildListRepository.exists(e)) {
                continue;
            }

            boolean res = this.guildListRepository.create(e);
            if (!res) {
                respondError(event, "Something went wrong while saving data...");
                return;
            }
        }

        respond(event, String.format("Successfully added %s guild%s!\n%s",
                guilds.size(), guilds.size() == 1 ? "" : "s",
                guilds.stream().map(t -> "`" + t.getName() + "`").collect(Collectors.joining(", "))));
    }

    private void removeFromList(MessageReceivedEvent event, String[] args) {
        String listName = args[3];
        List<String> guildNames;
        if (args.length == 4) {
            // no guilds specified
            List<GuildListEntry> list = this.guildListRepository.getList(event.getAuthor().getIdLong(), listName);
            if (list == null) {
                respondError(event, "Something went wrong while retrieving data...");
                return;
            }
            if (list.isEmpty()) {
                respond(event, String.format("No guilds to remove for list `%s`.", listName));
                return;
            }

            guildNames = list.stream().map(GuildListEntry::getGuildName).collect(Collectors.toList());
        } else {
            // guilds to remove specified
            List<String> specifiedNames = Arrays.stream(String.join(" ", Arrays.copyOfRange(args, 4, args.length))
                    .split(",")).map(String::trim).collect(Collectors.toList());
            List<Guild> guilds = resolveGuilds(specifiedNames);

            if (guilds.isEmpty()) {
                respond(event, "No guilds found with the given names. Make sure the guild exists and give the exact name(s).");
                return;
            }

            guildNames = guilds.stream().map(Guild::getName).collect(Collectors.toList());
        }

        for (String guildName : guildNames) {
            GuildListEntry e = new GuildListEntry(
                    event.getAuthor().getIdLong(),
                    listName,
                    guildName
            );
            if (!this.guildListRepository.exists(e)) {
                continue;
            }

            boolean res = this.guildListRepository.delete(e);
            if (!res) {
                respondError(event, "Something went wrong while saving data...");
                return;
            }
        }

        respond(event, String.format("Successfully removed %s guild%s!\n%s",
                guildNames.size(), guildNames.size() == 1 ? "" : "s",
                guildNames.stream().map(t -> "`" + t + "`").collect(Collectors.joining(", "))));
    }
}
