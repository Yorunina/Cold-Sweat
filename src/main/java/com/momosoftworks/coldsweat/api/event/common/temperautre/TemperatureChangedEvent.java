package com.momosoftworks.coldsweat.api.event.common.temperautre;

import com.momosoftworks.coldsweat.api.util.Temperature;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class TemperatureChangedEvent extends Event implements ICancellableEvent
{
    private final LivingEntity entity;
    private final Temperature.Trait trait;
    private final double oldTemperature;
    private double newTemperature;

    public TemperatureChangedEvent(LivingEntity entity, Temperature.Trait trait, double oldTemperature, double newTemperature)
    {
        this.entity = entity;
        this.trait = trait;
        this.oldTemperature = oldTemperature;
        this.newTemperature = newTemperature;
    }

    public LivingEntity getEntity()
    {   return entity;
    }

    public Temperature.Trait getTrait()
    {   return trait;
    }

    public double getOldTemperature()
    {   return oldTemperature;
    }

    public double getTemperature()
    {   return newTemperature;
    }

    public void setTemperature(double newTemperature)
    {   this.newTemperature = newTemperature;
    }
}
