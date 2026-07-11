package dev.ua.theroer.commandflow.common.config;

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
public class AliasTemplateEntry extends CommandEntry {
    @ConfigValue("alias")
    @DefaultValue("agive")
    @Comment("Alias command name to register")
    private String alias = "agive";

    @ConfigValue("args")
    @Comment("Arguments available for this template alias")
    private List<AliasTemplateArgument> args = new ArrayList<>();

    public AliasTemplateEntry() {
        setTarget("minecraft:give @s minecraft:totem_of_undying[item_model=\"{item}\"]");
    }
}
