package net.minecraft.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class CommandSourceStack {
    public ServerPlayer getPlayerOrException() throws CommandSyntaxException { throw null; }
    public void sendSuccess(Supplier<Component> message, boolean broadcast) { throw null; }
    public void sendFailure(Component message) { throw null; }
    public boolean hasPermission(int level) { throw null; }
}
