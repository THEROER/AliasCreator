package dev.ua.theroer.commandflow.common.condition;

import dev.ua.theroer.magicutils.commands.MagicSender;

/**
 * Platform-specific condition checks that {@code common} cannot perform on its
 * own: the player's world, and economy cost (charging the player).
 *
 * <p>Permission, argument, and placeholder-compare conditions are handled in
 * common via {@link MagicSender} and MagicUtils placeholders; only the checks
 * below need a platform. A platform without economy (Fabric, or Bukkit without
 * Vault) returns {@code true} from {@link #chargeCost} so a cost condition is
 * treated as satisfied and skipped.</p>
 */
public interface ConditionChecker {
    /**
     * A checker that has no world/economy support: any world matches and cost is
     * always considered paid. Used as a safe default.
     */
    ConditionChecker PERMISSIVE = new ConditionChecker() {
        @Override
        public boolean inWorld(MagicSender sender, String worldName) {
            return true;
        }

        @Override
        public boolean chargeCost(MagicSender sender, double amount) {
            return true;
        }
    };

    /**
     * Returns whether the sender is a player currently in the named world.
     *
     * @param sender invoking sender
     * @param worldName world name to match (case-insensitive)
     * @return true when in that world (or when world checks are unsupported)
     */
    boolean inWorld(MagicSender sender, String worldName);

    /**
     * Charges the player the given economy amount, returning whether it
     * succeeded. Implementations without economy return {@code true} (no charge,
     * treated as satisfied).
     *
     * @param sender invoking sender
     * @param amount amount to charge
     * @return true when charged (or economy unsupported); false when unaffordable
     */
    boolean chargeCost(MagicSender sender, double amount);
}
