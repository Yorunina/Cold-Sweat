package com.momosoftworks.coldsweat.common.command.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.Codec;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.StringRepresentableArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Locale;

public class AttributeOperationArgument extends StringRepresentableArgument<AttributeModifier.Operation>
{
    private AttributeOperationArgument()
    {   super(AttributeModifier.Operation.CODEC, AttributeModifier.Operation::values);
    }

    public static AttributeOperationArgument operation()
    {   return new AttributeOperationArgument();
    }

    public static AttributeModifier.Operation getOperation(CommandContext<CommandSourceStack> context, String argument)
    {   return context.getArgument(argument, AttributeModifier.Operation.class);
    }

    protected String convertId(String id)
    {   return id.toLowerCase(Locale.ROOT);
    }

    public static class Info implements ArgumentTypeInfo<AttributeOperationArgument, Info.Template>
    {
        @Override
        public void serializeToNetwork(Template template, FriendlyByteBuf buffer)
        {
        }

        @Override
        public Template deserializeFromNetwork(FriendlyByteBuf buffer)
        {   return new Template();
        }

        @Override
        public void serializeToJson(Template template, JsonObject json)
        {
        }

        @Override
        public Template unpack(AttributeOperationArgument argument)
        {   return new Template();
        }

        public final class Template implements ArgumentTypeInfo.Template<AttributeOperationArgument>
        {
            @Override
            public AttributeOperationArgument instantiate(CommandBuildContext pContext)
            {   return new AttributeOperationArgument();
            }

            @Override
            public ArgumentTypeInfo<AttributeOperationArgument, ?> type()
            {   return Info.this;
            }
        }
    }
}
