package dev.momostudios.coldsweat.config;

import dev.momostudios.coldsweat.util.compat.CompatManager;
import dev.momostudios.coldsweat.util.serialization.ListBuilder;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public final class ItemSettingsConfig
{
    private static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> boilerItems;
    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> iceboxItems;
    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> hearthItems;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> blacklistedPotions;
    private static final ForgeConfigSpec.BooleanValue allowPotionsInHearth;
    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> soulLampItems;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> soulLampDimensions;
    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> temperatureFoods;

    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> insulatingItems;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> insulationBlacklist;
    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> adaptiveInsulatingItems;
    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> insulatingArmor;
    private static final ForgeConfigSpec.ConfigValue<List<? extends Number>> insulationSlots;

    private static final ForgeConfigSpec.IntValue waterskinStrength;

    static final ItemSettingsConfig INSTANCE = new ItemSettingsConfig();

    static
    {
        /*
          Fuel Items
         */
        BUILDER.push("Fuel Items")
                .comment("Defines items that can be used as fuel",
                         "Format: [[\"item-id-1\", amount-1], [\"item-id-2\", amount-2], ...etc]");
        boilerItems = BUILDER
                .defineList("Boiler", ListBuilder.begin(
                                Arrays.asList("minecraft:coal",         37),
                                Arrays.asList("minecraft:charcoal",     37),
                                Arrays.asList("minecraft:coal_block",   333),
                                Arrays.asList("minecraft:magma_block",  333),
                                Arrays.asList("minecraft:lava_bucket",  1000)
                        ).build(),
                        it ->
                        {
                            if (it instanceof List<?>)
                            {   List<?> list = ((List<?>) it);
                                return list.size() == 2 && list.get(0) instanceof String && list.get(1) instanceof Number;
                            }
                            return false;
                        });

        iceboxItems = BUILDER
                .defineList("Icebox", ListBuilder.begin(
                                Arrays.asList("minecraft:snowball",     37),
                                Arrays.asList("minecraft:clay",         37),
                                Arrays.asList("minecraft:snow_block",   333),
                                Arrays.asList("minecraft:water_bucket", 333),
                                Arrays.asList("minecraft:ice",          333),
                                Arrays.asList("minecraft:packed_ice",   1000)
                        ).build(),
                        it ->
                        {
                            if (it instanceof List<?>)
                            {   List<?> list = ((List<?>) it);
                                return list.size() == 2 && list.get(0) instanceof String && list.get(1) instanceof Number;
                            }
                            return false;
                        });

        hearthItems = BUILDER
                .comment("Negative values indicate cold fuel")
                .defineList("Hearth", ListBuilder.begin(
                                // Hot
                                Arrays.asList("minecraft:coal",         37),
                                Arrays.asList("minecraft:charcoal",     37),
                                Arrays.asList("minecraft:coal_block",   333),
                                Arrays.asList("minecraft:magma_block",  333),
                                Arrays.asList("minecraft:lava_bucket",  1000),
                                // Cold
                                Arrays.asList("minecraft:snowball",     -37),
                                Arrays.asList("minecraft:clay",         -37),
                                Arrays.asList("minecraft:snow_block",   -333),
                                Arrays.asList("minecraft:water_bucket", -333),
                                Arrays.asList("minecraft:ice",          -333),
                                Arrays.asList("minecraft:packed_ice",   -1000)
                        ).build(),
                        it ->
                        {
                            if (it instanceof List<?>)
                            {   List<?> list = ((List<?>) it);
                                return list.size() == 2 && list.get(0) instanceof String && list.get(1) instanceof Number;
                            }
                            return false;
                        });
        blacklistedPotions = BUILDER
                .comment("Potions containing any of these effects will not be allowed in the hearth",
                         "Format: [\"effect_id\", \"effect_id\", ...etc]")
                .defineList("Blacklisted Potion Effects", ListBuilder.begin(
                                "minecraft:instant_damage",
                                "minecraft:poison",
                                "minecraft:wither",
                                "minecraft:weakness",
                                "minecraft:mining_fatigue",
                                "minecraft:slowness"
                        ).build(),
                        it -> it instanceof String);
        allowPotionsInHearth = BUILDER
                .comment("If true, potions can be used as fuel in the hearth",
                         "This gives all players in range the potion effect")
                .define("Allow Potions in Hearth", true);
        BUILDER.pop();

        /*
          Soulspring Lamp Items
         */
        BUILDER.push("Soulspring Lamp");
        soulLampItems = BUILDER
                .comment("Defines the items that the Soulspring Lamp can use as fuel and their values",
                        "Format: [\"item-id-1\", \"item-id-2\", ...etc]")
                .defineList("Fuel Items", ListBuilder.begin(
                                    Arrays.asList("cold_sweat:soul_sprout", 4)
                        ).build(),
                        it ->
                        {
                            if (it instanceof List<?>)
                            {   List<?> list = ((List<?>) it);
                                return list.size() == 2 && list.get(0) instanceof String && list.get(1) instanceof Number;
                            }
                            return false;
                        });

        soulLampDimensions = BUILDER
                .comment("Defines the dimensions that the Soulspring Lamp can be used in",
                        "Format: [\"dimension-id-1\", \"dimension-id-2\", ...etc]")
                .defineList("Valid Dimensions", ListBuilder.begin(
                                "minecraft:the_nether"
                        ).build(),
                        it -> it instanceof String);
        BUILDER.pop();

        /*
         Insulation
         */
        BUILDER.push("Insulation");
        insulatingItems = BUILDER
                .comment("Defines the items that can be used for insulating armor in the Sewing Table",
                        "Format: [[\"item_id\", cold, hot], [\"item_id\", cold, hot], ...etc]",
                        "\"item_id\": The item's ID (i.e. \"minecraft:iron_ingot\"). Accepts tags with \"#\" (i.e. \"#minecraft:wool\").",
                        "\"cold\": The amount of cold insulation the item provides.",
                        "\"hot\": The amount of heat insulation the item provides.")
                .defineList("Insulation Ingredients", ListBuilder.begin(
                                Arrays.asList("minecraft:leather_helmet",     4,  4),
                                Arrays.asList("minecraft:leather_chestplate", 6,  6),
                                Arrays.asList("minecraft:leather_leggings",   5,  5),
                                Arrays.asList("minecraft:leather_boots",      4,  4),
                                Arrays.asList("minecraft:leather",            1,  1),
                                Arrays.asList("cold_sweat:hoglin_hide",       0,  2),
                                Arrays.asList("cold_sweat:llama_fur",          2,  0),
                                Arrays.asList("#minecraft:wool",             1.5, 0),
                                Arrays.asList("minecraft:rabbit_hide",        0,  1.5),
                                Arrays.asList("cold_sweat:hoglin_headpiece",  0,  8),
                                Arrays.asList("cold_sweat:hoglin_tunic",      0,  12),
                                Arrays.asList("cold_sweat:hoglin_trousers",   0,  10),
                                Arrays.asList("cold_sweat:hoglin_hooves",     0,  8),
                                Arrays.asList("cold_sweat:llama_fur_cap",      8,  0),
                                Arrays.asList("cold_sweat:llama_fur_parka",    12, 0),
                                Arrays.asList("cold_sweat:llama_fur_pants",    10, 0),
                                Arrays.asList("cold_sweat:llama_fur_boots",    8,  0))
                            .addIf(CompatManager.isEnvironmentalLoaded(),
                                () -> Arrays.asList("environmental:yak_hair", 1.5, -1)
                        ).build(),
                        it ->
                        {
                            if (it instanceof List<?>)
                            {   List<?> list = ((List<?>) it);
                                return list.size() == 3 && list.get(0) instanceof String && list.get(1) instanceof Number && list.get(2) instanceof Number;
                            }
                            return false;
                        });

         adaptiveInsulatingItems = BUILDER
                .comment("Defines insulation items that have the special \"chameleon molt\" effect",
                         "Format: [[\"item_id\", insulation, adaptSpeed], [\"item_id\", insulation, adaptSpeed], ...etc]",
                        "\"item_id\": The item's ID (i.e. \"minecraft:iron_ingot\"). Accepts tags with \"#\" (i.e. \"#minecraft:wool\").",
                        "\"insulation\": The amount of insulation the item provides. Will adjust to hot/cold based on the environment.",
                        "\"adaptSpeed\": The speed at which the item adapts to the current temperature. Higher values mean faster adaptation (from 0 to 1).")
                .defineList("Adaptive Insulation Ingredients", ListBuilder.begin(
                                Arrays.asList("cold_sweat:chameleon_molt", 2, 0.0085)
                            ).build(),
                        it ->
                        {
                            if (it instanceof List<?>)
                            {   List<?> list = ((List<?>) it);
                                return list.size() == 3 && list.get(0) instanceof String && list.get(1) instanceof Number && list.get(2) instanceof Number;
                            }
                            return false;
                        });

        insulatingArmor = BUILDER
                .comment("Defines the items that provide insulation when worn",
                        "Format: [[\"item_id\", amount], [\"item_id\", amount], ...etc]")
                .defineList("Insulating Armor", ListBuilder.begin(
                                Arrays.asList("minecraft:leather_helmet",      4,  4),
                                Arrays.asList("minecraft:leather_chestplate",  6,  6),
                                Arrays.asList("minecraft:leather_leggings",    5,  5),
                                Arrays.asList("minecraft:leather_boots",       4,  4),
                                Arrays.asList("cold_sweat:hoglin_headpiece",   0,  8),
                                Arrays.asList("cold_sweat:hoglin_tunic",       0,  12),
                                Arrays.asList("cold_sweat:hoglin_trousers",    0,  10),
                                Arrays.asList("cold_sweat:hoglin_hooves",      0,  8),
                                Arrays.asList("cold_sweat:llama_fur_cap",       8,  0),
                                Arrays.asList("cold_sweat:llama_fur_parka",     12, 0),
                                Arrays.asList("cold_sweat:llama_fur_pants",     10, 0),
                                Arrays.asList("cold_sweat:llama_fur_boots",     8,  0))
                            .addIf(CompatManager.isEnvironmentalLoaded(),
                                () -> Arrays.asList("environmental:yak_pants", 7.5, -5)
                        ).build(),
                        it ->
                        {
                            if (it instanceof List<?>)
                            {   List<?> list = ((List<?>) it);
                                return list.size() == 3 && list.get(0) instanceof String && list.get(1) instanceof Number && list.get(2) instanceof Number;
                            }
                            return false;
                        });

        insulationSlots = BUILDER
                .comment("Defines how many insulation slots armor pieces have",
                         "Format: [head, chest, legs, feet]")
                .defineList("Insulation Slots", Arrays.asList(4, 6, 5, 4),
                        it -> it instanceof Number);

        insulationBlacklist = BUILDER
                .comment("Defines wearable items that cannot be insulated",
                        "Format: [\"item_id\", \"item_id\", ...etc]")
                .defineList("Insulation Blacklist", Arrays.asList(),
                        it -> it instanceof String);

        BUILDER.pop();

        /*
         Temperature-Affecting Foods
         */
        BUILDER.push("Consumables");
        temperatureFoods = BUILDER
                .comment("Defines items that affect the player's temperature when consumed",
                        "Format: [[\"item_id\", amount], [\"item_id\", amount], ...etc]",
                        "Negative values are cold foods, positive values are hot foods")
                .defineList("Temperature-Affecting Foods", Arrays.asList
                                (
                                        // nothing here
                                ),
                        it ->
                        {
                            if (it instanceof List<?>)
                            {   List<?> list = ((List<?>) it);
                                return list.size() == 2 && list.get(0) instanceof String && list.get(1) instanceof Number;
                            }
                            return false;
                        });
        waterskinStrength = BUILDER
                .comment("Defines the amount a player's body temperature will change by when using a waterskin")
                .defineInRange("Waterskin Strength", 50, 0, Integer.MAX_VALUE);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static void setup()
    {
        Path configPath = FMLPaths.CONFIGDIR.get();
        Path csConfigPath = Paths.get(configPath.toAbsolutePath().toString(), "coldsweat");

        // Create the config folder
        try
        {
            Files.createDirectory(csConfigPath);
        }
        catch (Exception ignored) {}

        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, SPEC, "coldsweat/item_settings.toml");
    }

    public static ItemSettingsConfig getInstance()
    {
        return INSTANCE;
    }

    public List<? extends List<?>> getBoilerFuelItems()
    {
        return boilerItems.get();
    }

    public List<? extends List<?>> getIceboxFuelItems()
    {
        return iceboxItems.get();
    }

    public List<? extends List<?>> getHearthFuelItems()
    {
        return hearthItems.get();
    }

    public List<? extends List<?>> getInsulationItems()
    {
        return insulatingItems.get();
    }

    public List<? extends List<?>> getAdaptiveInsulationItems()
    {
        return adaptiveInsulatingItems.get();
    }

    public List<? extends List<?>> getInsulatingArmorItems()
    {
        return insulatingArmor.get();
    }

    public List<? extends Number> getArmorInsulationSlots()
    {
        return insulationSlots.get();
    }

    public List<? extends String> getInsulationBlacklist()
    {   return insulationBlacklist.get();
    }

    public List<? extends List<?>> getSoulLampFuelItems()
    {
        return soulLampItems.get();
    }

    public List<? extends List<?>> getFoodTemperatures()
    {
        return temperatureFoods.get();
    }

    public List<? extends String> getValidSoulLampDimensions()
    {
        return soulLampDimensions.get();
    }

    public int getWaterskinStrength()
    {
        return waterskinStrength.get();
    }

    public boolean arePotionsEnabled()
    {   return allowPotionsInHearth.get();
    }

    public List<String> getPotionBlacklist()
    {   return (List<String>) blacklistedPotions.get();
    }

    public void setBoilerFuelItems(List<? extends List<?>> itemMap)
    {
        boilerItems.set(itemMap);
    }

    public void setIceboxFuelItems(List<? extends List<?>> itemMap)
    {
        iceboxItems.set(itemMap);
    }

    public void setHearthFuelItems(List<? extends List<?>> itemMap)
    {
        hearthItems.set(itemMap);
    }

    public void setInsulationItems(List<? extends List<?>> items)
    {
        insulatingItems.set(items);
    }

    public void setAdaptiveInsulationItems(List<? extends List<?>> items)
    {
        adaptiveInsulatingItems.set(items);
    }

    public void setInsulatingArmorItems(List<? extends List<?>> itemMap)
    {
        insulatingArmor.set(itemMap);
    }

    public void setArmorInsulationSlots(List<? extends Number> slots)
    {
        insulationSlots.set(slots);
    }

    public void setSoulLampFuelItems(List<? extends List<?>> items)
    {
        soulLampItems.set(items);
    }

    public void setFoodTemperatures(List<? extends List<?>> itemMap)
    {
        temperatureFoods.set(itemMap);
    }

    public void setValidSoulLampDimensions(List<? extends String> items)
    {
        soulLampDimensions.set(items);
    }

    public void setWaterskinStrength(int strength)
    {
        waterskinStrength.set(strength);
    }

    public void setPotionsEnabled(Boolean saver)
    {   allowPotionsInHearth.set(saver);
    }

    public void setPotionBlacklist(List<String> saver)
    {   blacklistedPotions.set(saver);
    }

    public void setInsulationBlacklist(List<String> blacklist)
    {   insulationBlacklist.set(blacklist);
    }
}
