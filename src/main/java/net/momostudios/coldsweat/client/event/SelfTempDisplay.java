package net.momostudios.coldsweat.client.event;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.momostudios.coldsweat.config.ClientSettingsConfig;
import net.momostudios.coldsweat.config.ConfigCache;
import net.momostudios.coldsweat.core.capabilities.PlayerTempCapability;
import net.momostudios.coldsweat.util.CSMath;
import net.momostudios.coldsweat.util.PlayerHelper;
import net.momostudios.coldsweat.util.Units;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class SelfTempDisplay
{
    public static PlayerTempCapability playerCap = null;

    @SubscribeEvent
    public static void eventHandler(RenderGameOverlayEvent event)
    {
        ClientSettingsConfig CCS = ClientSettingsConfig.getInstance();
        Minecraft mc = Minecraft.getInstance();

        if (mc.getRenderViewEntity() != null && mc.getRenderViewEntity() instanceof PlayerEntity &&
        !event.isCancelable() && event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR
        && !((PlayerEntity) mc.getRenderViewEntity()).abilities.isCreativeMode && !mc.getRenderViewEntity().isSpectator())
        {
            int scaleX = event.getWindow().getScaledWidth();
            int scaleY = event.getWindow().getScaledHeight();
            PlayerEntity entity = (PlayerEntity) Minecraft.getInstance().getRenderViewEntity();

            if (playerCap == null || entity.ticksExisted % 40 == 0)
                playerCap = entity.getCapability(PlayerTempCapability.TEMPERATURE).orElse(new PlayerTempCapability());

            int temp = (int) playerCap.get(PlayerHelper.Types.COMPOSITE);
            double ambient = CSMath.convertUnits(AmbientGaugeDisplay.clientTemp, Units.F, Units.MC, true);
            double max = ConfigCache.getInstance().maxTemp;
            double min = ConfigCache.getInstance().minTemp;

            int threatLevel = 0;

            ResourceLocation icon;
            int color =
                    temp > 0 ? 16744509 :
                    temp < 0 ? 4233468 :
                    11513775;
            int colorBG =
                    temp < 0 ? 1122643 :
                    temp > 0 ? 5376516 :
                    0;
            int colorBG2 =
                    CSMath.isBetween(temp, -110, -100) ? 6866175 :
                    CSMath.isBetween(temp, -120, -110) ? 7390719 :
                    CSMath.isBetween(temp, -130, -120) ? 9824511 :
                    CSMath.isBetween(temp, -140, -130) ? 12779519 :
                    temp < - 140 ?                16777215 :
                    CSMath.isBetween(temp, 100, 110) ? 16744509 :
                    CSMath.isBetween(temp, 110, 120) ? 16755544 :
                    CSMath.isBetween(temp, 120, 130) ? 16766325 :
                    CSMath.isBetween(temp, 130, 140) ? 16771509 :
                    temp > 140 ? 16777215 : 0;

            if      (ambient >= CSMath.blend(min, max, 1.00, 0, 1)) { icon = new ResourceLocation("cold_sweat:textures/gui/overlay/temp_gauge_hot_2.png");  threatLevel = 2; }
            else if (ambient >= CSMath.blend(min, max, 0.80, 0, 1)) { icon = new ResourceLocation("cold_sweat:textures/gui/overlay/temp_gauge_hot_1.png");  threatLevel = 1; }
            else if (ambient >= CSMath.blend(min, max, 0.64, 0, 1)) { icon = new ResourceLocation("cold_sweat:textures/gui/overlay/temp_gauge_hot_0.png"); }
            else if (ambient >= CSMath.blend(min, max, 0.48, 0, 1)) { icon = new ResourceLocation("cold_sweat:textures/gui/overlay/temp_gauge_default.png"); }
            else if (ambient >= CSMath.blend(min, max, 0.32, 0, 1)) { icon = new ResourceLocation("cold_sweat:textures/gui/overlay/temp_gauge_default.png"); }
            else if (ambient >= CSMath.blend(min, max, 0.16, 0, 1)) { icon = new ResourceLocation("cold_sweat:textures/gui/overlay/temp_gauge_cold_0.png"); }
            else if (ambient >= CSMath.blend(min, max, 0.00, 0, 1)) { icon = new ResourceLocation("cold_sweat:textures/gui/overlay/temp_gauge_cold_1.png"); threatLevel = 1; }
            else                                                    { icon = new ResourceLocation("cold_sweat:textures/gui/overlay/temp_gauge_cold_2.png"); threatLevel = 2; }
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

            int threatOffset = 0;
            if (CCS.iconBobbing())
            {
                if (threatLevel == 1) threatOffset = entity.ticksExisted % 10 == 0 && Math.random() < 0.5 ? 1 : 0;
                if (threatLevel == 2) threatOffset = entity.ticksExisted % 2 == 0 ? 1 : 0;
            }

            // Render Icon
            mc.getTextureManager().bindTexture(icon);
            mc.ingameGUI.blit(event.getMatrixStack(), (scaleX / 2) - 5 + CCS.steveHeadX(), scaleY - 51 + threatOffset + CCS.steveHeadY(), 0, 0, 10, 10, 10, 10);


            // Render Readout
            FontRenderer fontRenderer = mc.fontRenderer;
            int scaledWidth = mc.getMainWindow().getScaledWidth();
            int scaledHeight = mc.getMainWindow().getScaledHeight();
            MatrixStack matrixStack = event.getMatrixStack();

            String s = "" + (int) Math.ceil(Math.min(Math.abs(temp), 100));
            float i1 = (scaledWidth - fontRenderer.getStringWidth(s)) / 2f + CCS.tempGaugeX();
            float j1 = scaledHeight - 31f - 8f + CCS.tempGaugeY();
            if (!CSMath.isBetween(temp, -100, 100))
            {
                fontRenderer.drawString(matrixStack, s, i1 + 2f, j1, colorBG2);
                fontRenderer.drawString(matrixStack, s, i1 - 2f, j1, colorBG2);
                fontRenderer.drawString(matrixStack, s, i1, j1 + 2f, colorBG2);
                fontRenderer.drawString(matrixStack, s, i1, j1 - 2f, colorBG2);
                fontRenderer.drawString(matrixStack, s, i1 + 1f, j1 + 1f, colorBG2);
                fontRenderer.drawString(matrixStack, s, i1 + 1f, j1 - 1f, colorBG2);
                fontRenderer.drawString(matrixStack, s, i1 - 1f, j1 - 1f, colorBG2);
                fontRenderer.drawString(matrixStack, s, i1 - 1f, j1 + 1f, colorBG2);
            }
            fontRenderer.drawString(matrixStack, s, i1 + 1, j1, colorBG);
            fontRenderer.drawString(matrixStack, s, i1 - 1, j1, colorBG);
            fontRenderer.drawString(matrixStack, s, i1, j1 + 1, colorBG);
            fontRenderer.drawString(matrixStack, s, i1, j1 - 1, colorBG);
            fontRenderer.drawString(matrixStack, s, i1, j1, color);
        }
    }
}

