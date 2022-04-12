package commands;

import app.Bot;
import commands.base.GuildCommand;
import commands.event.CommandEvent;
import db.model.serverLog.ServerLogEntry;
import db.repository.base.ServerLogRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class ServerLogCmd extends GuildCommand {
    private final ServerLogRepository serverLogRepository;

    public ServerLogCmd(Bot bot) {
        this.serverLogRepository = bot.getDatabase().getServerLogRepository();
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"serverLog"}};
    }

    @Override
    public @NotNull String[] slashName() {
        return new String[]{"serverlog"};
    }

    @Override
    public @NotNull OptionData[] slashOptions() {
        return new OptionData[]{};
    }

    @Override
    public @NotNull String syntax() {
        return "serverLog";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Sets the server log channel.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                        .setAuthor("Server Log Command Help")
                        .setDescription(this.shortHelp() + "\n" +
                                "The bot will send all notable actions in the server such as member join, leave to the channel. " +
                                "You are highly advised to set the log channel to a private channel where only server admins/moderators can see, " +
                                "because the bot will log **all** actions and messages it can see in the server.")
        ).build();
    }

    @NotNull
    @Override
    protected Permission[] getRequiredPermissions() {
        return new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(1);
    }

    @Override
    public void process(@NotNull CommandEvent event, @NotNull String[] args) {
        ServerLogEntry old = this.serverLogRepository.findOne(() -> event.getGuild().getIdLong());
        ServerLogEntry entry = new ServerLogEntry(event.getGuild().getIdLong(), event.getChannel().getIdLong());

        if (old != null && old.getChannelId() == entry.getChannelId()) {
            boolean res = this.serverLogRepository.delete(old);
            if (!res) {
                event.reply("Something went wrong while saving your data...");
                return;
            }
            event.reply("Successfully removed server log from this channel.");
            return;
        }

        boolean res = old == null
                ? this.serverLogRepository.create(entry)
                : this.serverLogRepository.update(entry);
        if (!res) {
            event.replyError("Something went wrong while saving your data...");
            return;
        }

        event.reply("Successfully set server log channel to here!");
    }
}
