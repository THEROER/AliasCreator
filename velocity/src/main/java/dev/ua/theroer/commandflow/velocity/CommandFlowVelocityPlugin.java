package dev.ua.theroer.commandflow.velocity;

import java.nio.file.Path;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import dev.ua.theroer.commandflow.common.ProxyForwarder;
import dev.ua.theroer.commandflow.common.condition.CompareOperatorAdapter;
import dev.ua.theroer.commandflow.common.config.CommandFlowConfig;
import dev.ua.theroer.commandflow.common.remote.MessagingRemoteDispatcher;
import dev.ua.theroer.commandflow.common.remote.RemoteDispatcher;
import dev.ua.theroer.magicutils.bootstrap.MagicRuntime;
import dev.ua.theroer.magicutils.bootstrap.VelocityBootstrap;
import dev.ua.theroer.magicutils.commands.CommandRegistry;
import dev.ua.theroer.magicutils.config.ConfigManager;
import dev.ua.theroer.magicutils.messaging.MessagingService;
import org.slf4j.Logger;

/**
 * CommandFlow proxy plugin for Velocity. Forwards alias invocations to the
 * player's backend server over the MagicUtils message bus; the backend runs the
 * alias's actions and conditions.
 *
 * <p>The Velocity descriptor is the hand-authored {@code velocity-plugin.json}
 * (its {@code version} filled by {@code processResources}), matching how the
 * MagicUtils velocity-bundle wires itself.</p>
 */
public final class CommandFlowVelocityPlugin {
    private static final String PLUGIN_NAME = "CommandFlow";

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private MagicRuntime runtime;

    /**
     * Velocity injects the proxy, plugin logger and data directory.
     *
     * @param proxy the Velocity proxy server
     * @param logger the plugin's SLF4J logger
     * @param dataDirectory the plugin data directory
     */
    @Inject
    public CommandFlowVelocityPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    /**
     * Boots the runtime, wires messaging, and registers forwarding aliases.
     *
     * @param event the proxy initialize event
     */
    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        VelocityBootstrap.RuntimeResult bootstrap = VelocityBootstrap
                .forPlugin(proxy, this, PLUGIN_NAME, dataDirectory)
                .slf4j(logger)
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
            VelocityProxyAliasRegistrar registrar =
                    new VelocityProxyAliasRegistrar(commandRegistry, forwarder);
            registrar.applyAliases(config.getAliases());
        }

        logger.info("CommandFlow Velocity proxy module loaded.");
    }

    /**
     * Tears down the runtime on proxy shutdown.
     *
     * @param event the proxy shutdown event
     */
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (runtime != null) {
            runtime.close();
        }
    }
}
