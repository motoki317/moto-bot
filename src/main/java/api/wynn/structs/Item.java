package api.wynn.structs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@JsonIgnoreProperties(value = {"displayName", "skin"}, ignoreUnknown = true)
public class Item {
    private String name;
    private String tier;
    // Weapon and Armor only (category: "weapon", "armor")
    @Nullable
    private String type;
    // Accessory only (category: "accessory")
    @Nullable
    private String accessoryType;
    @Nullable
    private String set;
    private String restrictions;
    @Nullable
    private String material;
    @Nullable
    private String armorType;
    @Nullable
    private String armorColor;
    private String dropType;
    @Nullable
    private String addedLore;

    @Nullable
    private List<String> majorIds;

    private boolean identified;

    private int sockets;
    private int health;

    // damage string example: "0-0"
    // weapon only begin
    private String damage;

    private String earthDamage;
    private String thunderDamage;
    private String waterDamage;
    private String fireDamage;
    private String airDamage;

    private String attackSpeed;
    // weapon only end

    private int earthDefense;
    private int thunderDefense;
    private int waterDefense;
    private int fireDefense;
    private int airDefense;

    private int level;

    @Nullable
    private String quest;
    @Nullable
    private String classRequirement;

    // Skill point requirements
    private int strength;
    private int dexterity;
    private int intelligence;
    private int defense;
    private int agility;

    // Identifications begin
    private int strengthPoints;
    private int dexterityPoints;
    private int intelligencePoints;
    private int defensePoints;
    private int agilityPoints;

    private int damageBonus;
    private int damageBonusRaw;

    private int spellDamage;
    private int spellDamageRaw;

    private int rainbowSpellDamageRaw;

    private int healthRegen;
    private int healthRegenRaw;

    private int healthBonus;

    private int poison;

    private int lifeSteal;

    private int manaRegen;
    private int manaSteal;

    private int spellCostPct1;
    private int spellCostRaw1;
    private int spellCostPct2;
    private int spellCostRaw2;
    private int spellCostPct3;
    private int spellCostRaw3;
    private int spellCostPct4;
    private int spellCostRaw4;

    private int thorns;
    private int reflection;

    private int attackSpeedBonus;

    private int speed;

    private int exploding;

    private int soulPoints;

    private int sprint;
    private int sprintRegen;
    private int jumpHeight;

    private int xpBonus;
    private int lootBonus;
    private int lootQuality;
    private int emeraldStealing;

    private int gatherXpBonus;
    private int gatherSpeed;

    private int bonusEarthDamage;
    private int bonusThunderDamage;
    private int bonusWaterDamage;
    private int bonusFireDamage;
    private int bonusAirDamage;

    private int bonusEarthDefense;
    private int bonusThunderDefense;
    private int bonusWaterDefense;
    private int bonusFireDefense;
    private int bonusAirDefense;
    // Identifications end

    private String category;

    public String getName() {
        return name;
    }

    public String getTier() {
        return tier;
    }

    @Nullable
    public String getType() {
        return type;
    }

    @Nullable
    public String getAccessoryType() {
        return accessoryType;
    }

    @Nullable
    public String getSet() {
        return set;
    }

    public String getRestrictions() {
        return restrictions;
    }

    @Nullable
    public String getMaterial() {
        return material;
    }

    @Nullable
    public String getArmorType() {
        return armorType;
    }

    @Nullable
    public String getArmorColor() {
        return armorColor;
    }

    public String getDropType() {
        return dropType;
    }

    @Nullable
    public String getAddedLore() {
        return addedLore;
    }

    @Nullable
    public List<String> getMajorIds() {
        return majorIds;
    }

    public boolean isIdentified() {
        return identified;
    }

    public int getSockets() {
        return sockets;
    }

    public int getHealth() {
        return health;
    }

    public String getDamage() {
        return damage;
    }

    public String getEarthDamage() {
        return earthDamage;
    }

    public String getThunderDamage() {
        return thunderDamage;
    }

    public String getWaterDamage() {
        return waterDamage;
    }

    public String getFireDamage() {
        return fireDamage;
    }

    public String getAirDamage() {
        return airDamage;
    }

    public String getAttackSpeed() {
        return attackSpeed;
    }

    public int getEarthDefense() {
        return earthDefense;
    }

    public int getThunderDefense() {
        return thunderDefense;
    }

    public int getWaterDefense() {
        return waterDefense;
    }

    public int getFireDefense() {
        return fireDefense;
    }

    public int getAirDefense() {
        return airDefense;
    }

    public int getLevel() {
        return level;
    }

    @Nullable
    public String getQuest() {
        return quest;
    }

    @Nullable
    public String getClassRequirement() {
        return classRequirement;
    }

    public int getStrength() {
        return strength;
    }

    public int getDexterity() {
        return dexterity;
    }

    public int getIntelligence() {
        return intelligence;
    }

    public int getDefense() {
        return defense;
    }

    public int getAgility() {
        return agility;
    }

    public int getStrengthPoints() {
        return strengthPoints;
    }

    public int getDexterityPoints() {
        return dexterityPoints;
    }

    public int getIntelligencePoints() {
        return intelligencePoints;
    }

    public int getDefensePoints() {
        return defensePoints;
    }

    public int getAgilityPoints() {
        return agilityPoints;
    }

    public int getDamageBonus() {
        return damageBonus;
    }

    public int getDamageBonusRaw() {
        return damageBonusRaw;
    }

    public int getSpellDamage() {
        return spellDamage;
    }

    public int getSpellDamageRaw() {
        return spellDamageRaw;
    }

    public int getRainbowSpellDamageRaw() {
        return rainbowSpellDamageRaw;
    }

    public int getHealthRegen() {
        return healthRegen;
    }

    public int getHealthRegenRaw() {
        return healthRegenRaw;
    }

    public int getHealthBonus() {
        return healthBonus;
    }

    public int getPoison() {
        return poison;
    }

    public int getLifeSteal() {
        return lifeSteal;
    }

    public int getManaRegen() {
        return manaRegen;
    }

    public int getManaSteal() {
        return manaSteal;
    }

    public int getSpellCostPct1() {
        return spellCostPct1;
    }

    public int getSpellCostRaw1() {
        return spellCostRaw1;
    }

    public int getSpellCostPct2() {
        return spellCostPct2;
    }

    public int getSpellCostRaw2() {
        return spellCostRaw2;
    }

    public int getSpellCostPct3() {
        return spellCostPct3;
    }

    public int getSpellCostRaw3() {
        return spellCostRaw3;
    }

    public int getSpellCostPct4() {
        return spellCostPct4;
    }

    public int getSpellCostRaw4() {
        return spellCostRaw4;
    }

    public int getThorns() {
        return thorns;
    }

    public int getReflection() {
        return reflection;
    }

    public int getAttackSpeedBonus() {
        return attackSpeedBonus;
    }

    public int getSpeed() {
        return speed;
    }

    public int getExploding() {
        return exploding;
    }

    public int getSoulPoints() {
        return soulPoints;
    }

    public int getSprint() {
        return sprint;
    }

    public int getSprintRegen() {
        return sprintRegen;
    }

    public int getJumpHeight() {
        return jumpHeight;
    }

    public int getXpBonus() {
        return xpBonus;
    }

    public int getLootBonus() {
        return lootBonus;
    }

    public int getLootQuality() {
        return lootQuality;
    }

    public int getEmeraldStealing() {
        return emeraldStealing;
    }

    public int getGatherXpBonus() {
        return gatherXpBonus;
    }

    public int getGatherSpeed() {
        return gatherSpeed;
    }

    public int getBonusEarthDamage() {
        return bonusEarthDamage;
    }

    public int getBonusThunderDamage() {
        return bonusThunderDamage;
    }

    public int getBonusWaterDamage() {
        return bonusWaterDamage;
    }

    public int getBonusFireDamage() {
        return bonusFireDamage;
    }

    public int getBonusAirDamage() {
        return bonusAirDamage;
    }

    public int getBonusEarthDefense() {
        return bonusEarthDefense;
    }

    public int getBonusThunderDefense() {
        return bonusThunderDefense;
    }

    public int getBonusWaterDefense() {
        return bonusWaterDefense;
    }

    public int getBonusFireDefense() {
        return bonusFireDefense;
    }

    public int getBonusAirDefense() {
        return bonusAirDefense;
    }

    public String getCategory() {
        return category;
    }
}
