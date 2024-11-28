package com.momosoftworks.coldsweat.mixin;

import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BeaconBlockEntity.class)
public class MixinBeaconEffects
{
    /*static
    {
        // get the current top-level effects as a mutable list
        List<MobEffect> effects = new ArrayList(Arrays.asList(BEACON_EFFECTS[3]));
        // add the insulation effect
        effects.add(ModEffects.INSULATION);
        // set the top-level effects to the new list
        BEACON_EFFECTS[3] = effects.toArray(new MobEffect[0]);
    }

    @ModifyArg(method = "applyEffects(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/world/effect/MobEffect;Lnet/minecraft/world/effect/MobEffect;)V",
               at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z", ordinal = 1))
    private static MobEffectInstance modifyEffect(MobEffectInstance effect)
    {
        if (effect.getEffect() == ModEffects.INSULATION)
        {   return new MobEffectInstance(effect.getEffect(), effect.getDuration(), 4, effect.isAmbient(), effect.isVisible(), effect.showIcon());
        }
        return effect;
    }*/
}
