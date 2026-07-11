package dev.ua.theroer.commandflow.common.condition;

import dev.ua.theroer.magicutils.config.serialization.ConfigAdapters;
import dev.ua.theroer.magicutils.config.serialization.ConfigValueAdapter;

/**
 * Config adapter making {@link CompareOperator} accept both enum names
 * ({@code GREATER_EQUAL}) and symbolic aliases ({@code >=}) on read, while
 * writing the canonical enum name.
 *
 * <p>Registered with {@link ConfigAdapters} so the config layer uses it instead
 * of the default {@code EnumAdapter} for {@link CompareOperator} fields.</p>
 */
public final class CompareOperatorAdapter implements ConfigValueAdapter<CompareOperator> {
    private static boolean registered;

    private CompareOperatorAdapter() {
    }

    /**
     * Registers the adapter once. Safe to call multiple times.
     */
    public static synchronized void register() {
        if (!registered) {
            ConfigAdapters.register(CompareOperator.class, new CompareOperatorAdapter());
            registered = true;
        }
    }

    @Override
    public CompareOperator deserialize(Object value) {
        return value != null ? CompareOperator.from(String.valueOf(value)) : CompareOperator.EQUALS;
    }

    @Override
    public Object serialize(CompareOperator value) {
        return value != null ? value.name() : CompareOperator.EQUALS.name();
    }
}
