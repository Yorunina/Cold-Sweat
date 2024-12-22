package com.momosoftworks.coldsweat.compat.kubejs;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.api.util.Temperature.Trait;
import com.momosoftworks.coldsweat.common.capability.temperature.ITemperatureCap;
import dev.latvian.mods.kubejs.level.LevelEventJS;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class TempChangedEventJS extends LevelEventJS {
    public LivingEntity entity;
    public Trait trait;
    public double temperature;
    public double oldTemperature;
    public ITemperatureCap temperatureCap;
    public Level level;


    public TempChangedEventJS(ITemperatureCap temperatureCap, LivingEntity entity, Trait trait, double value, double oldTemp) {
        this.entity = entity;
        this.trait = trait;
        this.temperature = value;
        this.oldTemperature = oldTemp;
        this.level = entity.level;
        this.temperatureCap = temperatureCap;
    }

    @Override
    public Level getLevel() {
        return this.level;
    }

    public LivingEntity getEntity() {
        return this.entity;
    }

    public Temperature.Trait getTrait() {
        return this.trait;
    }

    public double getOldTemperature() {
        return this.oldTemperature;
    }

    public double getTemperature() {
        return this.temperature;
    }
    public ITemperatureCap getTemperatureCap() {
        return this.temperatureCap;
    }

    public void setTemperature(double newTemperature) {
        this.temperatureCap.setTrait(this.trait, newTemperature);
    }

}
