package com.momosoftworks.coldsweat.core.network.message;

import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

public class ParticleBatchMessage
{
    private static final BinaryOperator<Vec3> MIN_POS_COMPARATOR = (a, b) -> new Vec3(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z));
    private static final BinaryOperator<Vec3> MAX_POS_COMPARATOR = (a, b) -> new Vec3(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));

    Set<Pair<ParticleOptions, ParticlePlacement>> particles = new HashSet<>();
    int minSetting;

    /**
     * @param minSetting The minimum particle setting for the particles to render.<br>
     * 0: All<br>
     * 1: Decreased<br>
     * 2: Minimal<br>
     */
    public ParticleBatchMessage(int minSetting)
    {   this.minSetting = minSetting;
    }

    public ParticleBatchMessage()
    {   this(-1);
    }

    public ParticleBatchMessage addParticle(ParticleOptions particle, ParticlePlacement placement)
    {   particles.add(Pair.of(particle, placement));
        return this;
    }

    public ParticleBatchMessage addParticle(ParticleOptions particle, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed)
    {   addParticle(particle, new ParticlePlacement(x, y, z, xSpeed, ySpeed, zSpeed));
        return this;
    }

    public void sendEntity(Entity entity)
    {
        if (particles.isEmpty() || entity.level.isClientSide()) return;
        ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), this);
    }

    public void sendWorld(Level level)
    {
        if (particles.isEmpty() || level.isClientSide()) return;
        Vec3 minPos = particles.stream().map(Pair::getSecond).map(p -> new Vec3(p.x, p.y, p.z)).reduce(MIN_POS_COMPARATOR).get();
        Vec3 maxPos = particles.stream().map(Pair::getSecond).map(p -> new Vec3(p.x, p.y, p.z)).reduce(MAX_POS_COMPARATOR).get();
        Vec3 midPos = minPos.add(maxPos).scale(0.5);
        PacketDistributor.TargetPoint target =  new PacketDistributor.TargetPoint(midPos.x, midPos.y, midPos.z, minPos.distanceTo(maxPos) + 32, level.dimension());
        ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.NEAR.with(() -> target), this);
    }

    public static void encode(ParticleBatchMessage message, FriendlyByteBuf buffer)
    {
        buffer.writeInt(message.minSetting);
        buffer.writeInt(message.particles.size());
        for (Pair<ParticleOptions, ParticlePlacement> entry : message.particles)
        {
            String particleID = ForgeRegistries.PARTICLE_TYPES.getKey(entry.getFirst().getType()).toString();
            buffer.writeInt(particleID.length());
            buffer.writeCharSequence(particleID, StandardCharsets.UTF_8);
            entry.getFirst().writeToNetwork(buffer);
            buffer.writeNbt(entry.getSecond().toNBT());
        }
    }

    public static ParticleBatchMessage decode(FriendlyByteBuf buffer)
    {
        ParticleBatchMessage message = new ParticleBatchMessage(buffer.readInt());
        int size = buffer.readInt();
        for (int i = 0; i < size; i++)
        {
            int particleIDLength = buffer.readInt();
            ParticleType type = ForgeRegistries.PARTICLE_TYPES.getValue(new ResourceLocation(buffer.readCharSequence(particleIDLength, StandardCharsets.UTF_8).toString()));
            ParticleOptions particle = type.getDeserializer().fromNetwork(type, buffer);
            ParticlePlacement placement = ParticlePlacement.fromNBT(buffer.readNbt());
            message.addParticle(particle, placement);
        }

        return message;
    }

    public static void handle(ParticleBatchMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient())
        context.enqueueWork(() ->
        {
            for (Pair<ParticleOptions, ParticlePlacement> entry : message.particles)
            {
                ParticleOptions particle = entry.getFirst();
                ParticlePlacement placement = entry.getSecond();

                if (message.minSetting == -1 || Minecraft.getInstance().options.particles().get().getId() <= message.minSetting)
                {
                    Minecraft.getInstance().level.addParticle(particle, placement.x, placement.y, placement.z, placement.vx, placement.vy, placement.vz);
                }
            }
        });
        context.setPacketHandled(true);
    }

    public static class ParticlePlacement
    {
        double x, y, z, vx, vy, vz;

        public ParticlePlacement(double x, double y, double z, double vx, double vy, double vz)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
        }

        public CompoundTag toNBT()
        {
            CompoundTag tag = new CompoundTag();
            tag.putDouble("x", x);
            tag.putDouble("y", y);
            tag.putDouble("z", z);
            tag.putDouble("vx", vx);
            tag.putDouble("vy", vy);
            tag.putDouble("vz", vz);
            return tag;
        }

        public static ParticlePlacement fromNBT(CompoundTag tag)
        {
            return new ParticlePlacement(
                    tag.getDouble("x"),
                    tag.getDouble("y"),
                    tag.getDouble("z"),
                    tag.getDouble("vx"),
                    tag.getDouble("vy"),
                    tag.getDouble("vz")
            );
        }
    }
}
