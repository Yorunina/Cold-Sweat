package com.momosoftworks.coldsweat.api.event.vanilla;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.neoforged.bus.api.Event;

public class RenderLevelEvent extends Event
{
    DeltaTracker deltaTracker;

    public RenderLevelEvent(DeltaTracker deltaTracker)
    {   this.deltaTracker = deltaTracker;
    }

    public DeltaTracker getDeltaTracker()
    {   return this.deltaTracker;
    }

    public float getPartialTick()
    {   return this.deltaTracker.getGameTimeDeltaPartialTick(true);
    }

    /**
     * Fired before any level rendering is done in {@link net.minecraft.client.renderer.GameRenderer#renderLevel(DeltaTracker)}.<br>
     * This event is not {@link net.neoforged.bus.api.ICancellableEvent}
     */
    public static class Pre extends RenderLevelEvent
    {
        public Pre(DeltaTracker deltaTracker)
        {   super(deltaTracker);
        }
    }

    /**
     * Fired after the level is rendered in {@link net.minecraft.client.renderer.GameRenderer#renderLevel(DeltaTracker)}.<br>
     */
    public static class Post extends RenderLevelEvent
    {
        public Post(DeltaTracker deltaTracker)
        {   super(deltaTracker);
        }
    }
}
