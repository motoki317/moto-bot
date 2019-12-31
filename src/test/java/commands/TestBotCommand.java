package commands;

import commands.base.BotCommand;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
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
            public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {}
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
                return new String[][]{{"guild", "g"}, {"levelrank", "lrank"}};
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
            public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {}
        };

        Set<String> levelRankNames = new HashSet<>();
        levelRankNames.add("guild levelrank");
        levelRankNames.add("guild lrank");
        levelRankNames.add("g levelrank");
        levelRankNames.add("g lrank");

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
        TestClass i = new TestClass(0);
        Supplier<Integer> supplier = i.getSupplier();
        assert supplier.get() == 0;
        i.setValue(1);
        assert supplier.get() == 1;
    }
}
