package com.momosoftworks.coldsweat.data.configuration.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class BlockTempData implements IForgeRegistryEntry<BlockTempData>
{
    List<Either<ITag<Block>, Block>> blocks;
    double temperature;
    double maxEffect;
    boolean fade;
    List<BlockState> conditions;
    Optional<CompoundNBT> tag;
    Optional<List<String>> requiredMods;

    public BlockTempData(List<Either<ITag<Block>, Block>> blocks, double temperature, double maxEffect, boolean fade,
                         List<BlockState> conditions, Optional<CompoundNBT> tag, Optional<List<String>> requiredMods)
    {
        this.blocks = blocks;
        this.temperature = temperature;
        this.maxEffect = maxEffect;
        this.fade = fade;
        this.conditions = conditions;
        this.tag = tag;
        this.requiredMods = requiredMods;
    }

    public static final Codec<BlockTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(
            // Convert from a string to a TagKey
            string ->
            {
                ResourceLocation tagLocation = new ResourceLocation(string.replace("#", ""));
                if (!string.contains("#")) return Either.<ITag<Block>, Block>right(ForgeRegistries.BLOCKS.getValue(tagLocation));

                return Either.<ITag<Block>, Block>left(BlockTags.getAllTags().getTag(tagLocation));
            },
            // Convert from a TagKey to a string
            tag ->
            {   if (tag == null) throw new IllegalArgumentException("Biome tag is null");
                String result = tag.left().isPresent()
                                ? "#" + BlockTags.getAllTags().getId(tag.left().get())
                                : tag.right().map(block -> ForgeRegistries.BLOCKS.getKey(block).toString()).orElse("");
                if (result.isEmpty()) throw new IllegalArgumentException("Biome field is not a tag or valid ID");
                return result;
            })
            .listOf()
            .fieldOf("blocks").forGetter(data -> data.blocks),
            Codec.DOUBLE.fieldOf("temperature").forGetter(data -> data.temperature),
            Codec.DOUBLE.optionalFieldOf("max_effect", Double.MAX_VALUE).forGetter(data -> data.maxEffect),
            Codec.BOOL.optionalFieldOf("fade", true).forGetter(data -> data.fade),
            BlockState.CODEC.listOf().optionalFieldOf("conditions", Arrays.asList()).forGetter(data -> data.conditions),
            CompoundNBT.CODEC.optionalFieldOf("tag").forGetter(data -> data.tag),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(data -> data.requiredMods)
    ).apply(instance, BlockTempData::new));

    @Override
    public BlockTempData setRegistryName(ResourceLocation name)
    {
        return null;
    }

    @Nullable
    @Override
    public ResourceLocation getRegistryName()
    {
        return null;
    }

    @Override
    public Class<BlockTempData> getRegistryType()
    {
        return BlockTempData.class;
    }
}