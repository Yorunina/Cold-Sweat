package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.List;
import java.util.Optional;

public record DimensionTempData(List<Either<TagKey<DimensionType>, DimensionType>> dimensions, double temperature,
                                Temperature.Units units, boolean isOffset, Optional<List<String>> requiredMods)
{
    public static final Codec<DimensionTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrVanillaRegistryCodec(Registries.DIMENSION_TYPE, DimensionType.CODEC).listOf().fieldOf("dimensions").forGetter(DimensionTempData::dimensions),
            Codec.DOUBLE.fieldOf("temperature").forGetter(DimensionTempData::temperature),
            Temperature.Units.CODEC.optionalFieldOf("units", Temperature.Units.MC).forGetter(DimensionTempData::units),
            Codec.BOOL.optionalFieldOf("is_offset", false).forGetter(DimensionTempData::isOffset),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(DimensionTempData::requiredMods)
    ).apply(instance, DimensionTempData::new));

    @Override
    public String toString()
    {   return CODEC.encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("serialize_failed");
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        DimensionTempData that = (DimensionTempData) obj;
        return Double.compare(that.temperature, temperature) == 0
            && isOffset == that.isOffset
            && dimensions.equals(that.dimensions)
            && units == that.units
            && requiredMods.equals(that.requiredMods);
    }
}
