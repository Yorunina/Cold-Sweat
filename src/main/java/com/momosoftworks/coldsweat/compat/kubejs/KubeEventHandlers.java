package com.momosoftworks.coldsweat.compat.kubejs;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.compat.kubejs.event.TempChangedEventJS;
import dev.architectury.event.EventResult;
import dev.latvian.mods.kubejs.bindings.event.EntityEvents;
import dev.latvian.mods.kubejs.event.EventHandler;
import net.minecraft.world.entity.LivingEntity;


public class KubeEventHandlers
{
    public static final EventHandler TEMP_CHANGED = EntityEvents.GROUP.server("cs:temperatureChanged", () -> TempChangedEventJS.class);

    public static void init()
    {
        KubeEventSignatures.TEMPERATURE_CHANGED.register(KubeEventHandlers::onTemperatureChanged);
    }

    private static EventResult onTemperatureChanged(LivingEntity entity, Temperature.Trait trait, double oldTemperature, double newTemperature)
    {
        if (TEMP_CHANGED.hasListeners())
        {   return TEMP_CHANGED.post(new TempChangedEventJS(entity, trait, oldTemperature, newTemperature)).arch();
        }
        return EventResult.pass();
    }
}
