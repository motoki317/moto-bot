package commands;

import api.wynn.WynnApi;
import api.wynn.structs.Item;
import api.wynn.structs.ItemDB;
import app.Bot;
import commands.base.GenericCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ItemView extends GenericCommand {
    private final WynnApi wynnApi;
    private final String imageURLBase;

    public ItemView(Bot bot) {
        this.wynnApi = new WynnApi(bot.getLogger(), bot.getProperties().wynnTimeZone);
        this.imageURLBase = bot.getProperties().githubImagesUrl;
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"i", "item"}};
    }

    @Override
    public @NotNull String syntax() {
        return "item <item name>";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Shows item stats.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                .setAuthor("Item Stats Command Help")
                .setDescription("Shows an item's stats. Give partial or full item name to the argument.")
                .build()
        ).build();
    }

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        if (args.length < 2) {
            respond(event, "Please input the item name you want to view stats of.");
            return;
        }

        ItemDB db = this.wynnApi.mustGetItemDB(false);
        if (db == null) {
            respondError(event, "Something went wrong while requesting Wynncraft API.");
            return;
        }

        String input = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        List<Item> matched = searchItem(input, db.getItems());

        if (matched.size() == 0) {
            respond(event, String.format("No items matched with input `%s`.", input));
            return;
        } else if (matched.size() > 1) {
            respond(event, String.format("Multiple items (%s items) matched with input `%s`.\nMatched items: %s",
                    matched.size(), input,
                    matched.stream().limit(50).map(i -> "`" + i.getName() + "`")
                            .collect(Collectors.joining(", "))));
            return;
        }

        Item item = matched.get(0);

        respond(event, formatItemInfo(item, this.imageURLBase));
    }

    static List<Item> searchItem(String input, List<Item> items) {
        // case insensitive search
        input = input.toLowerCase();

        List<Item> prefixMatch = new ArrayList<>();
        List<Item> partialMatch = new ArrayList<>();
        for (Item item : items) {
            String itemName = item.getName().toLowerCase();
            // Exact match
            if (input.equals(itemName)) {
                return new ArrayList<>(Collections.singletonList(item));
            }
            // Prefix match
            if (itemName.startsWith(input)) {
                prefixMatch.add(item);
                partialMatch.add(item);
            } else if (itemName.contains(input)) {
                // Partial match
                partialMatch.add(item);
            }
        }

        if (prefixMatch.size() == 1) {
            return prefixMatch;
        }
        if (prefixMatch.size() > 1) {
            return prefixMatch;
        }
        // No prefix match
        return partialMatch;
    }

    private static Message formatItemInfo(Item item, String imageURLBase) {
        return new MessageBuilder(
                "```ml\n" +
                        getIDs(item) +
                        "\n```"
        ).setEmbed(
                getEmbed(item, imageURLBase).build()
        ).build();
    }

    static EmbedBuilder getEmbed(Item item, String imageURLBase) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(String.format("Lv. %s %s, %s %s",
                item.getLevel(), item.getName(), item.getTier(),
                "accessory".equals(item.getCategory()) ? item.getAccessoryType() : item.getType()));

        eb.setThumbnail(imageURLBase + getImageName(item));

        Color col = getTierColor(item.getTier());
        if (col != null) {
            eb.setColor(col);
        }

        eb.addField("Base Status", getBaseStatus(item), true);
        eb.addField("Requirements", getRequirements(item), true);
        eb.addField("Misc.", getMiscStatus(item), true);

        if (item.getAddedLore() != null) {
            eb.addField("Lore", item.getAddedLore(), false);
        }

        if ("weapon".equals(item.getCategory())) {
            eb.addField("DPS", getDPS(item), false);
        }

        return eb;
    }

    private static class Status {
        Function<Item, Integer> status;
        String displayName;

        private Status(Function<Item, Integer> status, String displayName) {
            this.status = status;
            this.displayName = displayName;
        }
    }

    private static class Damage {
        private Function<Item, String> status;
        private String displayName;

        private Damage(Function<Item, String> status, String displayName) {
            this.status = status;
            this.displayName = displayName;
        }

        private double getAverageDamage(Item item) {
            // Damage example "0-0"
            String[] status = this.status.apply(item).split("-");
            double lower = Double.parseDouble(status[0]);
            double higher = Double.parseDouble(status[1]);
            return (lower + higher) / 2;
        }
    }

    private static final Status[] armorStatuses = {
            new Status(Item::getHealth, "Health"),
            new Status(Item::getEarthDefense, "Earth Defense"),
            new Status(Item::getThunderDefense, "Thunder Defense"),
            new Status(Item::getWaterDefense, "Water Defense"),
            new Status(Item::getFireDefense, "Fire Defense"),
            new Status(Item::getAirDefense, "Air Defense")
    };

    private static final Damage[] weaponStatuses = {
            new Damage(Item::getDamage, "Neutral Damage"),
            new Damage(Item::getEarthDamage, "Earth Damage"),
            new Damage(Item::getThunderDamage, "Thunder Damage"),
            new Damage(Item::getWaterDamage, "Water Damage"),
            new Damage(Item::getFireDamage, "Fire Damage"),
            new Damage(Item::getAirDamage, "Air Damage")
    };

    private enum AttackSpeed {
        SUPER_SLOW(0.51),
        VERY_SLOW(0.83),
        SLOW(1.5),
        NORMAL(2.05),
        FAST(2.5),
        VERY_FAST(3.1),
        SUPER_FAST(4.3);

        private double multiplier;

        AttackSpeed(double multiplier) {
            this.multiplier = multiplier;
        }
    }

    private static String getBaseStatus(Item item) {
        List<String> ret = new ArrayList<>();

        switch (item.getCategory()) {
            case "weapon":
                for (Damage status : weaponStatuses) {
                    String dmg = status.status.apply(item);
                    if ("0-0".equals(dmg) || dmg.isEmpty()) {
                        continue;
                    }
                    ret.add(String.format("%s : %s", status.displayName, dmg));
                }

                AttackSpeed speed = AttackSpeed.valueOf(item.getAttackSpeed());
                ret.add(String.format("Attack Speed : %s (%s)", speed.name(), speed.multiplier));
                break;
            case "armor":
            case "accessory":
                for (Status status : armorStatuses) {
                    int val = status.status.apply(item);
                    if (val == 0) {
                        continue;
                    }
                    ret.add(String.format("%s : %s", status.displayName, val));
                }
                break;
        }

        return String.join("\n", ret);
    }

    private static String getDPS(Item item) {
        List<String> ret = new ArrayList<>();

        AttackSpeed speed = AttackSpeed.valueOf(item.getAttackSpeed());

        double total = 0;
        for (Damage status : weaponStatuses) {
            String dmg = status.status.apply(item);
            if ("0-0".equals(dmg) || dmg.isEmpty()) {
                continue;
            }
            double dps = speed.multiplier * status.getAverageDamage(item);
            total += dps;
            ret.add(String.format("%s : %.2f", status.displayName, dps));
        }

        ret.add("");
        ret.add(String.format("Total : %.2f", total));

        return String.join("\n", ret);
    }

    private static final Status[] skillPointRequirements = {
            new Status(Item::getStrength, "Strength Min."),
            new Status(Item::getDexterity, "Dexterity Min."),
            new Status(Item::getIntelligence, "Intelligence Min."),
            new Status(Item::getDefense, "Defense Min."),
            new Status(Item::getAgility, "Agility Min.")
    };

    private static String getRequirements(Item item) {
        List<String> ret = new ArrayList<>();

        ret.add("Level : " + item.getLevel());

        if (item.getQuest() != null) {
            ret.add("Quest : " + item.getQuest());
        }
        if (item.getClassRequirement() != null) {
            ret.add("Class Req. : " + item.getClassRequirement());
        }

        for (Status req : skillPointRequirements) {
            int val = req.status.apply(item);
            if (val == 0) {
                continue;
            }
            ret.add(String.format("%s : %s", req.displayName, val));
        }

        return String.join("\n", ret);
    }

    private static String getMiscStatus(Item item) {
        List<String> ret = new ArrayList<>();

        ret.add(String.format("Drop type : %s", item.getDropType()));
        if (item.getSockets() != 0) {
            ret.add(String.format("Sockets : %s", item.getSockets()));
        }
        if (item.getRestrictions() != null) {
            ret.add(String.format("Restrictions : %s", item.getRestrictions()));
        }
        if (item.isIdentified()) {
            ret.add("Pre-identified item");
        }

        return String.join("\n", ret);
    }

    static class Identification extends Status {
        private String suffix;
        private boolean identified;

        private Identification(Function<Item, Integer> status, String displayName, String suffix, boolean identified) {
            super(status, displayName);
            this.suffix = suffix;
            this.identified = identified;
        }

        private String getFormattedID(Item item) {
            int val = this.status.apply(item);
            if (identified || item.isIdentified()) {
                return val + this.suffix;
            }

            // show value range
            int lo, hi;
            if (val > 0) {
                lo = (int) Math.round((double) val * 0.3d);
                hi = (int) Math.round((double) val * 1.3d);
            } else {
                lo = (int) Math.round((double) val * 1.3d);
                hi = (int) Math.round((double) val * 0.7d);
            }

            return String.format("%s%s, %s%s ~ %s%s", val, this.suffix, lo, this.suffix, hi, this.suffix);
        }

        boolean isIdentified() {
            return this.identified;
        }

        String getSuffix() {
            return this.suffix;
        }
    }

    // Identifications, their display name, and suffix
    static final Identification[] identifications = {
            // Bonus skill points
            new Identification(Item::getStrengthPoints, "Strength", "", true),
            new Identification(Item::getDexterityPoints, "Dexterity", "", true),
            new Identification(Item::getIntelligencePoints, "Intelligence", "", true),
            new Identification(Item::getDefensePoints, "Defense", "", true),
            new Identification(Item::getAgilityPoints, "Agility", "", true),
            // Melee
            new Identification(Item::getDamageBonus, "Melee Damage", "%", false),
            new Identification(Item::getDamageBonusRaw, "Melee Damage Raw", "", false),
            // Spell
            new Identification(Item::getSpellDamage, "Spell Damage", "%", false),
            new Identification(Item::getSpellDamageRaw, "Spell Damage Raw", "", false),
            // Rainbow
            new Identification(Item::getRainbowSpellDamageRaw, "Rainbow Spell Damage Raw", "", false),
            // HP Regen
            new Identification(Item::getHealthRegen, "Health Regen", "%", false),
            new Identification(Item::getHealthRegenRaw, "Health Regen Raw", "", false),
            // HP
            new Identification(Item::getHealthBonus, "Health", "", false),
            // Poison
            new Identification(Item::getPoison, "Poison", "/3s", false),
            // Life steal
            new Identification(Item::getLifeSteal, "Life Steal", "/4s", false),
            // Mana
            new Identification(Item::getManaRegen, "Mana Regen", "/4s", false),
            new Identification(Item::getManaSteal, "Mana Steal", "/4s", false),
            // Spell cost
            new Identification(Item::getSpellCostPct1, "1st Spell Cost", "%", false),
            new Identification(Item::getSpellCostRaw1, "1st Spell Cost", "", false),
            new Identification(Item::getSpellCostPct2, "2nd Spell Cost", "%", false),
            new Identification(Item::getSpellCostRaw2, "2nd Spell Cost", "", false),
            new Identification(Item::getSpellCostPct3, "3rd Spell Cost", "%", false),
            new Identification(Item::getSpellCostRaw3, "3rd Spell Cost", "", false),
            new Identification(Item::getSpellCostPct4, "4th Spell Cost", "%", false),
            new Identification(Item::getSpellCostRaw4, "4th Spell Cost", "", false),
            // Thorns and Reflection
            new Identification(Item::getThorns, "Thorns", "%", false),
            new Identification(Item::getReflection, "Reflection", "%", false),
            // Attack speed
            new Identification(Item::getAttackSpeedBonus, "Attack Speed Bonus", " tier", false),
            // Walk speed
            new Identification(Item::getSpeed, "Speed", "%", false),
            // Exploding
            new Identification(Item::getExploding, "Exploding", "%", false),
            // Soul point regen
            new Identification(Item::getSoulPoints, "Soul Point Regen", "%", false),
            // Sprint and Jump
            new Identification(Item::getSprint, "Sprint", "%", false),
            new Identification(Item::getSprintRegen, "Sprint Regen", "%", false),
            new Identification(Item::getJumpHeight, "Jump Height", "", false),
            // XP and Loot
            new Identification(Item::getXpBonus, "XP Bonus", "%", false),
            new Identification(Item::getLootBonus, "Loot Bonus", "%", false),
            new Identification(Item::getLootQuality, "Loot Quality", "%", false),
            new Identification(Item::getEmeraldStealing, "Emerald Stealing", "%", false),
            // Gathering
            new Identification(Item::getGatherXpBonus, "Gather XP Bonus", "%", false),
            new Identification(Item::getGatherSpeed, "Gather Speed", "%", false),
            // Bonus elemental damage
            new Identification(Item::getBonusEarthDamage, "Earth Damage", "%", false),
            new Identification(Item::getBonusThunderDamage, "Thunder Damage", "%", false),
            new Identification(Item::getBonusWaterDamage, "Water Damage", "%", false),
            new Identification(Item::getBonusFireDamage, "Fire Damage", "%", false),
            new Identification(Item::getBonusAirDamage, "Air Damage", "%", false),
            // Bonus elemental defense
            new Identification(Item::getBonusEarthDefense, "Earth Defense", "%", false),
            new Identification(Item::getBonusThunderDefense, "Thunder Defense", "%", false),
            new Identification(Item::getBonusWaterDefense, "Water Defense", "%", false),
            new Identification(Item::getBonusFireDefense, "Fire Defense", "%", false),
            new Identification(Item::getBonusAirDefense, "Air Defense", "%", false)
    };

    private static String getIDs(Item item) {
        List<Identification> availableIDs = Arrays.stream(identifications)
                .filter(i -> i.status.apply(item) != 0).collect(Collectors.toList());

        List<String> ret = new ArrayList<>();
        if (availableIDs.isEmpty()) {
            return "No Identifications";
        }

        int displayJustify = availableIDs.stream().mapToInt(i -> i.displayName.length()).max().getAsInt() + 1;
        for (Identification id : availableIDs) {
            ret.add(String.format("%s%s : %s",
                    nSpaces(displayJustify - id.displayName.length()), id.displayName,
                    id.getFormattedID(item)));
        }

        return String.join("\n", ret);
    }

    private static final Set<String> gifImages = new HashSet<>(Arrays.asList("27", "28", "160-14"));

    /**
     * Retrieves image name such as "160-14.gif"
     * @param item Item
     * @return Image name
     */
    private static String getImageName(Item item) {
        String id = getImageID(item);
        if (gifImages.contains(id)) {
            return id + ".gif";
        }
        return id + ".png";
    }

    /**
     * Retrieves image name such as "160-14"
     * @param item Item
     * @return Image ID
     */
    private static String getImageID(Item item) {
        // Expected values like "1" or "161:1"
        String material = item.getMaterial();
        if (material != null) {
            if (material.matches("\\d+:\\d+")) {
                String[] sp = material.split(":");
                if (sp[1].equals("0")) {
                    return sp[0];
                }
                return sp[0] + "-" + sp[1];
            } else {
                return material;
            }
        }

        // Get ID from armor type
        if (item.getType() != null && item.getArmorType() != null) {
            return getArmorID(item.getType(), item.getArmorType()) + "-0";
        }

        return "0";
    }

    /**
     * Retrieves item ID from type and armor type
     * @param type Type such as "Helmet"
     * @param armorType Armor type such as "Leather"
     * @return Numeric item ID such as "298"
     */
    private static int getArmorID(String type, String armorType) {
        int offset = 0;
        switch (type) {
            case "Helmet":
                offset = 0;
                break;
            case "Chestplate":
                offset = 1;
                break;
            case "Leggings":
                offset = 2;
                break;
            case "Boots":
                offset = 3;
                break;
        }

        switch (armorType) {
            case "Leather":
                return 298 + offset;
            case "Chain":
                return 302 + offset;
            case "Iron":
                return 306 + offset;
            case "Diamond":
                return 310 + offset;
            case "Golden":
                return 314 + offset;
        }

        return 0;
    }

    /**
     * Retrieves corresponding theme color from item tier.
     * @param tier Item tier such as "Mythic"
     * @return Color
     */
    @Nullable
    private static Color getTierColor(String tier) {
        switch (tier) {
            case "Unique":
                return new Color(255, 255, 85);
            case "Rare":
                return new Color(255, 85, 255);
            case "Legendary":
                return new Color(85, 255, 255);
            case "Mythic":
                return new Color(170, 0, 170);
            case "Set":
                return new Color(0, 170, 0);
        }
        return null;
    }

    private static String nSpaces(int n) {
        return String.join("", Collections.nCopies(n, " "));
    }
}
