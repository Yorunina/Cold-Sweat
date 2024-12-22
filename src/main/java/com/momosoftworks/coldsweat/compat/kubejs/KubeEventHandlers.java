package com.momosoftworks.coldsweat.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;


public class KubeEventHandlers
{
    public static final EventGroup COLD_SWEAT_GROUP = EventGroup.of("ColdSweatEvents");
    public static final EventHandler TEMP_CHANGED_EVENT = COLD_SWEAT_GROUP.server("temperatureChanged", () -> TempChangedEventJS.class);
}
