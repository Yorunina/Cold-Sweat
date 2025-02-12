package com.momosoftworks.coldsweat.compat.kubejs.event;

import com.momosoftworks.coldsweat.api.event.common.temperautre.TemperatureChangedEvent;
import com.momosoftworks.coldsweat.api.util.Temperature;
import dev.latvian.mods.kubejs.entity.LivingEntityEventJS;
import net.minecraft.world.entity.LivingEntity;

public class TempChangedEventJS extends LivingEntityEventJS
{
    private final TemperatureChangedEvent event;

    public TempChangedEventJS(TemperatureChangedEvent event)
    {   this.event = event;
    }

    @Override
    public LivingEntity getEntity()
    {   return event.getEntity();
    }

    public Temperature.Trait getTrait()
    {   return event.getTrait();
    }

    public double getOldTemperature()
    {   return event.getOldTemperature();
    }

    public double getTemperature()
    {   return event.getTemperature();
    }

    public void setTemperature(double newTemperature)
    {   event.setTemperature(newTemperature);
    }
}
