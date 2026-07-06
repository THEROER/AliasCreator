package dev.ua.theroer.aliascreator.bukkit;

import dev.ua.theroer.aliascreator.common.AliasController;
import dev.ua.theroer.aliascreator.common.AliasService;
import dev.ua.theroer.aliascreator.common.commands.AliasCommand;
import dev.ua.theroer.aliascreator.common.config.AliasCreatorConfig;
import dev.ua.theroer.magicutils.Logger;
import dev.ua.theroer.magicutils.bootstrap.BukkitBootstrap;
import dev.ua.theroer.magicutils.commands.CommandRegistry;
import dev.ua.theroer.magicutils.commands.HelpCommandSupport;
import dev.ua.theroer.magicutils.config.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class AliasCreatorPlugin extends JavaPlugin {
    private final AliasService aliasService = new AliasService();
    private ConfigManager configManager;
    private AliasCreatorConfig config;
    private AliasBukkitRegistrar aliasRegistrar;
    private AliasTemplateCommandRegistrar templateRegistrar;
    private AliasController controller;
    private Logger logger;
    private CommandRegistry commandRegistry;

    @Override
    public void onEnable() {
        BukkitBootstrap.RuntimeResult bootstrap = BukkitBootstrap.forPlugin(this)
                .permissionPrefix("aliascreator")
                .enableCommands()
                .enableDiagnostics()
                .buildRuntime();
        configManager = bootstrap.configManager();
        config = configManager.register(AliasCreatorConfig.class);
        logger = bootstrap.logger();
        commandRegistry = bootstrap.commandRegistry();

        aliasService.replace(config.getAliases());
        aliasRegistrar = new AliasBukkitRegistrar(this, aliasService);
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
            getLogger().info("Aliases reloaded (" + count + ").");
        });

        templateRegistrar = new AliasTemplateCommandRegistrar(commandRegistry);
        templateRegistrar.applyTemplates(config.getTemplateAliases());
        AliasCommand aliasCommand = new AliasCommand(controller,
                new BukkitTargetSuggestionProvider(() -> config.isTargetNamespaced()));
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
