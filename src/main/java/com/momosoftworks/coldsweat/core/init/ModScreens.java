package com.momosoftworks.coldsweat.core.init;

import com.momosoftworks.coldsweat.client.gui.BoilerScreen;
import com.momosoftworks.coldsweat.client.gui.HearthScreen;
import com.momosoftworks.coldsweat.client.gui.IceboxScreen;
import com.momosoftworks.coldsweat.client.gui.SewingScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModScreens
{
    @SubscribeEvent
    public static void clientSetup(RegisterMenuScreensEvent event)
    {
        event.register(ModMenus.BOILER_CONTAINER_TYPE.get(), BoilerScreen::new);
        event.register(ModMenus.ICEBOX_CONTAINER_TYPE.get(), IceboxScreen::new);
        event.register(ModMenus.SEWING_CONTAINER_TYPE.get(), SewingScreen::new);
        event.register(ModMenus.HEARTH_CONTAINER_TYPE.get(), HearthScreen::new);
    }
}