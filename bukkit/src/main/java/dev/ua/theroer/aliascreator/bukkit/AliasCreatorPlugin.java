package dev.ua.theroer.aliascreator.bukkit;

import dev.ua.theroer.aliascreator.common.AliasController;
import dev.ua.theroer.aliascreator.common.AliasService;
import dev.ua.theroer.aliascreator.common.commands.AliasCommand;
import dev.ua.theroer.aliascreator.common.config.AliasCreatorConfig;
import dev.ua.theroer.magicutils.Logger;
import dev.ua.theroer.magicutils.commands.CommandRegistry;
import dev.ua.theroer.magicutils.commands.HelpCommandSupport;
import dev.ua.theroer.magicutils.config.ConfigManager;
import dev.ua.theroer.magicutils.platform.bukkit.BukkitPlatformProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class AliasCreatorPlugin extends JavaPlugin {
    private final AliasService aliasService = new AliasService();
    private ConfigManager configManager;
    private AliasCreatorConfig config;
    private AliasBukkitRegistrar aliasRegistrar;
    private AliasController controller;
    private Logger logger;
    private CommandRegistry commandRegistry;

    @Override
    public void onEnable() {
        BukkitPlatformProvider platform = new BukkitPlatformProvider(this);
        configManager = new ConfigManager(platform);
        config = configManager.register(AliasCreatorConfig.class);
        logger = new Logger(platform, this, configManager);

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
            getLogger().info("Aliases reloaded (" + aliasService.snapshot().size() + ").");
        });

        commandRegistry = CommandRegistry.create(this, "aliascreator", logger);
        AliasCommand aliasCommand = new AliasCommand(controller,
                new BukkitTargetSuggestionProvider(() -> config.isTargetNamespaced()));
        aliasCommand.addSubCommand(HelpCommandSupport.createHelpSubCommand(logger.getCore(), commandRegistry::commandManager));
        commandRegistry.registerAllCommands(aliasCommand);

        getLogger().info("Loaded " + aliasService.snapshot().size() + " aliases.");
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
