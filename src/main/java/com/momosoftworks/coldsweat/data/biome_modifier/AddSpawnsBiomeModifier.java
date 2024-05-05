package com.momosoftworks.coldsweat.data.biome_modifier;

import com.mojang.serialization.Codec;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.init.BiomeCodecInit;
import com.momosoftworks.coldsweat.data.configuration.SpawnBiomeData;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;


public record AddSpawnsBiomeModifier(boolean useConfigs) implements BiomeModifier
{
    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder)
    {
        if (phase == Phase.ADD && useConfigs)
        {
            Collection<SpawnBiomeData> spawns = ConfigSettings.ENTITY_SPAWN_BIOMES.get().get(biome.value());
            if (!spawns.isEmpty()) System.out.println("Adding spawns to " + biome.unwrapKey().get().location());
            for (SpawnBiomeData spawn : spawns)
            {
                ConfigHelper.mapForgeRegistryTagList(ForgeRegistries.ENTITY_TYPES, spawn.entities())
                .forEach(entityType ->
                {
                    builder.getMobSpawnSettings().getSpawner(MobCategory.CREATURE).removeIf(spawnerData -> spawnerData.type == entityType);
                    builder.getMobSpawnSettings().addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(entityType, spawn.weight(), 1, 1));
                });
            }
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec()
    {   return BiomeCodecInit.ADD_SPAWNS_CODEC.get();
    }
}