package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.util.ExtraCodecs;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record BlockRequirement(Optional<List<Either<TagKey<Block>, Block>>> blocks, Optional<StateRequirement> state,
                               Optional<NbtRequirement> nbt, Optional<Direction> sturdyFace,
                               Optional<Boolean> withinWorldBounds, Optional<Boolean> replaceable,
                               boolean negate)
{
    public static final BlockRequirement NONE = new BlockRequirement(Optional.empty(), Optional.empty(), Optional.empty(),
                                                                     Optional.empty(), Optional.empty(), Optional.empty(),
                                                                     false);

    public static final Codec<BlockRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrBuiltinCodec(Registries.BLOCK, BuiltInRegistries.BLOCK).listOf().optionalFieldOf("blocks").forGetter(predicate -> predicate.blocks),
            StateRequirement.CODEC.optionalFieldOf("state").forGetter(predicate -> predicate.state),
            NbtRequirement.CODEC.optionalFieldOf("nbt").forGetter(predicate -> predicate.nbt),
            Direction.CODEC.optionalFieldOf("has_sturdy_face").forGetter(predicate -> predicate.sturdyFace),
            Codec.BOOL.optionalFieldOf("within_world_bounds").forGetter(predicate -> predicate.withinWorldBounds),
            Codec.BOOL.optionalFieldOf("replaceable").forGetter(predicate -> predicate.replaceable),
            Codec.BOOL.optionalFieldOf("negate", false).forGetter(predicate -> predicate.negate)
    ).apply(instance, BlockRequirement::new));

    public boolean test(Level level, BlockPos pos)
    {
        if (!level.isLoaded(pos))
        {   return false;
        }
        else
        {
            BlockState blockstate = level.getBlockState(pos);
            if (this.blocks.isPresent() && this.blocks.get().stream().noneMatch(either -> either.map(blockstate::is, blockstate::is)))
            {   return false ^ this.negate;
            }
            else if (this.state.isPresent() && !this.state.get().test(blockstate))
            {   return false ^ this.negate;
            }
            else if (this.nbt.isPresent())
            {
                BlockEntity blockentity = level.getBlockEntity(pos);
                return (blockentity != null && this.nbt.get().test(blockentity.saveWithFullMetadata(level.registryAccess()))) ^ this.negate;
            }
            else if (this.sturdyFace.isPresent())
            {   return blockstate.isFaceSturdy(level, pos, this.sturdyFace.get()) ^ this.negate;
            }
            else if (this.withinWorldBounds.isPresent())
            {   return level.getWorldBorder().isWithinBounds(pos) ^ this.negate;
            }
            else if (this.replaceable.isPresent())
            {   return blockstate.isAir() || blockstate.canBeReplaced() ^ this.negate;
            }
            else
            {   return true ^ this.negate;
            }
        }
    }

    public CompoundTag serialize()
    {   return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
    }

    public static BlockRequirement deserialize(CompoundTag tag)
    {   return CODEC.decode(NbtOps.INSTANCE, tag).result().orElseThrow(() -> new IllegalArgumentException("Could not deserialize BlockRequirement")).getFirst();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {   return true;
        }
        if (obj == null || getClass() != obj.getClass())
        {   return false;
        }

        BlockRequirement that = (BlockRequirement) obj;

        if (!blocks.equals(that.blocks))
        {   return false;
        }
        if (!state.equals(that.state))
        {   return false;
        }
        if (!nbt.equals(that.nbt))
        {   return false;
        }
        if (!sturdyFace.equals(that.sturdyFace))
        {   return false;
        }
        if (!withinWorldBounds.equals(that.withinWorldBounds))
        {   return false;
        }
        if (!replaceable.equals(that.replaceable))
        {   return false;
        }
        return negate == that.negate;
    }

    public record StateRequirement(Map<String, Object> properties)
    {
        public static final Codec<StateRequirement> CODEC = Codec.unboundedMap(Codec.STRING, ExtraCodecs.anyOf(Codec.BOOL, Codec.INT, Codec.STRING, IntegerBounds.CODEC))
                                                                 .xmap(map ->
                                                                       {
                                                                           System.out.println("Map of properties: " + map);
                                                                           return new StateRequirement(map);
                                                                       }, StateRequirement::properties);

        public CompoundTag serialize()
        {   return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
        }

        public static StateRequirement deserialize(CompoundTag tag)
        {   return CODEC.decode(NbtOps.INSTANCE, tag).result().orElseThrow(() -> new IllegalArgumentException("Could not deserialize BlockRequirement")).getFirst();
        }

        public boolean test(BlockState state)
        {   return this.test(state.getBlock().getStateDefinition(), state);
        }

        public boolean test(FluidState state)
        {   return this.test(state.getType().getStateDefinition(), state);
        }

        public <S extends StateHolder<?, S>> boolean test(StateDefinition<?, S> stateDefinition, S state)
        {
            for (Map.Entry<String, Object> entry : this.properties.entrySet())
            {
                String key = entry.getKey();
                Object value = entry.getValue();

                Property<?> property = stateDefinition.getProperty(key);

                if (property == null)
                {   return false;
                }
                if (value instanceof IntegerBounds bounds)
                {
                    if (!property.getPossibleValues().contains(bounds.min())
                    || !property.getPossibleValues().contains(bounds.max())
                    || !bounds.test((Integer) state.getValue(property)))
                    {   return false;
                    }
                }
                else
                {
                    if (!property.getPossibleValues().contains(value)
                    || !state.getValue(property).toString().equals(value.toString()))
                    {   return false;
                    }
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {   return true;
            }
            if (obj == null || getClass() != obj.getClass())
            {   return false;
            }

            StateRequirement that = (StateRequirement) obj;

            return properties.equals(that.properties);
        }
    }

    @Override
    public String toString()
    {   return CODEC.encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("serialize_failed");
    }
}
