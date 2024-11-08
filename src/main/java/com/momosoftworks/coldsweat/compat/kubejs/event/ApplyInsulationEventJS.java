package com.momosoftworks.coldsweat.compat.kubejs.event;

import com.momosoftworks.coldsweat.api.event.common.insulation.InsulateItemEvent;
import dev.latvian.mods.kubejs.player.PlayerEventJS;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ApplyInsulationEventJS extends PlayerEventJS
{
    private final InsulateItemEvent event;

    public ApplyInsulationEventJS(InsulateItemEvent event)
    {
        this.event = event;
    }

    @Override
    public Player getEntity()
    {   return event.getPlayer();
    }

    public Player getPlayer()
    {   return event.getPlayer();
    }

    public ItemStack getArmorItem()
    {   return event.getArmorItem();
    }

    public ItemStack getInsulator()
    {   return event.getInsulator();
    }

    public void setInsulator(ItemStack insulator)
    {   event.setInsulator(insulator);
    }
}
