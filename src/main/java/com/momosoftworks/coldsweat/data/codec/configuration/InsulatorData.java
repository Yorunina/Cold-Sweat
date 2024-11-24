package com.momosoftworks.coldsweat.data.codec.configuration;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.insulation.AdaptiveInsulation;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.api.insulation.StaticInsulation;
import com.momosoftworks.coldsweat.data.codec.impl.ConfigData;
import com.momosoftworks.coldsweat.data.codec.impl.RequirementHolder;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemComponentsRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.util.AttributeModifierMap;
import com.momosoftworks.coldsweat.data.codec.util.CommonStreamCodecs;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record InsulatorData(Insulation.Slot slot,
                            Insulation insulation, ItemRequirement data,
                            EntityRequirement predicate, AttributeModifierMap attributes,
                            Map<ResourceLocation, Double> immuneTempModifiers,
                            Optional<List<String>> requiredMods) implements NbtSerializable, RequirementHolder, ConfigData<InsulatorData>
{

    public InsulatorData(Insulation.Slot slot, Insulation insulation, ItemRequirement data,
                         EntityRequirement predicate, AttributeModifierMap attributes,
                         Map<ResourceLocation, Double> immuneTempModifiers)
    {
        this(slot, insulation, data, predicate, attributes, immuneTempModifiers, Optional.empty());
    }

    public static final Codec<InsulatorData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Insulation.Slot.CODEC.fieldOf("type").forGetter(InsulatorData::slot),
            Insulation.getCodec().fieldOf("insulation").forGetter(InsulatorData::insulation),
            ItemRequirement.CODEC.fieldOf("data").forGetter(InsulatorData::data),
            EntityRequirement.getCodec().optionalFieldOf("entity", EntityRequirement.NONE).forGetter(InsulatorData::predicate),
            AttributeModifierMap.CODEC.optionalFieldOf("attributes", new AttributeModifierMap()).forGetter(InsulatorData::attributes),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.DOUBLE).optionalFieldOf("immune_temp_modifiers", new HashMap<>()).forGetter(InsulatorData::immuneTempModifiers),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(InsulatorData::requiredMods)
    ).apply(instance, InsulatorData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, InsulatorData> STREAM_CODEC = StreamCodec.of(
    (buf, insulator) ->
    {
        Insulation.getNetworkCodec().encode(buf, insulator.insulation());
        buf.writeEnum(insulator.slot());
        ItemRequirement.STREAM_CODEC.encode(buf, insulator.data());
        buf.writeNbt(EntityRequirement.getCodec().encode(insulator.predicate(), NbtOps.INSTANCE, new CompoundTag()).result().orElse(new CompoundTag()));
        AttributeModifierMap.STREAM_CODEC.encode(buf, insulator.attributes());
        buf.writeMap(insulator.immuneTempModifiers(), ResourceLocation.STREAM_CODEC, CommonStreamCodecs.DOUBLE);
    },
    (buf) ->
    {
        Insulation insulation = Insulation.getNetworkCodec().decode(buf);
        Insulation.Slot slot = buf.readEnum(Insulation.Slot.class);
        ItemRequirement data = ItemRequirement.STREAM_CODEC.decode(buf);
        EntityRequirement predicate = EntityRequirement.getCodec().decode(NbtOps.INSTANCE, buf.readNbt()).result().orElse(Pair.of(EntityRequirement.NONE, new CompoundTag())).getFirst();
        AttributeModifierMap attributes = AttributeModifierMap.STREAM_CODEC.decode(buf);
        Map<ResourceLocation, Double> immuneTempModifiers = buf.readMap(ResourceLocation.STREAM_CODEC, CommonStreamCodecs.DOUBLE);
        return new InsulatorData(slot, insulation, data, predicate, attributes, immuneTempModifiers);
    });

    @Override
    public boolean test(ItemStack stack)
    {   return data.test(stack, true);
    }

    @Override
    public boolean test(Entity entity)
    {   return predicate.test(entity);
    }

    @Nullable
    public static InsulatorData fromToml(List<?> entry, Insulation.Slot slot)
    {
        String[] itemIDs = ((String) entry.get(0)).split(",");
        List<Item> items = ConfigHelper.getItems(itemIDs);
        if (items.isEmpty())
        {   ColdSweat.LOGGER.error("Error parsing {} insulator config: string \"{}\" does not contain any valid items", slot.getSerializedName(), entry.get(0));
            return null;
        }
        if (entry.size() < 3)
        {   ColdSweat.LOGGER.error("Error parsing {} insulator config: not enough arguments", slot.getSerializedName());
            return null;
        }

        boolean adaptive = entry.size() > 3 && entry.get(3).equals("adaptive");
        ItemComponentsRequirement components = entry.size() > 4 ? ItemComponentsRequirement.parse((String) entry.get(4)) : new ItemComponentsRequirement();
        double insulVal1 = ((Number) entry.get(1)).doubleValue();
        double insulVal2 = ((Number) entry.get(2)).doubleValue();

        Insulation insulation = adaptive ? new AdaptiveInsulation(insulVal1, insulVal2)
                                         : new StaticInsulation(insulVal1, insulVal2);

        ItemRequirement requirement = new ItemRequirement(items, components);

        return new InsulatorData(slot, insulation, requirement, EntityRequirement.NONE, new AttributeModifierMap(), new HashMap<>(), Optional.empty());
    }

    @Override
    public CompoundTag serialize()
    {   return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
    }

    public static InsulatorData deserialize(CompoundTag tag)
    {   return CODEC.decode(NbtOps.INSTANCE, tag).result().orElseThrow(() -> new IllegalArgumentException("Could not deserialize Insulator")).getFirst();
    }

    @Override
    public Codec<InsulatorData> getCodec()
    {   return CODEC;
    }

    @Override
    public String toString()
    {   return this.asString();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        InsulatorData that = (InsulatorData) obj;
        return slot == that.slot
            && insulation.equals(that.insulation)
            && data.equals(that.data)
            && predicate.equals(that.predicate)
            && attributes.equals(that.attributes)
            && immuneTempModifiers.equals(that.immuneTempModifiers)
            && requiredMods.equals(that.requiredMods);
    }
}
