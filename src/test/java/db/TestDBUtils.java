package db;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import utils.TestUtils;

class TestDBUtils {
    @NotNull
    @Contract(" -> new")
    @TestOnly
    static Database createDatabase() {
        return new DatabaseImpl(TestUtils.getLogger());
    }
}
