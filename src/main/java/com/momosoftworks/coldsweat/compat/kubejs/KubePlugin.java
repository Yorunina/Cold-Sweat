package com.momosoftworks.coldsweat.compat.kubejs;

import com.momosoftworks.coldsweat.api.event.common.temperautre.TemperatureChangedEvent;
import dev.architectury.event.EventResult;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class KubePlugin extends KubeJSPlugin
{
    @Override
    public void registerEvents()
    {
        KubeEventHandlers.init();
    }

    @Override
    public void registerBindings(BindingsEvent event)
    {
        event.add("coldsweat", new KubeBindings());
    }

    @SubscribeEvent
    public static void onTempChanged(TemperatureChangedEvent event)
    {
        LivingEntity entity = event.getEntity();
        EventResult result = KubeEventSignatures.TEMPERATURE_CHANGED.invoker().onTemperatureChanged(entity, event.getTrait(), event.getOldTemperature(), event.getTemperature());
        if (result.isTrue())
        {   event.setCanceled(true);
        }
    }
}
