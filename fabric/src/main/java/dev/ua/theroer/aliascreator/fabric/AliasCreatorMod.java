package dev.ua.theroer.aliascreator.fabric;

import dev.ua.theroer.aliascreator.common.AliasController;
import dev.ua.theroer.aliascreator.common.AliasService;
import dev.ua.theroer.aliascreator.common.commands.AliasCommand;
import dev.ua.theroer.aliascreator.common.config.AliasCreatorConfig;
import dev.ua.theroer.magicutils.Logger;
import dev.ua.theroer.magicutils.bootstrap.FabricBootstrap;
import dev.ua.theroer.magicutils.commands.CommandRegistry;
import dev.ua.theroer.magicutils.commands.HelpCommandSupport;
import dev.ua.theroer.magicutils.config.ConfigManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.atomic.AtomicReference;

public final class AliasCreatorMod implements ModInitializer {
    private final AliasService aliasService = new AliasService();
    private final AtomicReference<MinecraftServer> serverRef = new AtomicReference<>();
    private ConfigManager configManager;
    private AliasCreatorConfig config;
    private AliasFabricRegistrar aliasRegistrar;
    private AliasTemplateCommandRegistrar templateRegistrar;
    private AliasController controller;
    private Logger magicLogger;
    private CommandRegistry commandRegistry;

    @Override
    public void onInitialize() {
        FabricBootstrap.RuntimeResult bootstrap = FabricBootstrap.forMod("aliascreator", serverRef::get)
                .permissionPrefix("aliascreator")
                .opLevel(2)
                .enableCommands()
                .enableDiagnostics()
                .buildRuntime();
        configManager = bootstrap.configManager();
        config = configManager.register(AliasCreatorConfig.class);
        magicLogger = bootstrap.logger();
        commandRegistry = bootstrap.commandRegistry();

        aliasService.replace(config.getAliases());
        aliasRegistrar = new AliasFabricRegistrar(aliasService, serverRef::get);
        aliasRegistrar.applyAliases(config.getAliases());
        controller = new AliasController(configManager, config, aliasService,
                () -> aliasRegistrar.applyAliases(config.getAliases()));
        configManager.onChange(AliasCreatorConfig.class, (updated, sections) -> {
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

        templateRegistrar = new AliasTemplateCommandRegistrar("aliascreator", "aliascreator",
                magicLogger, 2, serverRef::get);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            aliasRegistrar.registerDispatcher(dispatcher);
            aliasRegistrar.applyAliases(config.getAliases());
            templateRegistrar.registerDispatcher(dispatcher);
            templateRegistrar.applyTemplates(config.getTemplateAliases());
            AliasCommand aliasCommand = new AliasCommand(controller,
                    new FabricTargetSuggestionProvider(serverRef::get, () -> config.isTargetNamespaced()));
            aliasCommand.addSubCommand(HelpCommandSupport.createHelpSubCommand(magicLogger.getCore(),
                    commandRegistry::commandManager));
            commandRegistry.registerAllCommands(dispatcher, aliasCommand);
        });
    }
}
