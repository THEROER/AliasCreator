package dev.ua.theroer.commandflow.bukkit;

import dev.ua.theroer.commandflow.common.AliasCommandLine;
import dev.ua.theroer.commandflow.common.AliasTemplateResolver;
import dev.ua.theroer.commandflow.common.config.AliasTemplateArgument;
import dev.ua.theroer.commandflow.common.config.AliasTemplateEntry;
import dev.ua.theroer.magicutils.commands.CommandArgument;
import dev.ua.theroer.magicutils.commands.CommandExecution;
import dev.ua.theroer.magicutils.commands.CommandRegistry;
import dev.ua.theroer.magicutils.commands.CommandResult;
import dev.ua.theroer.magicutils.commands.CommandSpec;
import dev.ua.theroer.magicutils.commands.MagicPermissionDefault;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class AliasTemplateCommandRegistrar {
    private final CommandRegistry commandRegistry;
    private final Set<String> registered = new HashSet<>();

    AliasTemplateCommandRegistrar(CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    void applyTemplates(List<AliasTemplateEntry> templates) {
        unregisterAll();
        if (templates == null) {
            return;
        }

        for (AliasTemplateEntry entry : templates) {
            if (entry == null) {
                continue;
            }
            String alias = AliasCommandLine.normalizeAlias(entry.getAlias());
            if (alias.isEmpty()) {
                continue;
            }
            commandRegistry.registerSpec(buildSpec(alias, entry));
            registered.add(alias);
        }
    }

    private void unregisterAll() {
        for (String name : new HashSet<>(registered)) {
            commandRegistry.unregisterCommand(name);
        }
        registered.clear();
    }

    private CommandSpec<CommandSender> buildSpec(String alias, AliasTemplateEntry entry) {
        CommandSpec.Builder<CommandSender> builder = CommandSpec.builder(alias);
        String permission = normalizePermission(entry.getPermission());
        MagicPermissionDefault defaultValue = permission.isEmpty()
                ? MagicPermissionDefault.TRUE
                : MagicPermissionDefault.OP;
        builder.permission(permission);
        builder.permissionDefault(defaultValue);

        List<CommandArgument> arguments = new ArrayList<>();
        List<AliasTemplateArgument> templateArgs = entry.getArgs();
        if (templateArgs != null) {
            for (AliasTemplateArgument arg : templateArgs) {
                if (arg == null) {
                    continue;
                }
                CommandArgument.Builder argBuilder = CommandArgument.builder(normalizeName(arg), String.class);
                if (arg.isGreedy()) {
                    argBuilder.greedy();
                }
                if (arg.getDefaultValue() != null && !arg.getDefaultValue().isBlank()) {
                    argBuilder.defaultValue(arg.getDefaultValue());
                } else if (arg.isOptional()) {
                    argBuilder.optional();
                }
                String suggestionSource = buildSuggestionSource(arg);
                if (suggestionSource != null) {
                    argBuilder.suggestions(suggestionSource);
                }
                arguments.add(argBuilder.build());
            }
        }
        builder.arguments(arguments);
        builder.execute(execution -> executeTemplate(execution, entry));
        return builder.build();
    }

    private CommandResult executeTemplate(CommandExecution<CommandSender> execution, AliasTemplateEntry entry) {
        CommandSender sender = execution.sender();
        String[] inputArgs = execution.rawArgs().toArray(new String[0]);
        AliasTemplateResolver.Result result = AliasTemplateResolver.resolve(entry, inputArgs, sender.getName());
        if (!result.isSuccess()) {
            return CommandResult.failure(result.errorMessage(), false);
        }
        return Bukkit.dispatchCommand(sender, result.commandLine())
                ? CommandResult.success()
                : CommandResult.failure(false);
    }

    private static String normalizePermission(String permission) {
        if (permission == null) {
            return "";
        }
        String trimmed = permission.trim();
        return trimmed.equals("-") ? "" : trimmed;
    }

    private static String normalizeName(AliasTemplateArgument arg) {
        String name = arg.getName();
        if (name == null || name.isBlank()) {
            return "arg";
        }
        return name.trim();
    }

    private static String buildSuggestionSource(AliasTemplateArgument arg) {
        Set<String> suggestions = new LinkedHashSet<>();
        if (arg.getValues() != null) {
            for (String value : arg.getValues()) {
                if (value != null && !value.isBlank()) {
                    suggestions.add(value);
                }
            }
        }
        if (arg.getAliases() != null) {
            for (String alias : arg.getAliases().keySet()) {
                if (alias != null && !alias.isBlank()) {
                    suggestions.add(alias);
                }
            }
        }
        if (suggestions.isEmpty()) {
            return null;
        }
        return "{" + String.join(", ", suggestions) + "}";
    }
}
