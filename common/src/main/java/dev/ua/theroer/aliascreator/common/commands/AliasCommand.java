package dev.ua.theroer.aliascreator.common.commands;

import dev.ua.theroer.aliascreator.common.AliasController;
import dev.ua.theroer.aliascreator.common.TargetSuggestionProvider;
import dev.ua.theroer.aliascreator.common.config.AliasEntry;
import dev.ua.theroer.magicutils.annotations.CommandInfo;
import dev.ua.theroer.magicutils.annotations.DefaultValue;
import dev.ua.theroer.magicutils.annotations.Greedy;
import dev.ua.theroer.magicutils.annotations.Sender;
import dev.ua.theroer.magicutils.annotations.SubCommand;
import dev.ua.theroer.magicutils.annotations.Suggest;
import dev.ua.theroer.magicutils.commands.CommandResult;
import dev.ua.theroer.magicutils.commands.MagicCommand;
import dev.ua.theroer.magicutils.commands.MagicSender;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@CommandInfo(name = "aliascreator", aliases = { "alias", "ac" }, description = "Manage command aliases")
public final class AliasCommand extends MagicCommand {
    private final AliasController controller;
    private final TargetSuggestionProvider targetSuggestions;

    public AliasCommand(AliasController controller) {
        this(controller, TargetSuggestionProvider.empty());
    }

    public AliasCommand(AliasController controller, TargetSuggestionProvider targetSuggestions) {
        this.controller = controller;
        this.targetSuggestions = targetSuggestions != null ? targetSuggestions : TargetSuggestionProvider.empty();
    }

    public CommandResult execute(@Sender MagicSender sender) {
        return CommandResult.success("Use /aliascreator list or /aliascreator set");
    }

    @SubCommand(name = "list", description = "List configured aliases")
    public CommandResult list(@Sender MagicSender sender) {
        List<AliasEntry> entries = controller.entries();
        if (entries.isEmpty()) {
            sender.audience().send(Component.text("No aliases configured."));
            return CommandResult.success();
        }
        sender.audience().send(Component.text("Aliases (" + entries.size() + "):"));
        for (AliasEntry entry : entries) {
            String target = entry.getTarget();
            String permission = entry.getPermission();
            String aliasList = entry.getAliases() != null ? String.join(", ", entry.getAliases()) : "";
            String line = "- " + aliasList + " -> " + target;
            if (permission != null && !permission.isBlank()) {
                line += " (perm: " + permission + ")";
            }
            sender.audience().send(Component.text(line));
        }
        return CommandResult.success();
    }

    @SubCommand(name = "set", description = "Create or update an alias", aliases = "create")
    public CommandResult set(@Sender MagicSender sender,
            @Suggest("getAliasSuggestions") String alias,
            @Suggest("getTargetSuggestions") @Greedy String target) {
        if (!controller.setAlias(alias, target)) {
            return CommandResult.failure("Invalid alias or target", false);
        }
        return CommandResult.success("Alias updated", false);
    }

    @SubCommand(name = "perm", description = "Set permission for an alias")
    public CommandResult permission(@Sender MagicSender sender,
            @Suggest("getAliasSuggestions") String alias,
            @DefaultValue("-") String permission) {
        if (!controller.setPermission(alias, permission)) {
            return CommandResult.failure("Alias not found", false);
        }
        return CommandResult.success("Permission updated", false);
    }

    @SubCommand(name = "remove", aliases = { "delete" }, description = "Remove an alias")
    public CommandResult remove(@Sender MagicSender sender,
            @Suggest("getAliasSuggestions") String alias) {
        if (!controller.removeAlias(alias)) {
            return CommandResult.failure("Alias not found", false);
        }
        return CommandResult.success("Alias removed", false);
    }

    @SubCommand(name = "reload", description = "Reload alias configuration")
    public CommandResult reload(@Sender MagicSender sender) {
        controller.reload();
        return CommandResult.success("Alias config reloaded", false);
    }

    public List<String> getAliasSuggestions() {
        List<AliasEntry> entries = controller.entries();
        if (entries.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> aliases = new LinkedHashSet<>();
        for (AliasEntry entry : entries) {
            List<String> entryAliases = entry.getAliases();
            if (entryAliases == null) {
                continue;
            }
            for (String alias : entryAliases) {
                if (alias == null) {
                    continue;
                }
                String trimmed = alias.trim();
                if (!trimmed.isEmpty()) {
                    aliases.add(trimmed);
                }
            }
        }
        return new ArrayList<>(aliases);
    }

    public List<String> getTargetSuggestions() {
        List<String> suggestions = targetSuggestions.getSuggestions();
        if (suggestions == null || suggestions.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String suggestion : suggestions) {
            if (suggestion == null) {
                continue;
            }
            String trimmed = suggestion.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            unique.add(trimmed);
        }
        return new ArrayList<>(unique);
    }
}
