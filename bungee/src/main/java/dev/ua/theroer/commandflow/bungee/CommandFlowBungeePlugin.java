package dev.ua.theroer.commandflow.bungee;

import dev.ua.theroer.commandflow.common.ProxyForwarder;
import dev.ua.theroer.commandflow.common.condition.CompareOperatorAdapter;
import dev.ua.theroer.commandflow.common.config.CommandFlowConfig;
import dev.ua.theroer.commandflow.common.remote.MessagingRemoteDispatcher;
import dev.ua.theroer.commandflow.common.remote.RemoteDispatcher;
import dev.ua.theroer.magicutils.bootstrap.BungeeBootstrap;
import dev.ua.theroer.magicutils.bootstrap.MagicRuntime;
import dev.ua.theroer.magicutils.commands.CommandRegistry;
import dev.ua.theroer.magicutils.config.ConfigManager;
import dev.ua.theroer.magicutils.messaging.MessagingService;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * CommandFlow proxy plugin for BungeeCord/Waterfall. Forwards alias invocations
 * to the player's backend server over the MagicUtils message bus; the backend
 * runs the alias's actions and conditions.
 */
public final class CommandFlowBungeePlugin extends Plugin {
    private MagicRuntime runtime;

    @Override
    public void onEnable() {
        BungeeBootstrap.RuntimeResult bootstrap = BungeeBootstrap
                .forPlugin(this)
                .logger(getLogger())
                .permissionPrefix("commandflow")
                .enableCommands()
                .enableMessaging()
                .buildRuntime();
        runtime = bootstrap.runtime();

        ConfigManager configManager = bootstrap.configManager();
        CompareOperatorAdapter.register();
        CommandFlowConfig config = configManager.register(CommandFlowConfig.class);
        CommandRegistry commandRegistry = bootstrap.commandRegistry();

        RemoteDispatcher remote = runtime.findComponent(MessagingService.class)
                .map(messaging -> (RemoteDispatcher) new MessagingRemoteDispatcher(messaging, messaging.bus().self().id()))
                .orElse(RemoteDispatcher.DISABLED);

        if (commandRegistry != null) {
            ProxyForwarder forwarder = new ProxyForwarder(remote);
            BungeeProxyAliasRegistrar registrar =
                    new BungeeProxyAliasRegistrar(commandRegistry, forwarder);
            registrar.applyAliases(config.getAliases());
        }

        getLogger().info("CommandFlow Bungee proxy module loaded.");
    }

    @Override
    public void onDisable() {
        if (runtime != null) {
            runtime.close();
        }
    }
}
