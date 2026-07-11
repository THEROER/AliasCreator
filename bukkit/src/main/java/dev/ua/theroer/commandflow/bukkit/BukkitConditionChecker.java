package dev.ua.theroer.commandflow.bukkit;

import java.lang.reflect.Method;

import dev.ua.theroer.commandflow.common.condition.ConditionChecker;
import dev.ua.theroer.magicutils.commands.MagicSender;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.Nullable;

/**
 * Bukkit {@link ConditionChecker}: matches worlds by name and charges economy
 * cost through Vault when present.
 *
 * <p>Vault is a soft dependency accessed reflectively, so CommandFlow compiles
 * and runs without the Vault API on the classpath. When Vault (or an economy
 * provider) is absent, {@link #chargeCost} succeeds without charging, so cost
 * conditions are effectively skipped rather than blocking the command.</p>
 */
public final class BukkitConditionChecker implements ConditionChecker {
    private final Object economy; // net.milkbowl.vault.economy.Economy, or null
    private final Method hasMethod;
    private final Method withdrawMethod;

    /**
     * Creates the checker, resolving the Vault economy service if available.
     */
    public BukkitConditionChecker() {
        Object resolvedEconomy = null;
        Method has = null;
        Method withdraw = null;
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> provider = Bukkit.getServicesManager().getRegistration(economyClass);
            if (provider != null) {
                resolvedEconomy = provider.getProvider();
                has = economyClass.getMethod("has", OfflinePlayer.class, double.class);
                withdraw = economyClass.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
            }
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            // Vault not installed; economy stays null and cost is skipped.
        }
        this.economy = resolvedEconomy;
        this.hasMethod = has;
        this.withdrawMethod = withdraw;
    }

    @Override
    public boolean inWorld(MagicSender sender, String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return true;
        }
        Player player = unwrapPlayer(sender);
        return player != null && worldName.equalsIgnoreCase(player.getWorld().getName());
    }

    @Override
    public boolean chargeCost(MagicSender sender, double amount) {
        if (economy == null || amount <= 0) {
            return true; // no economy provider, or nothing to charge
        }
        Player player = unwrapPlayer(sender);
        if (player == null) {
            return true;
        }
        try {
            Object affords = hasMethod.invoke(economy, player, amount);
            if (affords instanceof Boolean canAfford && !canAfford) {
                return false;
            }
            Object response = withdrawMethod.invoke(economy, player, amount);
            return isTransactionSuccess(response);
        } catch (ReflectiveOperationException error) {
            return true; // fail open: don't block the command on an economy glitch
        }
    }

    private static boolean isTransactionSuccess(@Nullable Object response) {
        if (response == null) {
            return true;
        }
        try {
            Method transactionSuccess = response.getClass().getMethod("transactionSuccess");
            Object result = transactionSuccess.invoke(response);
            return !(result instanceof Boolean success) || success;
        } catch (ReflectiveOperationException ignored) {
            return true;
        }
    }

    private static Player unwrapPlayer(MagicSender sender) {
        Object handle = sender != null ? sender.handle() : null;
        return handle instanceof Player player ? player : null;
    }
}
