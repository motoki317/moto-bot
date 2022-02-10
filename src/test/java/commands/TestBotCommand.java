package commands;

import commands.base.BotCommand;
import commands.event.CommandEvent;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

class TestBotCommand {
    @Test
    void testBotCommand() {
        BotCommand help = new BotCommand() {
            @Override
            public boolean guildOnly() {
                return false;
            }

            @NotNull
            @Override
            protected String[][] names() {
                return new String[][]{{"help", "h"}};
            }

            @Override
            public @NotNull String[] slashName() {
                return new String[]{"help"};
            }

            @Override
            public @NotNull OptionData[] slashOptions() {
                return new OptionData[]{};
            }

            @Override
            public @NotNull String syntax() {
                return "syntax";
            }

            @Override
            public @NotNull String shortHelp() {
                return "short help";
            }

            @Override
            public @NotNull Message longHelp() {
                return new MessageBuilder("test").build();
            }

            @Override
            public long getCoolDown() {
                return TimeUnit.SECONDS.toMillis(1);
            }

            @Override
            public void process(@NotNull CommandEvent event, @NotNull String[] args) {
            }
        };

        Set<String> helpNames = new HashSet<>();
        helpNames.add("help");
        helpNames.add("h");

        assert help.getNames().equals(helpNames);

        BotCommand levelRank = new BotCommand() {
            @Override
            public boolean guildOnly() {
                return false;
            }

            @NotNull
            @Override
            protected String[][] names() {
                return new String[][]{{"guild", "g"}, {"levelRank", "lRank", "lr"}};
            }

            @Override
            public @NotNull String[] slashName() {
                return new String[]{"g", "levelrank"};
            }

            @Override
            public @NotNull OptionData[] slashOptions() {
                return new OptionData[]{};
            }

            @Override
            public @NotNull String syntax() {
                return "syntax";
            }

            @Override
            public @NotNull String shortHelp() {
                return "short help";
            }

            @Override
            public @NotNull Message longHelp() {
                return new MessageBuilder("test").build();
            }

            @Override
            public long getCoolDown() {
                return TimeUnit.SECONDS.toMillis(1);
            }

            @Override
            public void process(@NotNull CommandEvent event, @NotNull String[] args) {
            }
        };

        Set<String> levelRankNames = new HashSet<>();
        levelRankNames.add("guild levelRank");
        levelRankNames.add("guild lRank");
        levelRankNames.add("guild lr");
        levelRankNames.add("g levelRank");
        levelRankNames.add("g lRank");
        levelRankNames.add("g lr");

        assert levelRank.getNames().equals(levelRankNames);
    }

    private static class TestClass {
        private int value;

        TestClass(int value) {
            this.value = value;
        }

        private Supplier<Integer> getSupplier() {
            return () -> this.value;
        }

        void setValue(int value) {
            this.value = value;
        }
    }

    @Test
    void testSupplierReference() {
        TestClass instance = new TestClass(0);
        Supplier<Integer> supplier = instance.getSupplier();
        assert supplier.get() == 0;
        instance.setValue(1);
        assert supplier.get() == 1;
    }
}
