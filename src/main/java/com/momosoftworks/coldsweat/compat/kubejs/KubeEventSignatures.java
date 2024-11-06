package com.momosoftworks.coldsweat.compat.kubejs;

import com.momosoftworks.coldsweat.api.util.Temperature;
import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.architectury.event.EventResult;
import net.minecraft.world.entity.LivingEntity;

public interface KubeEventSignatures
{
    Event<KubeEventSignatures.TemperatureChanged> TEMPERATURE_CHANGED = EventFactory.createEventResult();

    interface TemperatureChanged
    {
        EventResult onTemperatureChanged(LivingEntity entity, Temperature.Trait trait, double oldTemperature, double newTemperature);
    }
}
