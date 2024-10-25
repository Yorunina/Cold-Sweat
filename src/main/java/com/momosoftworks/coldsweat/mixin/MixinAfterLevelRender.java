package com.momosoftworks.coldsweat.mixin;

import com.momosoftworks.coldsweat.api.event.vanilla.RenderLevelEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinAfterLevelRender
{
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void beforeLevelRender(DeltaTracker deltaTracker, CallbackInfo ci)
    {   NeoForge.EVENT_BUS.post(new RenderLevelEvent.Pre(deltaTracker));
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void afterLevelRender(DeltaTracker deltaTracker, CallbackInfo ci)
    {   NeoForge.EVENT_BUS.post(new RenderLevelEvent.Post(deltaTracker));
    }
}
