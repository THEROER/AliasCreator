package dev.ua.theroer.commandflow.common.config;

import dev.ua.theroer.magicutils.config.annotations.Comment;
import dev.ua.theroer.magicutils.config.annotations.ConfigSerializable;
import dev.ua.theroer.magicutils.config.annotations.ConfigValue;
import dev.ua.theroer.magicutils.config.annotations.DefaultValue;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ConfigSerializable
public class AliasTemplateArgument {
    @ConfigValue("name")
    @DefaultValue("item")
    @Comment("Argument name used in template placeholders, e.g. {item}")
    private String name = "item";

    @ConfigValue("values")
    @Comment("Allowed values (empty = allow any)")
    private List<String> values = new ArrayList<>();

    @ConfigValue("aliases")
    @Comment("Alias mapping for values (alias -> actual value)")
    private Map<String, String> aliases = new LinkedHashMap<>();

    @ConfigValue("optional")
    @DefaultValue("false")
    @Comment("Whether the argument is optional (best used at the end)")
    private boolean optional = false;

    @ConfigValue("default")
    @Comment("Default value used when the argument is missing")
    private String defaultValue;

    @ConfigValue("greedy")
    @DefaultValue("false")
    @Comment("Consume remaining input as a single argument")
    private boolean greedy = false;
}
