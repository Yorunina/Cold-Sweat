package com.momosoftworks.coldsweat.core.network.message;

import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.client.event.TooltipHandler;
import com.momosoftworks.coldsweat.common.capability.handler.ItemInsulationManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.configuration.*;
import com.momosoftworks.coldsweat.util.math.FastMap;
import com.momosoftworks.coldsweat.util.serialization.DynamicHolder;
import com.momosoftworks.coldsweat.util.serialization.NbtSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SyncItemPredicatesMessage implements CustomPacketPayload
{
    public static final Type<SyncItemPredicatesMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, "sync_item_predicates"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncItemPredicatesMessage> CODEC = CustomPacketPayload.codec(SyncItemPredicatesMessage::encode, SyncItemPredicatesMessage::decode);

    private final Map<String, Object> predicateMap = new FastMap<>();
    ItemStack stack = ItemStack.EMPTY;
    int inventorySlot = 0;
    @Nullable EquipmentSlot equipmentSlot = null;

    public static SyncItemPredicatesMessage fromClient(ItemStack stack, int inventorySlot, @Nullable EquipmentSlot equipmentSlot)
    {   return new SyncItemPredicatesMessage(stack, inventorySlot, equipmentSlot);
    }

    public static SyncItemPredicatesMessage fromServer(ItemStack stack, int inventorySlot, @Nullable EquipmentSlot equipmentSlot, Entity entity)
    {   return new SyncItemPredicatesMessage(stack, inventorySlot, equipmentSlot, entity);
    }

    public SyncItemPredicatesMessage(ItemStack stack, int inventorySlot, @Nullable EquipmentSlot equipmentSlot)
    {
        this.stack = stack;
        this.inventorySlot = inventorySlot;
        this.equipmentSlot = equipmentSlot;
    }

    public SyncItemPredicatesMessage(ItemStack stack, int inventorySlot, @Nullable EquipmentSlot equipmentSlot, Entity entity)
    {
        this.stack = stack;
        this.checkInsulator(stack, entity);
        this.checkInsulatingArmor(stack, entity);
        this.checkInsulatingCurio(stack, entity);
        this.checkArmorInsulation(stack, entity);

        this.checkFood(stack, entity);

        this.checkBoilerFuel(stack);
        this.checkIceboxFuel(stack);
        this.checkHearthFuel(stack);
        this.checkSoulLampFuel(stack);

        this.checkCarriedTemps(stack, inventorySlot, equipmentSlot, entity);
    }

    public SyncItemPredicatesMessage(ItemStack stack, int inventorySlot, @Nullable EquipmentSlot equipmentSlot, Map<String, Object> predicateMap)
    {
        this.stack = stack;
        this.inventorySlot = inventorySlot;
        this.equipmentSlot = equipmentSlot;
        this.predicateMap.putAll(predicateMap);
    }

    public static void encode(SyncItemPredicatesMessage message, RegistryFriendlyByteBuf buffer)
    {
        ItemStack.STREAM_CODEC.encode(buffer, message.stack);
        buffer.writeInt(message.inventorySlot);
        buffer.writeOptional(Optional.ofNullable(message.equipmentSlot), FriendlyByteBuf::writeEnum);

        if (message.predicateMap.isEmpty())
        {   buffer.writeBoolean(false);
            return;
        }
        else buffer.writeBoolean(true);

        buffer.writeMap(message.predicateMap,
        FriendlyByteBuf::writeUtf,
        (buf, value) ->
        {
            if (value instanceof Map)
            {
                buf.writeUtf("Map");
                buf.writeMap((Map<?, ?>) value,
                (buf1, key) ->
                {
                    if (key instanceof NbtSerializable)
                    {
                        buf1.writeUtf(key.getClass().getSimpleName());
                        buf1.writeNbt(((NbtSerializable) key).serialize());
                    }
                    else throw new IllegalStateException("Invalid key type: " + key.getClass());
                },
                (buf1, value1) ->
                {
                    if (value1 instanceof Boolean)
                        buf1.writeBoolean((Boolean) value1);
                    else throw new IllegalStateException("Invalid value type: " + value1.getClass());
                });
            }
            else if (value instanceof List)
            {
                buf.writeUtf("List");
                List<?> list = (List<?>) value;
                buf.writeCollection(list, (buf1, element) ->
                {
                    if (element instanceof Pair<?,?>)
                    {
                        Pair<?,?> pair = (Pair<?,?>) element;
                        if (pair.getFirst() instanceof ItemStack)
                        {
                            ItemStack.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf1, (ItemStack) pair.getFirst());
                            buf1.writeMap((Map<?, ?>) pair.getSecond(),
                            (buf2, key) ->
                            {
                                if (key instanceof NbtSerializable)
                                {
                                    buf2.writeUtf(key.getClass().getSimpleName());
                                    buf2.writeNbt(((NbtSerializable) key).serialize());
                                }
                                else throw new IllegalStateException("Invalid key type: " + key.getClass());
                            },
                            (buf2, value1) ->
                            {
                                if (value1 instanceof Boolean)
                                    buf2.writeBoolean((Boolean) value1);
                                else throw new IllegalStateException("Invalid value type: " + value1.getClass());
                            });
                        }
                        else throw new IllegalStateException("Invalid pair first type: " + pair.getFirst().getClass());
                    }
                    else throw new IllegalStateException("Invalid element type: " + element.getClass());
                });
            }
        });
    }

    public static SyncItemPredicatesMessage decode(RegistryFriendlyByteBuf buffer)
    {
        ItemStack stack = ItemStack.STREAM_CODEC.decode(buffer);
        int inventorySlot = buffer.readInt();
        EquipmentSlot equipmentSlot = buffer.readOptional(buf -> buf.readEnum(EquipmentSlot.class)).orElse(null);

        Map<String, Object> predicateMap;
        if (!buffer.readBoolean())
        {   predicateMap = new FastMap<>();
        }
        else predicateMap = buffer.readMap(FriendlyByteBuf::readUtf,
        buf ->
        {
            String type = buf.readUtf();
            if (type.equals("Map"))
            {
                return buf.readMap(
                FastMap::new,
                buf1 ->
                {
                    String className = buf1.readUtf();
                    return getDeserializer(className).apply(buf1.readNbt());
                },
                FriendlyByteBuf::readBoolean);
            }
            else if (type.equals("List"))
            {
                return buffer.readCollection(
                ArrayList::new,
                buf1 ->
                {
                    ItemStack itemStack = ItemStack.STREAM_CODEC.decode(((RegistryFriendlyByteBuf) buf1));
                    Map<Object, Boolean> map = buf1.readMap(
                    FastMap::new,
                    buf2 ->
                    {
                        String className = buf2.readUtf();
                        return getDeserializer(className).apply(buf2.readNbt());
                    },
                    FriendlyByteBuf::readBoolean);
                    return Pair.of(itemStack, map);
                });
            }
            else throw new IllegalStateException("Invalid type: " + type);
        });

        return new SyncItemPredicatesMessage(stack, inventorySlot, equipmentSlot, predicateMap);
    }

    public static void handle(SyncItemPredicatesMessage message, IPayloadContext context)
    {
        LogicalSide receivingSide = context.flow().getReceptionSide();

        // Server is telling client which insulators pass their checks
        if (receivingSide.isClient())
        {
            context.enqueueWork(() ->
            {   TooltipHandler.HOVERED_STACK_PREDICATES = message.predicateMap;
            });
        }
        // Client is asking server for insulator predicates
        else if (receivingSide.isServer() && context.player() instanceof ServerPlayer player)
        {
            context.enqueueWork(() ->
            {
                PacketDistributor.sendToPlayer(player, SyncItemPredicatesMessage.fromServer(message.stack, message.inventorySlot, message.equipmentSlot, player));
            });
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {   return TYPE;
    }

    private void checkInsulator(ItemStack stack, Entity entity)
    {   this.checkItemRequirement(stack, entity, (DynamicHolder) ConfigSettings.INSULATION_ITEMS);
    }

    private void checkInsulatingArmor(ItemStack stack, Entity entity)
    {   this.checkItemRequirement(stack, entity, (DynamicHolder) ConfigSettings.INSULATING_ARMORS);
    }

    private void checkInsulatingCurio(ItemStack stack, Entity entity)
    {   this.checkItemRequirement(stack, entity, (DynamicHolder) ConfigSettings.INSULATING_CURIOS);
    }

    private void checkArmorInsulation(ItemStack stack, Entity entity)
    {
        if (ItemInsulationManager.isInsulatable(stack))
        {
            ItemInsulationManager.getInsulationCap(stack).ifPresent(cap ->
            {
                List<Pair<ItemStack, Map<InsulatorData, Boolean>>> insulatorMap =
                cap.getInsulation().stream()
                   .map(pair -> Pair.of(pair.getFirst(),
                                        pair.getSecond().entries().stream()
                                            .map(entry -> Map.entry(entry.getKey(), entry.getKey().test(entity, pair.getFirst())))
                                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))))
                   .collect(Collectors.toList());

                this.predicateMap.put("armor_insulation", insulatorMap);
            });
        }
    }

    private void checkFood(ItemStack stack, Entity entity)
    {   this.checkItemRequirement(stack, entity, (DynamicHolder) ConfigSettings.FOOD_TEMPERATURES);
    }

    private void checkBoilerFuel(ItemStack stack)
    {   this.checkItemRequirement(stack, null, (DynamicHolder) ConfigSettings.BOILER_FUEL);
    }

    private void checkIceboxFuel(ItemStack stack)
    {   this.checkItemRequirement(stack, null, (DynamicHolder) ConfigSettings.ICEBOX_FUEL);
    }

    private void checkHearthFuel(ItemStack stack)
    {   this.checkItemRequirement(stack, null, (DynamicHolder) ConfigSettings.HEARTH_FUEL);
    }

    private void checkSoulLampFuel(ItemStack stack)
    {   this.checkItemRequirement(stack, null, (DynamicHolder) ConfigSettings.SOULSPRING_LAMP_FUEL);
    }

    private void checkCarriedTemps(ItemStack stack, int invSlot, EquipmentSlot equipmentSlot, Entity entity)
    {
        if (ConfigSettings.CARRIED_ITEM_TEMPERATURES.get().containsKey(stack.getItem()))
        {
            Map<ItemCarryTempData, Boolean> insulatorMap = ConfigSettings.CARRIED_ITEM_TEMPERATURES.get().get(stack.getItem()).stream()
                    .map(data -> Map.entry(data, data.test(entity, stack, invSlot, equipmentSlot)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            String configId = ConfigSettings.CONFIG_SETTINGS.inverse().get(ConfigSettings.CARRIED_ITEM_TEMPERATURES);
            this.predicateMap.put(configId, insulatorMap);
        }
    }

    private void checkItemRequirement(ItemStack stack, Entity entity, DynamicHolder<Multimap<Item, ? extends RequirementHolder>> configSetting)
    {
        if (configSetting.get().containsKey(stack.getItem()))
        {
            Map<? extends RequirementHolder, Boolean> configMap = configSetting.get().get(stack.getItem()).stream()
                    .map(data -> Map.entry(data, data.test(entity, stack)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            String configId = ConfigSettings.CONFIG_SETTINGS.inverse().get(configSetting);
            this.predicateMap.put(configId, configMap);
        }
    }

    private static Function<CompoundTag, Object> getDeserializer(String className)
    {
        return switch (className)
        {
            case "ItemCarryTempData" -> ItemCarryTempData::deserialize;
            case "InsulatorData" -> InsulatorData::deserialize;
            case "FuelData" -> FuelData::deserialize;
            default -> throw new IllegalStateException("Invalid class name: " + className);
        };
    }
}
