package com.momosoftworks.coldsweat.client.event;

import com.google.common.collect.Multimap;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.client.gui.tooltip.ClientSoulspringTooltip;
import com.momosoftworks.coldsweat.client.gui.tooltip.InsulationAttributeTooltip;
import com.momosoftworks.coldsweat.client.gui.tooltip.InsulationTooltip;
import com.momosoftworks.coldsweat.client.gui.tooltip.SoulspringTooltip;
import com.momosoftworks.coldsweat.common.capability.handler.ItemInsulationManager;
import com.momosoftworks.coldsweat.common.item.SoulspringLampItem;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;
import com.momosoftworks.coldsweat.core.network.message.SyncItemPredicatesMessage;
import com.momosoftworks.coldsweat.data.codec.configuration.FoodData;
import com.momosoftworks.coldsweat.data.codec.configuration.FuelData;
import com.momosoftworks.coldsweat.data.codec.configuration.InsulatorData;
import com.momosoftworks.coldsweat.data.codec.util.AttributeModifierMap;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.entity.EntityHelper;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.math.FastMap;
import com.momosoftworks.coldsweat.util.registries.ModAttributes;
import com.momosoftworks.coldsweat.util.registries.ModItems;
import com.momosoftworks.coldsweat.util.serialization.DynamicHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class TooltipHandler
{
    public static final Style COLD = Style.EMPTY.withColor(3767039);
    public static final Style HOT = Style.EMPTY.withColor(16736574);
    public static final Component EXPAND_TOOLTIP = Component.literal("?").withStyle(Style.EMPTY.withColor(ChatFormatting.BLUE).withUnderlined(true))
                                           .append(Component.literal(" 'Shift'").withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withUnderlined(false)));

    private static int HOVERED_ITEM_UPDATE_COOLDOWN = 0;
    private static ItemStack HOVERED_STACK = ItemStack.EMPTY;
    public static Map<String, Object> HOVERED_STACK_PREDICATES = new FastMap<>();

    private static <T> Map<T, Boolean> getPropertyMap(DynamicHolder<Multimap<Item, T>> config)
    {   return (Map<T, Boolean>) HOVERED_STACK_PREDICATES.getOrDefault(ConfigSettings.getKey(config), new FastMap<>());
    }
    public static <T> boolean passesRequirement(DynamicHolder<Multimap<Item, T>> config, T element)
    {   return getPropertyMap(config).getOrDefault(element, true);
    }

    public static boolean isShiftDown()
    {   return Screen.hasShiftDown() || ConfigSettings.EXPAND_TOOLTIPS.get();
    }

    public static int getTooltipTitleIndex(List<Either<FormattedText, TooltipComponent>> tooltip, ItemStack stack)
    {
        if (tooltip.isEmpty()) return 0;

        int tooltipStartIndex;
        String hoverName = stack.getHoverName().getString();

        if (CompatManager.isIcebergLoaded())
        {   tooltipStartIndex = CompatManager.getLegendaryTTStartIndex(tooltip) + 1;
        }
        else for (tooltipStartIndex = 0; tooltipStartIndex < tooltip.size(); tooltipStartIndex++)
        {
            if (tooltip.get(tooltipStartIndex).left().map(FormattedText::getString).map(String::strip).orElse("").equals(hoverName))
            {   tooltipStartIndex++;
                break;
            }
        }
        tooltipStartIndex = CSMath.clamp(tooltipStartIndex, 0, tooltip.size());
        return tooltipStartIndex;
    }

    public static int getTooltipEndIndex(List<Either<FormattedText, TooltipComponent>> tooltip, ItemStack stack)
    {
        int tooltipEndIndex = tooltip.size();
        if (Minecraft.getInstance().options.advancedItemTooltips)
        {
            for (--tooltipEndIndex; tooltipEndIndex > 0; tooltipEndIndex--)
            {
                if (tooltip.get(tooltipEndIndex).left().map(text -> text.getString().equals(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString())).orElse(false))
                {   break;
                }
            }
        }
        tooltipEndIndex = CSMath.clamp(tooltipEndIndex, 0, tooltip.size());
        return tooltipEndIndex;
    }

    public static void addModifierTooltipLines(List<Component> tooltip, AttributeModifierMap map, boolean strikethrough)
    {
        map.getMap().asMap().forEach((attribute, modifiers) ->
        {
            for (AttributeModifier.Operation operation : AttributeModifier.Operation.values())
            {
                double value = 0;
                for (AttributeModifier modifier : modifiers.stream().filter(mod -> mod.getOperation() == operation).toList())
                {   value += modifier.getAmount();
                }
                if (value != 0)
                {   tooltip.add(getFormattedAttributeModifier(attribute, value, operation, false, strikethrough));
                }
            }
        });
    }

    public static MutableComponent getFormattedAttributeModifier(Attribute attribute, double amount, AttributeModifier.Operation operation,
                                                                 boolean forTooltip, boolean strikethrough)
    {
        if (attribute == null) return Component.empty();
        double value = amount;
        String attributeName = attribute.getDescriptionId().replace("attribute.", "");

        if (operation == AttributeModifier.Operation.ADDITION
        && (attribute == ModAttributes.FREEZING_POINT
        || attribute == ModAttributes.BURNING_POINT
        || attribute == ModAttributes.WORLD_TEMPERATURE
        || attribute == ModAttributes.BASE_BODY_TEMPERATURE))
        {
            value = Temperature.convert(value, Temperature.Units.MC, ConfigSettings.CELSIUS.get() ? Temperature.Units.C : Temperature.Units.F, false);
        }
        String operationString = operation == AttributeModifier.Operation.ADDITION ? "add" : "multiply";
        ChatFormatting color;
        String sign;
        if (value >= 0)
        {
            color = ChatFormatting.BLUE;
            sign = "+";
        }
        else
        {   color = ChatFormatting.RED;
            sign = "";
        }
        String percent;
        if (operation != AttributeModifier.Operation.ADDITION
        || attribute == ModAttributes.HEAT_RESISTANCE
        || attribute == ModAttributes.COLD_RESISTANCE
        || attribute == ModAttributes.HEAT_DAMPENING
        || attribute == ModAttributes.COLD_DAMPENING)
        {   percent = "%";
            value *= 100;
        }
        else
        {   percent = "";
        }
        List<String> params = new ArrayList<>(List.of(sign + CSMath.formatDoubleOrInt(CSMath.round(value, 2)) + percent));
        if (forTooltip)
        {   params.add("show_icon");
        }
        if (strikethrough)
        {   params.add("strikethrough");
        }
        return Component.translatable(String.format("attribute.cold_sweat.modifier.%s.%s", operationString, attributeName),
                                      params.toArray()).withStyle(color);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void updateHoveredItem(ScreenEvent.Render event)
    {
        if (event.getScreen() instanceof AbstractContainerScreen<?> menu)
        {
            Slot hoveredSlot = menu.getSlotUnderMouse();
            if (hoveredSlot == null) return;

            ItemStack stack = hoveredSlot.getItem();
            if (stack.isEmpty()) return;

            EquipmentSlot equipmentSlot = EntityHelper.getEquipmentSlot(hoveredSlot.index);
            if (!HOVERED_STACK.equals(stack))
            {
                HOVERED_STACK_PREDICATES.clear();
                if (HOVERED_ITEM_UPDATE_COOLDOWN <= 0)
                {
                    ColdSweatPacketHandler.INSTANCE.sendToServer(SyncItemPredicatesMessage.fromClient(stack, hoveredSlot.index, equipmentSlot));
                    HOVERED_STACK = stack;
                    HOVERED_ITEM_UPDATE_COOLDOWN = 5;
                }
            }
        }
    }

    @SubscribeEvent
    public static void tickHoverCooldown(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END && HOVERED_ITEM_UPDATE_COOLDOWN > 0)
        {   HOVERED_ITEM_UPDATE_COOLDOWN--;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void addCustomTooltips(RenderTooltipEvent.GatherComponents event)
    {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();
        var elements = event.getTooltipElements();
        boolean hideTooltips = ConfigSettings.HIDE_TOOLTIPS.get() && !isShiftDown();
        if (stack.isEmpty()) return;

        // Get the index at which the tooltip should be inserted
        int tooltipStartIndex = getTooltipTitleIndex(elements, stack);
        // Get the index of the end of the tooltip, before the debug info (if enabled)
        int tooltipEndIndex = getTooltipEndIndex(elements, stack);

        Player player = Minecraft.getInstance().player;

        /*
         Tooltips for soulspring lamp
         */
        if (stack.getItem() instanceof SoulspringLampItem)
        {   if (!isShiftDown())
            {   elements.add(tooltipStartIndex, Either.left(EXPAND_TOOLTIP));
            }
            elements.add(tooltipStartIndex, Either.right(new SoulspringTooltip(stack.getOrCreateTag().getDouble("Fuel"))));
        }

        /*
         Tooltip for food temperature
         */
        if (stack.getUseAnimation() == UseAnim.DRINK || stack.getUseAnimation() == UseAnim.EAT)
        {
            // Check if Diet has their own tooltip already
            int dietTooltipSectionIndex = CSMath.getIndexOf(elements, line -> line.left().map(text -> text.getString().equalsIgnoreCase(Component.translatable("tooltip.diet.eaten").getString())).orElse(false));
            int index = dietTooltipSectionIndex != -1
                        ? dietTooltipSectionIndex + 1
                        : tooltipEndIndex;

            Map<Integer, Double> foodTemps = new FastMap<>();
            for (FoodData foodData : ConfigSettings.FOOD_TEMPERATURES.get().get(item))
            {
                if (passesRequirement(ConfigSettings.FOOD_TEMPERATURES, foodData))
                {   foodTemps.merge(foodData.duration(), foodData.temperature(), Double::sum);
                }
            }

            for (Map.Entry<Integer, Double> entry : foodTemps.entrySet())
            {
                double temp = entry.getValue();
                int duration = entry.getKey();

                MutableComponent consumeEffects = temp > 0
                                                  ? Component.translatable("tooltip.cold_sweat.temperature_effect", "+" + CSMath.formatDoubleOrInt(temp)).withStyle(HOT) :
                                                  temp == 0
                                                  ? Component.translatable("tooltip.cold_sweat.temperature_effect", "+" + CSMath.formatDoubleOrInt(temp)) :
                                                  Component.translatable("tooltip.cold_sweat.temperature_effect", CSMath.formatDoubleOrInt(temp)).withStyle(COLD);
                // Add a duration to the tooltip if it exists
                if (duration > 0)
                {   consumeEffects.append(" (" + StringUtil.formatTickDuration(duration) + ")");
                }
                // Add the effect to the tooltip
                elements.add(index, Either.left(consumeEffects));
            }

            // Don't add our own section title if one already exists
            if (!foodTemps.isEmpty() && dietTooltipSectionIndex == -1)
            {
                elements.add(tooltipEndIndex, Either.left(Component.translatable("tooltip.cold_sweat.consumed").withStyle(ChatFormatting.GRAY)));
                elements.add(tooltipEndIndex, Either.left(Component.empty()));
            }
        }

        /*
         Tooltips for insulation
         */
        if (!hideTooltips && !stack.isEmpty())
        {
            List<InsulatorData> validInsulations = new ArrayList<>();

            // Insulation ingredient
            {
                List<Insulation> insulation = new ArrayList<>();
                List<Insulation> unmetInsulation = new ArrayList<>();
                for (InsulatorData insulator : ConfigSettings.INSULATION_ITEMS.get().get(item))
                {
                    if (!insulator.insulation().isEmpty())
                    {
                        if (passesRequirement(ConfigSettings.INSULATION_ITEMS, insulator))
                        {   insulation.addAll(insulator.insulation().split());
                        }
                        else unmetInsulation.addAll(insulator.insulation().split());
                        validInsulations.add(insulator);
                    }
                }
                if (!insulation.isEmpty())
                {   elements.add(tooltipStartIndex, Either.right(new InsulationTooltip(insulation, Insulation.Slot.ITEM, stack, false)));
                }
                if (!unmetInsulation.isEmpty())
                {   elements.add(tooltipStartIndex, Either.right(new InsulationTooltip(unmetInsulation, Insulation.Slot.ITEM, stack, true)));
                }
            }

            // Insulating curio
            if (CompatManager.isCuriosLoaded())
            {
                List<Insulation> insulation = new ArrayList<>();
                List<Insulation> unmetInsulation = new ArrayList<>();
                for (InsulatorData insulator : ConfigSettings.INSULATING_CURIOS.get().get(item))
                {
                    if (!insulator.insulation().isEmpty())
                    {
                        if (passesRequirement(ConfigSettings.INSULATING_CURIOS, insulator))
                        {   insulation.addAll(insulator.insulation().split());
                        }
                        else unmetInsulation.addAll(insulator.insulation().split());
                        validInsulations.add(insulator);
                    }
                }
                if (!insulation.isEmpty())
                {   elements.add(tooltipStartIndex, Either.right(new InsulationTooltip(insulation, Insulation.Slot.CURIO, stack, false)));
                }
                if (!unmetInsulation.isEmpty())
                {   elements.add(tooltipStartIndex, Either.right(new InsulationTooltip(unmetInsulation, Insulation.Slot.CURIO, stack, true)));
                }
            }

            List<Insulation> insulation = new ArrayList<>();
            List<Insulation> unmetInsulation = new ArrayList<>();

            // Insulating armor
            for (InsulatorData insulator : ConfigSettings.INSULATING_ARMORS.get().get(item))
            {
                if (!insulator.insulation().isEmpty())
                {
                    if (passesRequirement(ConfigSettings.INSULATING_ARMORS, insulator))
                    {   insulation.addAll(insulator.insulation().split());
                    }
                    else unmetInsulation.addAll(insulator.insulation().split());
                    validInsulations.add(insulator);
                }
            }

            ItemInsulationManager.getInsulationCap(stack).ifPresent(cap ->
            {
                cap.deserializeNBT(stack.getOrCreateTag());

                // Iterate over both the insulation items and the checks for each item
                List<Pair<ItemStack, Multimap<InsulatorData, Insulation>>> insulators = cap.getInsulation();
                List<Pair<ItemStack, Map<InsulatorData, Boolean>>> insulatorChecks = ((List) HOVERED_STACK_PREDICATES.get("armor_insulation"));

                for (int i = 0; i < insulators.size(); i++)
                {
                    // Get the next insulator item
                    Pair<ItemStack, Multimap<InsulatorData, Insulation>> pair = insulators.get(i);
                    // Get the next insulator check, or create an empty one
                    Pair<ItemStack, Map<InsulatorData, Boolean>> checkPair = insulatorChecks.size() > i
                                                                             ? insulatorChecks.get(i)
                                                                             : Pair.of(pair.getFirst(), new FastMap<>());

                    // Iterate over the insulators for this insulation item
                    for (Map.Entry<InsulatorData, Insulation> entry : pair.getSecond().entries())
                    {
                        // If the insulator has passed, or the check isn't present, the insulation is valid
                        boolean passes = checkPair.getSecond().getOrDefault(entry.getKey(), true);
                        if (passes)
                        {   insulation.add(entry.getValue());
                        }
                        // If the insulator check says the insulation isn't valid, it is "unmet"
                        else unmetInsulation.add(entry.getValue());
                    }
                }

                if (!insulation.isEmpty())
                {   elements.add(tooltipStartIndex, Either.right(new InsulationTooltip(insulation, Insulation.Slot.ARMOR, stack, false)));
                }
                if (!unmetInsulation.isEmpty())
                {   elements.add(tooltipStartIndex, Either.right(new InsulationTooltip(unmetInsulation, Insulation.Slot.ARMOR, stack, true)));
                }
            });
        }

        /*
         Custom tooltips for attributes from insulation
         */
        for (int i = 0; i < elements.size(); i++)
        {
            Either<FormattedText, TooltipComponent> element = elements.get(i);
            if (element.left().isPresent() && element.left().get() instanceof Component component)
            {
                if (component.getContents() instanceof TranslatableContents translatableContents
                && translatableContents.getArgs() != null)
                {
                    List<Object> args = Arrays.asList(translatableContents.getArgs());
                    if (args.contains("show_icon"))
                    {
                        boolean strikethrough = args.contains("strikethrough");
                        elements.set(i, Either.right(new InsulationAttributeTooltip(component, Minecraft.getInstance().font, strikethrough)));
                    }
                }
            }
        }
    }

    static int FUEL_FADE_TIMER = 0;

    @SubscribeEvent
    public static void renderSoulLampInsertTooltip(ScreenEvent.Render.Post event)
    {
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen)
        {
            if (screen.getSlotUnderMouse() != null && screen.getSlotUnderMouse().getItem().getItem() == ModItems.SOULSPRING_LAMP)
            {
                double fuel = screen.getSlotUnderMouse().getItem().getOrCreateTag().getDouble("Fuel");
                ItemStack carriedStack = screen.getMenu().getCarried();

                FuelData itemFuel = ConfigSettings.SOULSPRING_LAMP_FUEL.get().get(carriedStack.getItem())
                                         .stream()
                                         .filter(predicate -> predicate.test(carriedStack))
                                         .findFirst().orElse(null);
                if (!carriedStack.isEmpty()
                && itemFuel != null)
                {
                    double fuelValue = screen.getMenu().getCarried().getCount() * itemFuel.fuel();
                    int slotX = screen.getSlotUnderMouse().x + screen.getGuiLeft();
                    int slotY = screen.getSlotUnderMouse().y + screen.getGuiTop();

                    GuiGraphics graphics = event.getGuiGraphics();
                    PoseStack ps = graphics.pose();
                    if (event.getMouseY() < slotY + 8)
                    {   ps.translate(0, 32, 0);
                    }

                    graphics.renderTooltip(Minecraft.getInstance().font, List.of(Component.literal("       ")), Optional.empty(), slotX - 18, slotY + 1);

                    RenderSystem.defaultBlendFunc();

                    // Render background
                    graphics.blit(ClientSoulspringTooltip.TOOLTIP_LOCATION.get(), slotX - 7, slotY - 11, 401, 0, 0, 30, 8, 30, 34);

                    // Render ghost overlay
                    RenderSystem.enableBlend();
                    RenderSystem.setShaderColor(1f, 1f, 1f, 0.15f + (float) ((Math.sin(FUEL_FADE_TIMER / 5f) + 1f) / 2f) * 0.4f);
                    graphics.blit(ClientSoulspringTooltip.TOOLTIP_LOCATION.get(), slotX - 7, slotY - 11, 401, 0, 8, Math.min(30, (int) ((fuel + fuelValue) / 2.1333f)), 8, 30, 34);
                    RenderSystem.disableBlend();

                    // Render fuel
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1f);
                    graphics.blit(ClientSoulspringTooltip.TOOLTIP_LOCATION.get(), slotX - 7, slotY - 11, 401, 0, 16, (int) (fuel / 2.1333f), 8, 30, 34);
                }
            }
        }
    }

    @SubscribeEvent
    public static void tickSoulLampInsertTooltip(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {   FUEL_FADE_TIMER++;
        }
    }
}
