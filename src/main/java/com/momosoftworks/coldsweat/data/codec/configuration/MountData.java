package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Optional;

public record MountData(List<Either<TagKey<EntityType<?>>, EntityType<?>>> entities, double coldInsulation,
                        double heatInsulation, EntityRequirement requirement,
                        Optional<List<String>> requiredMods)
{
    public static Codec<MountData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrForgeRegistryCodec(Registries.ENTITY_TYPE, ForgeRegistries.ENTITY_TYPES).listOf().fieldOf("entities").forGetter(MountData::entities),
            Codec.DOUBLE.fieldOf("cold_insulation").forGetter(MountData::coldInsulation),
            Codec.DOUBLE.fieldOf("heat_insulation").forGetter(MountData::heatInsulation),
            EntityRequirement.getCodec().fieldOf("entity").forGetter(MountData::requirement),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(MountData::requiredMods)
    ).apply(instance, MountData::new));

    @Override
    public String toString()
    {   return CODEC.encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("serialize_failed");
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        MountData that = (MountData) obj;
        return Double.compare(that.coldInsulation, coldInsulation) == 0
            && Double.compare(that.heatInsulation, heatInsulation) == 0
            && entities.equals(that.entities)
            && requirement.equals(that.requirement)
            && requiredMods.equals(that.requiredMods);
    }
}
