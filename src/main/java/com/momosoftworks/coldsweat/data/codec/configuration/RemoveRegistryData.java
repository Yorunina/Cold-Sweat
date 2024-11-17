package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.ModRegistries;
import com.momosoftworks.coldsweat.data.codec.requirement.NbtRequirement;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;

import java.util.List;
import java.util.Optional;

public record RemoveRegistryData<T>(ResourceKey<Registry<?>> registry, List<CompoundTag> entries)
{
    public static final Codec<RemoveRegistryData<?>> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(ModRegistries::getRegistry, ModRegistries::getRegistryName).fieldOf("registry").forGetter(data -> data.registry),
            CompoundTag.CODEC.listOf().fieldOf("matches").forGetter(data -> data.entries)
    ).apply(instance, RemoveRegistryData::new));

    public ResourceKey<Registry<T>> getRegistry()
    {   return (ResourceKey) registry;
    }

    public boolean matches(T object)
    {
        Optional<Tag> serialized = ModRegistries.getCodec((ResourceKey) registry).encodeStart(NbtOps.INSTANCE, object).result();
        if (serialized.isPresent())
        {
            for (CompoundTag data : entries)
            {
                if (NbtRequirement.compareNbt(data, serialized.get(), true))
                {   return true;
                }
            }
        }
        return false;
    }
}
