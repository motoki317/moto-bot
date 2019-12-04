package commands.base;

public abstract class GenericCommand extends BotCommand {
    @Override
    public boolean guildOnly() {
        return false;
    }
}
