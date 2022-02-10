package commands;

import commands.base.GenericCommand;
import commands.event.CommandEvent;
import db.model.ignoreChannel.IgnoreChannel;
import db.repository.base.IgnoreChannelRepository;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class Ignore extends GenericCommand {
    private final IgnoreChannelRepository ignoreChannelRepository;

    public Ignore(IgnoreChannelRepository ignoreChannelRepository) {
        this.ignoreChannelRepository = ignoreChannelRepository;
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"ignore"}};
    }

    @Override
    public @NotNull String[] slashName() {
        return new String[]{"ignore"};
    }

    @Override
    public @NotNull OptionData[] slashOptions() {
        return new OptionData[]{};
    }

    @Override
    public @NotNull String syntax() {
        return "ignore";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Make the bot ignore messages sent in a channel. (Has no effect on slash commands!)";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(this.shortHelp()).build();
    }

    @NotNull
    @Override
    protected Permission[] getRequiredPermissions() {
        return new Permission[]{Permission.MANAGE_CHANNEL};
    }

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(1);
    }

    @Override
    public void process(@NotNull CommandEvent event, @NotNull String[] args) {
        long channelId = event.getChannel().getIdLong();

        boolean isIgnored = this.ignoreChannelRepository.exists(() -> channelId);
        if (isIgnored) {
            boolean res = this.ignoreChannelRepository.delete(() -> channelId);
            if (res) {
                event.reply(":open_mouth: The bot will now respond to messages in this channel.");
            } else {
                event.replyError("Something went wrong while saving data...");
            }
        } else {
            boolean res = this.ignoreChannelRepository.create(new IgnoreChannel(channelId));
            if (res) {
                event.reply(":no_mouth: The bot will no longer respond to messages in this channel. " +
                        "Type in the same command (`ignore`) to un-ignore this channel.");
            } else {
                event.replyError("Something went wrong while saving data...");
            }
        }
    }
}
