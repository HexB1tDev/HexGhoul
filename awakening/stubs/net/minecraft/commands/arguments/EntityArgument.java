package net.minecraft.commands.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public class EntityArgument implements ArgumentType<Object> {
    public static EntityArgument player() { throw null; }
    public static ServerPlayer getPlayer(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException { throw null; }
}
