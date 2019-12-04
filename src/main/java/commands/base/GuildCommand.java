package commands.base;

public abstract class GuildCommand extends BotCommand {
    public boolean guildOnly() {
        return true;
    }
}
