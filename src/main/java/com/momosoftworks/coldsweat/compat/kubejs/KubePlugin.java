package com.momosoftworks.coldsweat.compat.kubejs;

import com.momosoftworks.coldsweat.api.event.common.insulation.InsulateItemEvent;
import com.momosoftworks.coldsweat.api.event.common.temperautre.TempModifierEvent;
import com.momosoftworks.coldsweat.api.event.common.temperautre.TemperatureChangedEvent;
import com.momosoftworks.coldsweat.api.event.core.registry.CreateRegistriesEvent;
import dev.architectury.event.EventResult;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class KubePlugin extends KubeJSPlugin
{
    @Override
    public void registerEvents() {
        KubeEventHandlers.COLD_SWEAT_GROUP.register();
    }

    @Override
    public void registerBindings(BindingsEvent event)
    {
        event.add("ColdSweat", new KubeBindings());
    }
}
