package com.momosoftworks.coldsweat.api.event.vanilla;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraftforge.eventbus.api.Event;

public class RenderLevelEvent extends Event
{
    MatrixStack poseStack;
    float partialTick;

    public RenderLevelEvent(MatrixStack poseStack, float partialTick)
    {
        this.poseStack = poseStack;
        this.partialTick = partialTick;
    }

    public MatrixStack getMatrixStack()
    {   return poseStack;
    }

    public float getPartialTick()
    {   return partialTick;
    }

    /**
     * Fired before any level rendering is done in {@link net.minecraft.client.renderer.GameRenderer#renderLevel(float, long, MatrixStack)}.<br>
     * This event is not {@link net.minecraftforge.eventbus.api.Cancelable}.
     */
    public static class Pre extends RenderLevelEvent
    {
        public Pre(MatrixStack poseStack, float partialTick)
        {   super(poseStack, partialTick);
        }
    }

    /**
     * Fired after the level is rendered in {@link net.minecraft.client.renderer.GameRenderer#renderLevel(float, long, MatrixStack)}.<br>
     */
    public static class Post extends RenderLevelEvent
    {
        public Post(MatrixStack poseStack, float partialTick)
        {   super(poseStack, partialTick);
        }
    }
}
