package com.momosoftworks.coldsweat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.momosoftworks.coldsweat.api.event.vanilla.RenderLevelEvent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinAfterLevelRender
{
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void beforeLevelRender(float partialTick, long finishTimeNano, PoseStack poseStack, CallbackInfo ci)
    {   MinecraftForge.EVENT_BUS.post(new RenderLevelEvent.Pre(poseStack, partialTick));
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void afterLevelRender(float partialTick, long finishTimeNano, PoseStack poseStack, CallbackInfo ci)
    {   MinecraftForge.EVENT_BUS.post(new RenderLevelEvent.Post(poseStack, partialTick));
    }
}
