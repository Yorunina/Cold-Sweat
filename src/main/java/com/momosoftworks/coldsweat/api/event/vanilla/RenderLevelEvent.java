package com.momosoftworks.coldsweat.api.event.vanilla;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.eventbus.api.Event;

public class RenderLevelEvent extends Event
{
    PoseStack poseStack;
    float partialTick;

    public RenderLevelEvent(PoseStack poseStack, float partialTick)
    {
        this.poseStack = poseStack;
        this.partialTick = partialTick;
    }

    public PoseStack getPoseStack()
    {   return poseStack;
    }

    public float getPartialTick()
    {   return partialTick;
    }

    /**
     * Fired before any level rendering is done in {@link net.minecraft.client.renderer.GameRenderer#renderLevel(float, long, PoseStack)}.<br>
     * This event is not {@link net.minecraftforge.eventbus.api.Cancelable}.
     */
    public static class Pre extends RenderLevelEvent
    {
        public Pre(PoseStack poseStack, float partialTick)
        {   super(poseStack, partialTick);
        }
    }

    /**
     * Fired after the level is rendered in {@link net.minecraft.client.renderer.GameRenderer#renderLevel(float, long, PoseStack)}.<br>
     */
    public static class Post extends RenderLevelEvent
    {
        public Post(PoseStack poseStack, float partialTick)
        {   super(poseStack, partialTick);
        }
    }
}
