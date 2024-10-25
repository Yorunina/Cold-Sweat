package com.momosoftworks.coldsweat.api.temperature.modifier.compat;

import com.momosoftworks.coldsweat.api.temperature.modifier.BlockTempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ValkShipBlockTempModifier extends BlockTempModifier
{
    public ValkShipBlockTempModifier() {}

    public ValkShipBlockTempModifier(int range)
    {   super(range);
    }

    @Override
    public Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait)
    {
        List<Function<Double, Double>> shipModifiers = new ArrayList<>();

        Level level = entity.level();

        for (Ship ship : VSGameUtilsKt.getShipsIntersecting(level, entity.getBoundingBox().inflate(ConfigSettings.BLOCK_RANGE.get())))
        {
            LivingEntity dummyPlayer = new ArmorStand(EntityType.ARMOR_STAND, level);
            Vec3 translatedPos = translateToShipCoords(entity.position(), ship).multiply(1, 1, 1);
            dummyPlayer.setPos(translatedPos.x, translatedPos.y, translatedPos.z);
            shipModifiers.add(super.calculate(dummyPlayer, trait));
        }
        return (temp) ->
        {
            for (int i = 0; i < shipModifiers.size(); i++)
            {   temp = shipModifiers.get(i).apply(temp);
            }
            return temp;
        };
    }

    protected Vec3 translateToShipCoords(Vec3 pos, Ship ship)
    {
        if (ship != null)
        {
            Vector3d posVec = VectorConversionsMCKt.toJOML(pos);
            ship.getWorldToShip().transformPosition(posVec);
            return VectorConversionsMCKt.toMinecraft(posVec);
        }
        return pos;
    }
}