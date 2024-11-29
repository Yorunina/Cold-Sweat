package com.momosoftworks.coldsweat.mixin;

import com.momosoftworks.coldsweat.config.ConfigSettings;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Gui.class)
public class MixinXPBar
{
    @Inject(method = "renderExperienceBar",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I",
                     ordinal = 0))
    public void shiftExperienceBar(GuiGraphics graphics, int x, CallbackInfo ci)
    {
        graphics.pose().pushPose();
        // Render XP bar
        if (ConfigSettings.CUSTOM_HOTBAR_LAYOUT.get())
        {   graphics.pose().translate(0, 4, 0);
        }
    }

    @Inject(method = "renderExperienceBar",
            at = @At
            (   value = "INVOKE",
                target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V",
                ordinal = 0
            ),
            slice = @Slice
            (   from = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;width(Ljava/lang/String;)I"),
                to   = @At(value = "RETURN")
            ))
    public void experienceBarPop(GuiGraphics graphics, int px, CallbackInfo ci)
    {
        graphics.pose().popPose();
    }

    @Mixin(Gui.class)
    public static class MixinItemLabel
    {
        @Inject(method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;I)V",
                at = @At(value = "HEAD"), remap = false)
        public void shiftItemName(GuiGraphics graphics, int height, CallbackInfo ci)
        {
            graphics.pose().pushPose();
            if (ConfigSettings.CUSTOM_HOTBAR_LAYOUT.get())
            {   graphics.pose().translate(0, -4, 0);
            }
        }

        @Inject(method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;I)V",
                at = @At(value = "TAIL"), remap = false)
        public void itemNamePop(GuiGraphics graphics, int height, CallbackInfo ci)
        {
            graphics.pose().popPose();
        }
    }
}
