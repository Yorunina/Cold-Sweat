package net.momostudios.coldsweat.core.network.message;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import net.momostudios.coldsweat.core.capabilities.PlayerTempCapability;
import net.momostudios.coldsweat.util.PlayerHelper;

import java.util.function.Supplier;

public class PlayerTempSyncMessage
{
    public double body;
    public double base;
    public double ambient;

    public PlayerTempSyncMessage() {
    }

    public PlayerTempSyncMessage(double body, double base, double ambient){
        this.body = body;
        this.base = base;
        this.ambient = ambient;
    }

    public static void encode(PlayerTempSyncMessage message, PacketBuffer buffer)
    {
        buffer.writeDouble(message.body);
        buffer.writeDouble(message.base);
        buffer.writeDouble(message.ambient);
    }

    public static PlayerTempSyncMessage decode(PacketBuffer buffer)
    {
        return new PlayerTempSyncMessage(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(PlayerTempSyncMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> syncTemperature(message.body, message.base, message.ambient)));

        context.setPacketHandled(true);
    }

    public static DistExecutor.SafeRunnable syncTemperature(double body, double base, double ambient)
    {
        return new DistExecutor.SafeRunnable()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void run()
            {
                ClientPlayerEntity player = Minecraft.getInstance().player;

                if (player != null && !player.isSpectator())
                {
                    player.getCapability(PlayerTempCapability.TEMPERATURE).ifPresent(cap ->
                    {
                        cap.set(PlayerHelper.Types.BODY, body);
                        cap.set(PlayerHelper.Types.BASE, base);
                        cap.set(PlayerHelper.Types.COMPOSITE, body + base);
                        cap.set(PlayerHelper.Types.AMBIENT, ambient);
                    });
                }
            }
        };
    }
}
