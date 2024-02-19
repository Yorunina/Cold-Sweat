package com.momosoftworks.coldsweat.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.capability.EntityTempManager;
import com.momosoftworks.coldsweat.common.capability.temperature.PlayerTempCap;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class Overlays
{
    public static final ResourceLocation BODY_TEMP_GAUGE = new ResourceLocation("cold_sweat:textures/gui/overlay/body_temp_gauge.png");
    public static final ResourceLocation BODY_TEMP_GAUGE_HC = new ResourceLocation("cold_sweat:textures/gui/overlay/body_temp_gauge_hc.png");
    public static final ResourceLocation WORLD_TEMP_GAUGE = new ResourceLocation("cold_sweat:textures/gui/overlay/world_temp_gauge.png");
    public static final ResourceLocation WORLD_TEMP_GAUGE_HC = new ResourceLocation("cold_sweat:textures/gui/overlay/world_temp_gauge_hc.png");
    public static final ResourceLocation VAGUE_TEMP_GAUGE = new ResourceLocation("cold_sweat:textures/gui/overlay/vague_temp_gauge.png");
    public static final ResourceLocation VAGUE_TEMP_GAUGE_HC = new ResourceLocation("cold_sweat:textures/gui/overlay/vague_temp_gauge_hc.png");

    public static final Supplier<ResourceLocation> BODY_TEMP_GAUGE_LOCATION  = () ->
            ConfigSettings.HIGH_CONTRAST.get() ? BODY_TEMP_GAUGE_HC
                                               : BODY_TEMP_GAUGE;
    public static final Supplier<ResourceLocation> WORLD_TEMP_GAUGE_LOCATION = () ->
            ConfigSettings.HIGH_CONTRAST.get() ? WORLD_TEMP_GAUGE_HC
                                               : WORLD_TEMP_GAUGE;
    public static final Supplier<ResourceLocation> VAGUE_TEMP_GAUGE_LOCATION = () ->
            ConfigSettings.HIGH_CONTRAST.get() ? VAGUE_TEMP_GAUGE_HC
                                               : VAGUE_TEMP_GAUGE;

    // Stuff for world temperature
    public static double WORLD_TEMP = 0;
    static boolean ADVANCED_WORLD_TEMP = false;
    static double PREV_WORLD_TEMP = 0;
    static double MAX_OFFSET = 0;
    static double MIN_OFFSET = 0;

    // Stuff for body temperature
    public static double BODY_TEMP = 0;
    static double PREV_BODY_TEMP = 0;
    static int BLEND_BODY_TEMP = 0;
    static int ICON_BOB = 0;
    static int BODY_ICON = 0;
    static int PREV_BODY_ICON = 0;
    static double BODY_TEMP_SEVERITY = 0;

    @SubscribeEvent
    public static void onWorldTempRender(RenderGameOverlayEvent.Post event)
    {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL)
        {
            ClientPlayerEntity player = Minecraft.getInstance().player;
            MatrixStack poseStack = event.getMatrixStack();
            int width = event.getWindow().getGuiScaledWidth();
            int height = event.getWindow().getGuiScaledHeight();

            if (player != null && (ADVANCED_WORLD_TEMP && Minecraft.getInstance().gameMode.getPlayerMode() != GameType.SPECTATOR
            && !Minecraft.getInstance().options.hideGui && ConfigSettings.WORLD_GAUGE_ENABLED.get()
            || player.isCreative()))
            {

            double min = ConfigSettings.MIN_TEMP.get();
            double max = ConfigSettings.MAX_TEMP.get();

            // Get player world temperature
            double temp = Temperature.convertUnits(WORLD_TEMP, ConfigSettings.CELSIUS.get() ? Temperature.Units.C : Temperature.Units.F, Temperature.Units.MC, true);

            // Get the temperature severity
            int severity = getWorldSeverity(temp, min, max, MIN_OFFSET, MAX_OFFSET);

            // Set text color
            int color;
            switch (severity)
            {   case  2 : case 3 : color = 16297781; break;
                case  4 : color = 16728089; break;
                case -2 : case -3 : color = 8443135; break;
                case -4 : color = 4236031; break;
                default : color = 14737376; break;
            }

            /* Render gauge */

            poseStack.pushPose();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

            // Set gauge texture
            Minecraft.getInstance().textureManager.bind(WORLD_TEMP_GAUGE_LOCATION.get());

            // Render frame
            AbstractGui.blit(poseStack, (width / 2) + 92 + ConfigSettings.WORLD_GAUGE_POS.get().getFirst(),
                              height - 19 + ConfigSettings.WORLD_GAUGE_POS.get().getSecond(), 0, 64 - severity * 16, 25, 16, 25, 144);

            RenderSystem.disableBlend();

            // Sets the text bobbing offset (or none if disabled)
            int bob = ConfigSettings.ICON_BOBBING.get() && !CSMath.isWithin(temp, min + MIN_OFFSET, max + MAX_OFFSET) && player.tickCount % 2 == 0 ? 1 : 0;

            // Render text
            int blendedTemp = (int) CSMath.blend(PREV_WORLD_TEMP, WORLD_TEMP, Minecraft.getInstance().getFrameTime(), 0, 1);

                Minecraft.getInstance().font.draw(poseStack, (blendedTemp + ConfigSettings.TEMP_OFFSET.get())+"",
                        /* X */ width / 2 + 105 + (Integer.toString(blendedTemp + ConfigSettings.TEMP_OFFSET.get()).length() * -3) + ConfigSettings.WORLD_GAUGE_POS.get().getFirst(),
                        /* Y */ height - 15 - bob + ConfigSettings.WORLD_GAUGE_POS.get().getSecond(), color);
                poseStack.popPose();
            }
        }
    }

    @SubscribeEvent
    public static void onBodyTempRender(RenderGameOverlayEvent.Post event)
    {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL)
        {
            Minecraft mc = Minecraft.getInstance();
            MatrixStack poseStack = event.getMatrixStack();
            int width = event.getWindow().getGuiScaledWidth();
            int height = event.getWindow().getGuiScaledHeight();

            if (mc.gameMode.canHurtPlayer() && mc.getCameraEntity() instanceof PlayerEntity && !Minecraft.getInstance().options.hideGui)
            {
                // Blend body temp (per frame)
                BLEND_BODY_TEMP = (int) CSMath.blend(PREV_BODY_TEMP, BODY_TEMP, Minecraft.getInstance().getFrameTime(), 0, 1);

                // Get text color
                int color;
                switch (((int) BODY_TEMP_SEVERITY))
                {   case  7 : case -7 : color = 16777215; break;
                    case  6 : color = 16777132; break;
                    case  5 : color = 16767856; break;
                    case  4 : color = 16759634; break;
                    case  3 : color = 16751174; break;
                    case -3 : color = 6078975; break;
                    case -4 : color = 7528447; break;
                    case -5 : color = 8713471; break;
                    case -6 : color = 11599871; break;
                    default : color = BLEND_BODY_TEMP > 0 ? 16744509
                             : BLEND_BODY_TEMP < 0 ? 4233468
                             : 11513775; break;
                }

                // Get the outer border color when readout is > 100
                int colorBG = BLEND_BODY_TEMP < 0 ? 1122643
                            : BLEND_BODY_TEMP > 0 ? 5376516
                            : 0;

                int bobLevel = Math.min(Math.abs(((int) BODY_TEMP_SEVERITY)), 3);
                int threatOffset = !ConfigSettings.ICON_BOBBING.get()
                                   ? 0
                                   : bobLevel == 2
                                     ? ICON_BOB
                                     : bobLevel == 3
                                       ? Minecraft.getInstance().cameraEntity.tickCount % 2
                                       : 0;

                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();

                // Render old icon (if blending)
                if (ConfigSettings.BODY_ICON_ENABLED.get())
                {
                    int icon = Math.abs(BLEND_BODY_TEMP) >= 100 ? BLEND_BODY_TEMP <= -100 ? -4 : 4 : BODY_ICON;
                    AbstractGui.blit(poseStack,
                                      (width / 2) - 5 + ConfigSettings.BODY_ICON_POS.get().getFirst(),
                                  height - 53 - threatOffset + ConfigSettings.BODY_ICON_POS.get().getSecond(), 0, 40 - icon * 10, 10, 10, 10, 90);
                    if (Math.abs(BLEND_BODY_TEMP) < 100)
                    {
                        // render new icon over old icon
                        double blend = CSMath.blend(1, 9, Math.abs(BODY_TEMP_SEVERITY), Math.abs(CSMath.floor(BODY_TEMP_SEVERITY)), Math.abs(CSMath.ceil(BODY_TEMP_SEVERITY)));
                        AbstractGui.blit(poseStack,
                                          (width / 2) - 5 + ConfigSettings.BODY_ICON_POS.get().getFirst(),
                                          height - 53 - threatOffset + ConfigSettings.BODY_ICON_POS.get().getSecond() + 10 - CSMath.ceil(blend),
                                          0, 40 - CSMath.grow(icon, BLEND_BODY_TEMP > 0 ? 0 : 2) * 10 - CSMath.ceil(blend), 10, CSMath.ceil(blend), 10, 90);
                    }
                }

                // Render Readout
                if (ConfigSettings.BODY_READOUT_ENABLED.get())
                {
                    FontRenderer font = mc.font;
                    int scaledWidth = mc.getWindow().getGuiScaledWidth();
                    int scaledHeight = mc.getWindow().getGuiScaledHeight();

                    String s = "" + Math.min(Math.abs(BLEND_BODY_TEMP), 100);
                    int x = (scaledWidth - font.width(s)) / 2 + ConfigSettings.BODY_READOUT_POS.get().getFirst();
                    int y = scaledHeight - 31 - 10 + ConfigSettings.BODY_READOUT_POS.get().getSecond();

                    // Draw the outline
                    font.draw(poseStack, s, x + 1, y, colorBG);
                    font.draw(poseStack, s, x - 1, y, colorBG);
                    font.draw(poseStack, s, x, y + 1, colorBG);
                    font.draw(poseStack, s, x, y - 1, colorBG);

                    // Draw the readout
                    font.draw(poseStack, s, x, y, color);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onVagueTempRender(RenderGameOverlayEvent.Post event)
    {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL)
        {
            Minecraft mc = Minecraft.getInstance();
            PlayerEntity player = mc.player;
            MatrixStack poseStack = event.getMatrixStack();
            int width = event.getWindow().getGuiScaledWidth();
            int height = event.getWindow().getGuiScaledHeight();

            if (player != null && !ADVANCED_WORLD_TEMP && mc.gameMode.getPlayerMode() != GameType.SPECTATOR
            && !mc.options.hideGui && ConfigSettings.WORLD_GAUGE_ENABLED.get() && gui.shouldDrawSurvivalElements())
            {
                double min = ConfigSettings.MIN_TEMP.get();
                double max = ConfigSettings.MAX_TEMP.get();

                // Get player world temperature
                double temp = Temperature.convertUnits(WORLD_TEMP, ConfigSettings.CELSIUS.get() ? Temperature.Units.C : Temperature.Units.F, Temperature.Units.MC, true);
                // Get the temperature severity
                int severity = getWorldSeverity(temp, min, max, MIN_OFFSET, MAX_OFFSET);
                int renderOffset = CSMath.clamp(severity, -1, 1) * 2;

                poseStack.pushPose();
                RenderSystem.defaultBlendFunc();
                RenderSystem.enableBlend();
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

                int bobLevel = Math.min(Math.abs(((int) BODY_TEMP_SEVERITY)), 3);
                int threatOffset = !ConfigSettings.ICON_BOBBING.get()
                                   ? 0
                                   : bobLevel == 2
                                     ? ICON_BOB
                                     : bobLevel == 3
                                       ? Minecraft.getInstance().cameraEntity.tickCount % 2
                                       : 0;

                // Set gauge texture
                mc.textureManager.bind(VAGUE_TEMP_GAUGE_LOCATION.get());

                // Render frame
                AbstractGui.blit(poseStack,
                                  //(width / 2) + 96 + CLIENT_CONFIG.getWorldGaugeX(),
                                  //height - 19 + CLIENT_CONFIG.getWorldGaugeY() - renderOffset,
                                  (width / 2) - 8 + ConfigSettings.BODY_ICON_POS.get().getFirst(),
                                  height - 56 + ConfigSettings.BODY_ICON_POS.get().getSecond() - renderOffset - threatOffset,
                                  0, 64 - severity * 16, 16, 16, 16, 144);

                poseStack.popPose();
            }
        }
    }

    // Handle temperature blending and transitions
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        PlayerEntity player = Minecraft.getInstance().player;
        if (event.phase == TickEvent.Phase.START && player != null)
        {
            EntityTempManager.getTemperatureCap(player).ifPresent(icap ->
            {
                if (!(icap instanceof PlayerTempCap)) return;
                PlayerTempCap cap = (PlayerTempCap) icap;

                cap.calculateVisibility(player);
                ADVANCED_WORLD_TEMP = cap.showAdvancedWorldTemp();


                /* World Temp */

                // Get temperature in actual degrees
                boolean celsius = ConfigSettings.CELSIUS.get();
                double worldTemp = cap.getTemp(Temperature.Type.WORLD);
                double realTemp = Temperature.convertUnits(worldTemp, Temperature.Units.MC, celsius ? Temperature.Units.C : Temperature.Units.F, true);
                // Calculate the blended world temp for this tick
                double diff = realTemp - WORLD_TEMP;
                PREV_WORLD_TEMP = WORLD_TEMP;
                WORLD_TEMP += Math.abs(diff) <= 1 ? diff : CSMath.maxAbs(diff / ConfigSettings.TEMP_SMOOTHING.get(), 0.25 * CSMath.getSign(diff));

                // Update max/min offset
                MAX_OFFSET = cap.getAbility(Temperature.Ability.FREEZING_POINT);
                MIN_OFFSET = cap.getAbility(Temperature.Ability.BURNING_POINT);


                /* Body Temp */

                // Blend body temp (per tick)
                PREV_BODY_TEMP = BODY_TEMP;
                double currentTemp = cap.getTemp(Temperature.Type.BODY);
                BODY_TEMP = Math.abs(currentTemp - BODY_TEMP) < 0.1 ? currentTemp : BODY_TEMP + (cap.getTemp(Temperature.Type.BODY) - BODY_TEMP) / 5;

                // Handle effects for the icon (bobbing, stage, transition)
                // Get icon bob
                ICON_BOB = player.tickCount % 3 == 0 && Math.random() < 0.3 ? 1 : 0;

                // Get the severity of the player's body temperature
                BODY_TEMP_SEVERITY = getBodySeverity(BLEND_BODY_TEMP);

                // Get the icon to be displayed
                int neededIcon = ((int) CSMath.clamp(BODY_TEMP_SEVERITY, -4, 4));

                // Start transition
                if (BODY_ICON != neededIcon)
                {   BODY_ICON = neededIcon;
                }
            });
        }
    }

    public static int getWorldSeverity(double temp, double min, double max, double offsMin, double offsMax)
    {   return (int) CSMath.blend(-4, 4, temp, min + offsMin, max + offsMax);
    }

    static double getBodySeverity(int temp)
    {   int sign = CSMath.getSign(temp);
        int absTemp = Math.abs(temp);

        return absTemp < 100 ? CSMath.blend(0d, 3d, absTemp, 0, 100) * sign
                             : CSMath.blend(3d, 7d, absTemp, 100, 150) * sign;
    }

    public static void setBodyTempInstant(double temp)
    {   BODY_TEMP = temp;
        PREV_BODY_TEMP = temp;
        BLEND_BODY_TEMP = (int) temp;
        BODY_ICON = CSMath.clamp(((int) getBodySeverity(BLEND_BODY_TEMP)), -3, 3);
        PREV_BODY_ICON = BODY_ICON;
    }

    public static void setWorldTempInstant(double temp)
    {   WORLD_TEMP = temp;
        PREV_WORLD_TEMP = temp;
    }
}
