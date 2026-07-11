package dev.ua.theroer.commandflow.fabric;

import dev.ua.theroer.commandflow.common.AliasController;
import dev.ua.theroer.commandflow.common.AliasService;
import dev.ua.theroer.commandflow.common.commands.AliasCommand;
import dev.ua.theroer.commandflow.common.config.CommandFlowConfig;
import dev.ua.theroer.magicutils.Logger;
import dev.ua.theroer.magicutils.bootstrap.FabricBootstrap;
import dev.ua.theroer.magicutils.commands.CommandRegistry;
import dev.ua.theroer.magicutils.commands.HelpCommandSupport;
import dev.ua.theroer.commandflow.common.CommandGate;
import dev.ua.theroer.commandflow.common.action.ActionPlatform;
import dev.ua.theroer.commandflow.common.condition.CompareOperatorAdapter;
import dev.ua.theroer.commandflow.common.cooldown.CooldownStore;
import dev.ua.theroer.commandflow.common.remote.MessagingRemoteDispatcher;
import dev.ua.theroer.commandflow.common.remote.RemoteDispatcher;
import dev.ua.theroer.commandflow.common.remote.RemoteReceiver;
import dev.ua.theroer.magicutils.messaging.MessagingService;
import dev.ua.theroer.magicutils.config.ConfigManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public final class CommandFlowMod implements ModInitializer {
    private final AliasService aliasService = new AliasService();
    private final AtomicReference<MinecraftServer> serverRef = new AtomicReference<>();
    private ConfigManager configManager;
    private CommandFlowConfig config;
    private AliasFabricRegistrar aliasRegistrar;
    private AliasTemplateCommandRegistrar templateRegistrar;
    private AliasController controller;
    private Logger magicLogger;
    private CommandRegistry commandRegistry;

    @Override
    public void onInitialize() {
        FabricBootstrap.RuntimeResult bootstrap = FabricBootstrap.forMod("commandflow", serverRef::get)
                .permissionPrefix("commandflow")
                .opLevel(2)
                .enableCommands()
                .enableDiagnostics()
                .enableMessaging()
                .buildRuntime();
        configManager = bootstrap.configManager();
        // Let the CompareOperator config field accept symbolic aliases (>=, <, ~)
        // in addition to enum names, before the config is first read.
        CompareOperatorAdapter.register();
        config = configManager.register(CommandFlowConfig.class);
        magicLogger = bootstrap.logger();
        commandRegistry = bootstrap.commandRegistry();

        ActionPlatform actionPlatform = new FabricActionPlatform(serverRef::get);
        MiniMessage miniMessage = MiniMessage.miniMessage();
        Function<String, Component> textParser = miniMessage::deserialize;
        FabricConditionChecker conditionChecker = new FabricConditionChecker();
        CommandGate gate = new CommandGate(new CooldownStore(), conditionChecker, textParser);

        // Cross-server: wire the message bus (Fabric needs Redis for real delivery;
        // without it the bus is loopback-only and cross-server actions no-op).
        RemoteDispatcher remote = RemoteDispatcher.DISABLED;
        MessagingService messaging = bootstrap.runtime().findComponent(MessagingService.class).orElse(null);
        if (messaging != null) {
            FabricRemoteExecutor remoteExecutor = new FabricRemoteExecutor(serverRef::get, actionPlatform);
            RemoteReceiver.install(messaging, remoteExecutor, remoteExecutor::resolvePlaceholder);
            remote = new MessagingRemoteDispatcher(messaging, messaging.bus().self().id());
        }

        aliasService.replace(config.getAliases());
        aliasRegistrar = new AliasFabricRegistrar(aliasService, serverRef::get, actionPlatform, gate, textParser, remote);
        aliasRegistrar.applyAliases(config.getAliases());
        controller = new AliasController(configManager, config, aliasService,
                () -> aliasRegistrar.applyAliases(config.getAliases()));
        configManager.onChange(CommandFlowConfig.class, (updated, sections) -> {
            if (!updated.isRealtime()) {
                return;
            }
            aliasService.replace(updated.getAliases());
            aliasRegistrar.applyAliases(updated.getAliases());
            if (templateRegistrar != null) {
                templateRegistrar.applyTemplates(updated.getTemplateAliases());
            }
            int count = aliasService.snapshot().size()
                    + (updated.getTemplateAliases() != null ? updated.getTemplateAliases().size() : 0);
            magicLogger.info("Aliases reloaded ({}).", count);
        });

        ServerLifecycleEvents.SERVER_STARTING.register(serverRef::set);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            serverRef.set(null);
            if (configManager != null) {
                configManager.shutdown();
            }
        });

        templateRegistrar = new AliasTemplateCommandRegistrar("commandflow", "commandflow",
                magicLogger, 2, serverRef::get);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            aliasRegistrar.registerDispatcher(dispatcher);
            aliasRegistrar.applyAliases(config.getAliases());
            templateRegistrar.registerDispatcher(dispatcher);
            templateRegistrar.applyTemplates(config.getTemplateAliases());
            AliasCommand aliasCommand = new AliasCommand(controller,
                    new FabricTargetSuggestionProvider(serverRef::get, () -> config.isTargetNamespaced()),
                    conditionChecker, textParser);
            aliasCommand.addSubCommand(HelpCommandSupport.createHelpSubCommand(magicLogger.getCore(),
                    commandRegistry::commandManager));
            commandRegistry.registerAllCommands(dispatcher, aliasCommand);
        });
    }
}
