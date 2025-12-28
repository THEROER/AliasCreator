package dev.ua.theroer.aliascreator.common.config;

import dev.ua.theroer.magicutils.config.annotations.Comment;
import dev.ua.theroer.magicutils.config.annotations.ConfigSerializable;
import dev.ua.theroer.magicutils.config.annotations.ConfigValue;
import dev.ua.theroer.magicutils.config.annotations.DefaultValue;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigSerializable
public class AliasEntry {
    @ConfigValue("target")
    @DefaultValue("/spawn")
    @Comment("Target command to execute")
    private String target = "/spawn";

    @ConfigValue("aliases")
    @Comment("Alias names that trigger the target")
    private List<String> aliases = new ArrayList<>();

    @ConfigValue("permission")
    @DefaultValue("")
    @Comment("Permission required to use the alias (empty = no permission)")
    private String permission = "";

    public AliasEntry() {
    }

    public AliasEntry(String target, List<String> aliases, String permission) {
        this.target = target;
        this.aliases = aliases != null ? new ArrayList<>(aliases) : new ArrayList<>();
        this.permission = permission;
    }
}
