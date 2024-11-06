package com.momosoftworks.coldsweat.util.compat.kubejs;

import com.momosoftworks.coldsweat.api.event.common.temperautre.TemperatureChangedEvent;
import dev.architectury.event.EventResult;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;

public class KubePlugin implements KubeJSPlugin
{
    @Override
    public void registerEvents(EventGroupRegistry registry)
    {
        KubeEventHandlers.init();
    }

    @Override
    public void registerBindings(BindingRegistry bindings)
    {
        bindings.add("coldsweat", new KubeBindings());
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
