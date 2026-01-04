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
public class AliasTemplateEntry {
    @ConfigValue("alias")
    @DefaultValue("agive")
    @Comment("Alias command name to register")
    private String alias = "agive";

    @ConfigValue("target")
    @DefaultValue("minecraft:give @s minecraft:totem_of_undying[item_model=\\\"{item}\\\"]")
    @Comment("Target command template with placeholders, e.g. {item}")
    private String target = "minecraft:give @s minecraft:totem_of_undying[item_model=\"{item}\"]";

    @ConfigValue("permission")
    @DefaultValue("")
    @Comment("Permission required to use the template alias (empty = no permission)")
    private String permission = "";

    @ConfigValue("args")
    @Comment("Arguments available for this template alias")
    private List<AliasTemplateArgument> args = new ArrayList<>();
}
