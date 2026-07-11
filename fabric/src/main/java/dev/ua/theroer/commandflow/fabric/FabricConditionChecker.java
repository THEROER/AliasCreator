package dev.ua.theroer.commandflow.fabric;

import dev.ua.theroer.commandflow.common.condition.ConditionChecker;
import dev.ua.theroer.magicutils.commands.MagicSender;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric {@link ConditionChecker}: matches worlds by dimension id. Fabric has no
 * standard economy, so {@link #chargeCost} is a no-op that always succeeds (cost
 * conditions are effectively skipped on Fabric).
 */
public final class FabricConditionChecker implements ConditionChecker {
    @Override
    public boolean inWorld(MagicSender sender, String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return true;
        }
        ServerPlayer player = unwrapPlayer(sender);
        if (player == null) {
            return false;
        }
        Identifier dimension = player.level().dimension().identifier();
        return worldName.equalsIgnoreCase(dimension.getPath())
                || worldName.equalsIgnoreCase(dimension.toString());
    }

    @Override
    public boolean chargeCost(MagicSender sender, double amount) {
        return true; // no economy on Fabric
    }

    private static ServerPlayer unwrapPlayer(MagicSender sender) {
        Object handle = sender != null ? sender.handle() : null;
        if (handle instanceof CommandSourceStack source) {
            return source.getEntity() instanceof ServerPlayer player ? player : null;
        }
        return handle instanceof ServerPlayer player ? player : null;
    }
}
