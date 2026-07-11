package dev.ua.theroer.commandflow.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import dev.ua.theroer.commandflow.common.CommandNameSelector;
import dev.ua.theroer.commandflow.common.TargetSuggestionProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class FabricTargetSuggestionProvider implements TargetSuggestionProvider {
    private final Supplier<MinecraftServer> serverSupplier;
    private final BooleanSupplier alwaysNamespaced;

    public FabricTargetSuggestionProvider(Supplier<MinecraftServer> serverSupplier,
                                          BooleanSupplier alwaysNamespaced) {
        this.serverSupplier = serverSupplier;
        this.alwaysNamespaced = alwaysNamespaced != null ? alwaysNamespaced : () -> false;
    }

    @Override
    public List<String> getSuggestions() {
        MinecraftServer server = serverSupplier.get();
        if (server == null) {
            return new ArrayList<>();
        }
        CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
        if (dispatcher == null) {
            return new ArrayList<>();
        }
        Map<String, Object> identities = new LinkedHashMap<>();
        List<String> raw = collectNames(dispatcher.getRoot().getChildren(), identities);
        return CommandNameSelector.select(raw, identities, alwaysNamespaced.getAsBoolean());
    }

    private List<String> collectNames(Collection<CommandNode<CommandSourceStack>> nodes,
                                      Map<String, Object> identities) {
        List<String> names = new ArrayList<>();
        for (CommandNode<CommandSourceStack> node : nodes) {
            if (node == null) {
                continue;
            }
            String name = node.getName();
            if (name == null || name.isEmpty()) {
                continue;
            }
            names.add(name);
            CommandNode<CommandSourceStack> identity = node.getRedirect() != null ? node.getRedirect() : node;
            identities.put(name, identity);
        }
        return names;
    }
}
