package dev.ua.theroer.aliascreator.common.config;

import dev.ua.theroer.magicutils.config.annotations.Comment;
import dev.ua.theroer.magicutils.config.annotations.ConfigFile;
import dev.ua.theroer.magicutils.config.annotations.ConfigReloadable;
import dev.ua.theroer.magicutils.config.annotations.ConfigValue;
import dev.ua.theroer.magicutils.config.annotations.DefaultValue;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigReloadable
@ConfigFile("aliascreator.{ext}")
@Comment("AliasCreator configuration")
public class AliasCreatorConfig {
    @ConfigValue("realtime")
    @DefaultValue("true")
    @Comment("Apply alias changes in real time when the config changes")
    private boolean realtime = true;

    @ConfigValue("target-namespaced")
    @DefaultValue("false")
    @Comment("Always show namespaced commands in target suggestions")
    private boolean targetNamespaced = false;

    @ConfigValue("aliases")
    @Comment("Alias groups (target command, aliases list, permission)")
    private List<AliasEntry> aliases = defaults();

    @ConfigValue("template-aliases")
    @Comment("Template aliases with arguments and placeholder replacements")
    private List<AliasTemplateEntry> templateAliases = templateDefaults();

    private static List<AliasEntry> defaults() {
        List<AliasEntry> list = new ArrayList<>();
        list.add(new AliasEntry("/spawn", List.of("spawn"), "aliascreator.alias.spawn"));
        list.add(new AliasEntry("/server hub", List.of("hub", "lobby"), "aliascreator.alias.hub"));
        return list;
    }

    private static List<AliasTemplateEntry> templateDefaults() {
        List<AliasTemplateEntry> list = new ArrayList<>();

        AliasTemplateArgument item = new AliasTemplateArgument();
        item.setName("item");
        item.setValues(List.of("minecraft:elytra"));

        AliasTemplateArgument count = new AliasTemplateArgument();
        count.setName("count");
        count.setOptional(true);
        count.setDefaultValue("1");

        AliasTemplateEntry entry = new AliasTemplateEntry();
        entry.setAlias("agive");
        entry.setTarget("minecraft:give @s minecraft:totem_of_undying[item_model=\"{item}\"] {count}");
        entry.setPermission("aliascreator.alias.agive");
        entry.setArgs(List.of(item, count));

        list.add(entry);
        return list;
    }
}
