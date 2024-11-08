package com.momosoftworks.coldsweat.compat.kubejs.event;

import com.momosoftworks.coldsweat.api.event.common.insulation.InsulateItemEvent;
import dev.latvian.mods.kubejs.player.KubePlayerEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ApplyInsulationEventJS implements KubePlayerEvent
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
