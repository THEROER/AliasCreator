package dev.ua.theroer.commandflow.common.config;

import dev.ua.theroer.magicutils.config.annotations.Comment;
import dev.ua.theroer.magicutils.config.annotations.ConfigSerializable;
import dev.ua.theroer.magicutils.config.annotations.ConfigValue;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigSerializable
public class AliasEntry extends CommandEntry {
    @ConfigValue("aliases")
    @Comment("Alias names that trigger the target")
    private List<String> aliases = new ArrayList<>();

    public AliasEntry() {
        setTarget("/spawn");
    }

    public AliasEntry(String target, List<String> aliases, String permission) {
        setTarget(target);
        this.aliases = aliases != null ? new ArrayList<>(aliases) : new ArrayList<>();
        setPermission(permission);
    }
}
