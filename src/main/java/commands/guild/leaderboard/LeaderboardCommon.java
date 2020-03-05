package commands.guild.leaderboard;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.InputChecker;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class LeaderboardCommon {
    static class Range {
        @NotNull
        final Date start;
        @NotNull
        final Date end;

        Range(@NotNull Date start, @NotNull Date end) {
            this.start = start;
            this.end = end;
        }
    }

    @Nullable
    static Range parseRange(Map<String, String> parsedArgs) throws NumberFormatException {
        if (parsedArgs.containsKey("d") || parsedArgs.containsKey("-days")) {
            int days = InputChecker.getPositiveInteger(parsedArgs.get("d") != null
                    ? parsedArgs.get("d") : parsedArgs.get("-days"));
            if (days < 0 || days > 30) {
                throw new NumberFormatException("Please input an integer between 1 and 30 for days argument!");
            }

            Date now = new Date();
            Date old = new Date(now.getTime() - TimeUnit.DAYS.toMillis(days));
            return new Range(old, now);
        } else {
            return null;
        }
    }
}
