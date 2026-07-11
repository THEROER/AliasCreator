package dev.ua.theroer.commandflow.bukkit;

import dev.ua.theroer.commandflow.common.AliasController;
import dev.ua.theroer.commandflow.common.AliasService;
import dev.ua.theroer.commandflow.common.commands.AliasCommand;
import dev.ua.theroer.commandflow.common.config.CommandFlowConfig;
import dev.ua.theroer.magicutils.Logger;
import dev.ua.theroer.magicutils.bootstrap.BukkitBootstrap;
import dev.ua.theroer.magicutils.commands.CommandRegistry;
import dev.ua.theroer.magicutils.commands.HelpCommandSupport;
import dev.ua.theroer.commandflow.common.CommandGate;
import dev.ua.theroer.commandflow.common.action.ActionPlatform;
import dev.ua.theroer.commandflow.common.condition.CompareOperatorAdapter;
import dev.ua.theroer.commandflow.common.cooldown.CooldownStore;
import dev.ua.theroer.commandflow.common.remote.MessagingRemoteDispatcher;
import dev.ua.theroer.commandflow.common.remote.RemoteDispatcher;
import dev.ua.theroer.commandflow.common.remote.RemoteReceiver;
import dev.ua.theroer.magicutils.config.ConfigManager;
import dev.ua.theroer.magicutils.messaging.MessagingService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Function;

public final class CommandFlowPlugin extends JavaPlugin {
    private final AliasService aliasService = new AliasService();
    private ConfigManager configManager;
    private CommandFlowConfig config;
    private AliasBukkitRegistrar aliasRegistrar;
    private AliasTemplateCommandRegistrar templateRegistrar;
    private AliasController controller;
    private Logger logger;
    private CommandRegistry commandRegistry;

    @Override
    public void onEnable() {
        BukkitBootstrap.RuntimeResult bootstrap = BukkitBootstrap.forPlugin(this)
                .permissionPrefix("commandflow")
                .enableCommands()
                .enableDiagnostics()
                .enableMessaging()
                .buildRuntime();
        configManager = bootstrap.configManager();
        // Let the CompareOperator config field accept symbolic aliases (>=, <, ~)
        // in addition to enum names, before the config is first read.
        CompareOperatorAdapter.register();
        config = configManager.register(CommandFlowConfig.class);
        logger = bootstrap.logger();
        commandRegistry = bootstrap.commandRegistry();

        ActionPlatform actionPlatform = new BukkitActionPlatform(this);
        MiniMessage miniMessage = MiniMessage.miniMessage();
        Function<String, Component> textParser = miniMessage::deserialize;
        BukkitConditionChecker conditionChecker = new BukkitConditionChecker();
        CommandGate gate = new CommandGate(new CooldownStore(), conditionChecker, textParser);

        // Cross-server: wire the message bus. As a backend, receive forwarded
        // commands and answer placeholder queries; as a requester, dispatch them.
        RemoteDispatcher remote = RemoteDispatcher.DISABLED;
        MessagingService messaging = bootstrap.runtime().findComponent(MessagingService.class).orElse(null);
        if (messaging != null) {
            BukkitRemoteExecutor remoteExecutor = new BukkitRemoteExecutor(actionPlatform);
            RemoteReceiver.install(messaging, remoteExecutor, remoteExecutor::resolvePlaceholder);
            remote = new MessagingRemoteDispatcher(messaging, messaging.bus().self().id());
        }

        aliasService.replace(config.getAliases());
        aliasRegistrar = new AliasBukkitRegistrar(this, aliasService, actionPlatform, gate, textParser, remote);
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
            getLogger().info("Aliases reloaded (" + count + ").");
        });

        templateRegistrar = new AliasTemplateCommandRegistrar(commandRegistry);
        templateRegistrar.applyTemplates(config.getTemplateAliases());
        AliasCommand aliasCommand = new AliasCommand(controller,
                new BukkitTargetSuggestionProvider(() -> config.isTargetNamespaced()),
                conditionChecker, textParser);
        aliasCommand.addSubCommand(HelpCommandSupport.createHelpSubCommand(logger.getCore(), commandRegistry::commandManager));
        commandRegistry.registerAllCommands(aliasCommand);

        int count = aliasService.snapshot().size()
                + (config.getTemplateAliases() != null ? config.getTemplateAliases().size() : 0);
        getLogger().info("Loaded " + count + " aliases.");
    }

    @Override
    public void onDisable() {
        if (aliasRegistrar != null) {
            aliasRegistrar.unregisterAll();
        }
        if (configManager != null) {
            configManager.shutdown();
        }
    }
}
