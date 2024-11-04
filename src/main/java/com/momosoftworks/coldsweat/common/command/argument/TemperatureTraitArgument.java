package com.momosoftworks.coldsweat.common.command.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.Codec;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.util.serialization.ObjectBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.StringRepresentableArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.Locale;

public class TemperatureTraitArgument extends StringRepresentableArgument<Temperature.Trait>
{
    private static final Temperature.Trait[] VALID_GETTER_TRAITS = ObjectBuilder.build(() -> {
        Temperature.Trait[] traits = Arrays.copyOf(EntityTempManager.VALID_TEMPERATURE_TRAITS, EntityTempManager.VALID_TEMPERATURE_TRAITS.length + 1);
        traits[traits.length - 1] = Temperature.Trait.BODY;
        return traits;
    });

    private static final Codec<Temperature.Trait> TEMPERATURES_CODEC_SETTER = StringRepresentable.fromEnum(() -> EntityTempManager.VALID_TEMPERATURE_TRAITS);
    private static final Codec<Temperature.Trait> TEMPERATURES_CODEC_GETTER = StringRepresentable.fromEnum(() -> {
        Temperature.Trait[] traits = Arrays.copyOf(EntityTempManager.VALID_TEMPERATURE_TRAITS, EntityTempManager.VALID_TEMPERATURE_TRAITS.length + 1);
        traits[traits.length - 1] = Temperature.Trait.BODY;
        return traits;
    });

    private final boolean includeBody;

    private TemperatureTraitArgument(boolean includeBody)
    {
        super(includeBody ? TEMPERATURES_CODEC_GETTER : TEMPERATURES_CODEC_SETTER,
              includeBody ? () -> VALID_GETTER_TRAITS : () -> EntityTempManager.VALID_TEMPERATURE_TRAITS);
        this.includeBody = includeBody;
    }

    public static TemperatureTraitArgument temperatureGet()
    {   return new TemperatureTraitArgument(true);
    }

    public static TemperatureTraitArgument temperatureSet()
    {   return new TemperatureTraitArgument(false);
    }

    public static Temperature.Trait getTemperature(CommandContext<CommandSourceStack> context, String argument)
    {   return context.getArgument(argument, Temperature.Trait.class);
    }

    protected String convertId(String Id)
    {   return Id.toLowerCase(Locale.ROOT);
    }

    public static class Info implements ArgumentTypeInfo<TemperatureTraitArgument, Info.Template>
    {
        @Override
        public void serializeToNetwork(Template template, FriendlyByteBuf buffer)
        {
            buffer.writeBoolean(template.includeBody);
        }

        @Override
        public Template deserializeFromNetwork(FriendlyByteBuf buffer)
        {   return new Template(buffer.readBoolean());
        }

        @Override
        public void serializeToJson(Template template, JsonObject json)
        {   json.addProperty("include_body", template.includeBody);
        }

        @Override
        public Template unpack(TemperatureTraitArgument argument)
        {   return new Template(argument.includeBody);
        }

        public final class Template implements ArgumentTypeInfo.Template<TemperatureTraitArgument>
        {
            private final boolean includeBody;

            public Template(boolean includeBody)
            {   this.includeBody = includeBody;
            }

            @Override
            public TemperatureTraitArgument instantiate(CommandBuildContext context)
            {   return new TemperatureTraitArgument(includeBody);
            }

            @Override
            public ArgumentTypeInfo<TemperatureTraitArgument, ?> type()
            {   return Info.this;
            }
        }
    }
}
