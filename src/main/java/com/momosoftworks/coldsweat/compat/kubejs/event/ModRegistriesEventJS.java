package com.momosoftworks.coldsweat.compat.kubejs.event;

import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.registry.BlockTempRegistry;
import com.momosoftworks.coldsweat.api.registry.TempModifierRegistry;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.compat.kubejs.event.builder.*;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.config.type.CarriedItemTemperature;
import com.momosoftworks.coldsweat.config.type.Insulator;
import com.momosoftworks.coldsweat.config.type.PredicateItem;
import com.momosoftworks.coldsweat.util.serialization.DynamicHolder;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import dev.latvian.mods.kubejs.event.KubeStartupEvent;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import oshi.util.tuples.Triplet;

import java.util.function.Consumer;
import java.util.function.Function;

public class ModRegistriesEventJS implements KubeStartupEvent
{
    /*
     Block Temperature
     */

    public void addBlockTemperature(Consumer<BlockTempBuilderJS> builder, BlockTempBuilderJS.Function function)
    {
        BlockTempBuilderJS blockTempJS = new BlockTempBuilderJS();
        builder.accept(blockTempJS);
        BlockTempRegistry.register(blockTempJS.build(function));
        ColdSweat.LOGGER.info("Registered KubeJS block temperature for blocks: {}", blockTempJS.blocks);
    }

    public void addBlockTemperature(double temperature, String units, Consumer<BlockTempBuilderJS> builder)
    {
        double convertedTemp = Temperature.convert(temperature, Temperature.Units.fromID(units), Temperature.Units.MC, false);
        addBlockTemperature(builder, (level, entity, state, pos, distance) -> convertedTemp);
    }

    public void addBlockTemperature(double temperature, Consumer<BlockTempBuilderJS> builder)
    {
        addBlockTemperature(temperature, "mc", builder);
    }

    /*
     Insulator
     */

    public void addInsulator(Consumer<InsulatorBuilderJS> builder)
    {
        InsulatorBuilderJS insulatorJS = new InsulatorBuilderJS();
        builder.accept(insulatorJS);
        Insulator insulator = insulatorJS.build();

        Multimap<Item, Insulator> map = switch (insulatorJS.slot)
        {
            case ITEM -> ConfigSettings.INSULATION_ITEMS.get();
            case ARMOR -> ConfigSettings.INSULATING_ARMORS.get();
            case CURIO -> ConfigSettings.INSULATING_CURIOS.get();
        };
        for (Item item : insulatorJS.items)
        {   map.put(item, insulator);
        }
        ColdSweat.LOGGER.info("Registered KubeJS insulator for items: {}", insulatorJS.items);
    }

    /*
     Food Temperature
     */

    public void addFoodTemperature(Consumer<FoodBuilderJS> builder)
    {
        FoodBuilderJS foodJS = new FoodBuilderJS();
        builder.accept(foodJS);
        PredicateItem food = foodJS.build();

        for (Item item : foodJS.items)
        {   ConfigSettings.FOOD_TEMPERATURES.get().put(item, food);
        }
    }

    /*
     Fuel
     */

    private void addFuel(Consumer<FuelBuilderJS> builder, DynamicHolder<Multimap<Item, PredicateItem>> config)
    {
        FuelBuilderJS fuelJS = new FuelBuilderJS();
        builder.accept(fuelJS);
        PredicateItem fuel = fuelJS.build();

        for (Item item : fuelJS.items)
        {   config.get().put(item, fuel);
        }
    }

    public void addHearthFuel(Consumer<FuelBuilderJS> builder)
    {   addFuel(builder, ConfigSettings.HEARTH_FUEL);
    }

    public void addBoilerFuel(Consumer<FuelBuilderJS> builder)
    {   addFuel(builder, ConfigSettings.BOILER_FUEL);
    }

    public void addIceboxFuel(Consumer<FuelBuilderJS> builder)
    {   addFuel(builder, ConfigSettings.ICEBOX_FUEL);
    }

    /*
     Carried Item Temperature
     */

    public void addCarriedItemTemperature(Consumer<CarriedItemBuilderJS> builder)
    {
        CarriedItemBuilderJS carriedItemJS = new CarriedItemBuilderJS();
        builder.accept(carriedItemJS);
        CarriedItemTemperature carriedItem = carriedItemJS.build();

        for (Item item : carriedItemJS.items)
        {   ConfigSettings.CARRIED_ITEM_TEMPERATURES.get().put(item, carriedItem);
        }
    }

    /*
     Biome Temperature
     */

    public void addBiomeTemperature(String biomeId, double minTemp, double maxTemp, String units)
    {
        RegistryAccess registryAccess = RegistryHelper.getRegistryAccess();
        Biome biome = RegistryHelper.getBiome(ResourceLocation.parse(biomeId), registryAccess);
        if (biome == null)
        {   ColdSweat.LOGGER.error("Failed to find biome with ID: {}", biomeId);
            return;
        }
        ConfigSettings.BIOME_TEMPS.get().put(biome, new Triplet<>(minTemp, maxTemp, Temperature.Units.fromID(units)));
    }

    public void addBiomeTemperature(String biomeId, double minTemp, double maxTemp)
    {   addBiomeTemperature(biomeId, minTemp, maxTemp, "mc");
    }

    public void addBiomeOffset(String biomeId, double minTemp, double maxTemp, String units)
    {
        RegistryAccess registryAccess = RegistryHelper.getRegistryAccess();
        Biome biome = RegistryHelper.getBiome(ResourceLocation.parse(biomeId), registryAccess);
        if (biome == null)
        {   ColdSweat.LOGGER.error("Failed to find biome with ID: {}", biomeId);
            return;
        }
        ConfigSettings.BIOME_OFFSETS.get().put(biome, new Triplet<>(minTemp, maxTemp, Temperature.Units.fromID(units)));
    }

    public void addBiomeOffset(String biomeId, double minTemp, double maxTemp)
    {   addBiomeOffset(biomeId, minTemp, maxTemp, "mc");
    }

    /*
     Dimension Temperature
     */

    public void addDimensionTemperature(String dimensionId, double temperature, String units)
    {
        RegistryAccess registryAccess = RegistryHelper.getRegistryAccess();
        DimensionType dimension = RegistryHelper.getDimension(ResourceLocation.parse(dimensionId), registryAccess);
        if (dimension == null)
        {   ColdSweat.LOGGER.error("Failed to find dimension with ID: {}", dimensionId);
            return;
        }
        ConfigSettings.DIMENSION_TEMPS.get().put(dimension, new Pair<>(temperature, Temperature.Units.fromID(units)));
    }

    public void addDimensionTemperature(String dimensionId, double temperature)
    {   addDimensionTemperature(dimensionId, temperature, "mc");
    }

    public void addDimensionOffset(String dimensionId, double temperature, String units)
    {
        RegistryAccess registryAccess = RegistryHelper.getRegistryAccess();
        DimensionType dimension = RegistryHelper.getDimension(ResourceLocation.parse(dimensionId), registryAccess);
        if (dimension == null)
        {   ColdSweat.LOGGER.error("Failed to find dimension with ID: {}", dimensionId);
            return;
        }
        ConfigSettings.DIMENSION_OFFSETS.get().put(dimension, new Pair<>(temperature, Temperature.Units.fromID(units)));
    }

    public void addDimensionOffset(String dimensionId, double temperature)
    {   addDimensionOffset(dimensionId, temperature, "mc");
    }

    /*
     Structure Temperature
     */

    public void addStructureTemperature(String structureId, double temperature, String units)
    {
        RegistryAccess registryAccess = RegistryHelper.getRegistryAccess();
        StructureType<?> structure = RegistryHelper.getStructure(ResourceLocation.parse(structureId), registryAccess);
        if (structure == null)
        {   ColdSweat.LOGGER.error("Failed to find structure with ID: {}", structure);
            return;
        }
        ConfigSettings.STRUCTURE_TEMPS.get().put(structure, new Pair<>(temperature, Temperature.Units.fromID(units)));
    }

    public void addStructureTemperature(String structureId, double temperature)
    {   addStructureTemperature(structureId, temperature, "mc");
    }

    public void addStructureOffset(String structureId, double temperature, String units)
    {
        RegistryAccess registryAccess = RegistryHelper.getRegistryAccess();
        StructureType<?> structure = RegistryHelper.getStructure(ResourceLocation.parse(structureId), registryAccess);
        if (structure == null)
        {   ColdSweat.LOGGER.error("Failed to find structure with ID: {}", structure);
            return;
        }
        ConfigSettings.STRUCTURE_OFFSETS.get().put(structure, new Pair<>(temperature, Temperature.Units.fromID(units)));
    }

    public void addStructureOffset(String structureId, double temperature)
    {   addStructureOffset(structureId, temperature, "mc");
    }

    /*
     TempModifier
     */

    public void addTempModifier(String id, Function<TempModifierDataJS, Function<Double, Double>> constructor)
    {
        ResourceLocation key = ResourceLocation.parse(id);
        if (key.getNamespace().equals("minecraft"))
        {   ColdSweat.LOGGER.error("KubeJS: Non-Minecraft namespace required for TempModifier IDs (i.e. mymod:my_modifier)");
            return;
        }
        class TempModifierJS extends TempModifier
        {
            public TempModifierJS()
            {}

            @Override
            protected Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait)
            {
                return constructor.apply(new TempModifierDataJS(entity, trait));
            }
        }

        TempModifierRegistry.register(key, TempModifierJS::new);
    }
}