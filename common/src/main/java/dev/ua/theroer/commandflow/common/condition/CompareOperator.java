package dev.ua.theroer.commandflow.common.condition;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

/**
 * Comparison operator for {@link ConditionType#PLACEHOLDER_COMPARE}.
 *
 * <p>Accepts both the enum name ({@code GREATER_EQUAL}) and a symbolic alias
 * ({@code >=}) in config, via {@link #from(String)}. Numeric operators compare
 * numerically when both sides parse as numbers, else lexicographically.</p>
 */
public enum CompareOperator {
    EQUALS("==", "="),
    NOT_EQUALS("!=", "<>"),
    GREATER(">"),
    GREATER_EQUAL(">="),
    LESS("<"),
    LESS_EQUAL("<="),
    CONTAINS("~");

    private final String[] symbols;

    CompareOperator(String... symbols) {
        this.symbols = symbols;
    }

    /**
     * Parses an operator from its enum name or a symbolic alias.
     *
     * @param value config value (e.g. {@code GREATER_EQUAL} or {@code >=})
     * @return the operator, or {@link #EQUALS} when blank/unknown
     */
    public static CompareOperator from(String value) {
        if (value == null || value.isBlank()) {
            return EQUALS;
        }
        String trimmed = value.trim();
        for (CompareOperator op : values()) {
            for (String symbol : op.symbols) {
                if (symbol.equals(trimmed)) {
                    return op;
                }
            }
        }
        try {
            return valueOf(trimmed.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return EQUALS;
        }
    }

    /**
     * Evaluates {@code left <op> right}.
     *
     * @param left left operand (already placeholder-resolved)
     * @param right right operand
     * @return comparison result
     */
    public boolean test(@Nullable String left, @Nullable String right) {
        String l = left != null ? left : "";
        String r = right != null ? right : "";
        if (this == CONTAINS) {
            return l.contains(r);
        }
        Double ln = parseDouble(l);
        Double rn = parseDouble(r);
        int cmp;
        if (ln != null && rn != null) {
            cmp = Double.compare(ln, rn);
        } else {
            cmp = l.compareTo(r);
        }
        return switch (this) {
            case EQUALS -> ln != null && rn != null ? cmp == 0 : l.equals(r);
            case NOT_EQUALS -> ln != null && rn != null ? cmp != 0 : !l.equals(r);
            case GREATER -> cmp > 0;
            case GREATER_EQUAL -> cmp >= 0;
            case LESS -> cmp < 0;
            case LESS_EQUAL -> cmp <= 0;
            case CONTAINS -> l.contains(r);
        };
    }

    private static @Nullable Double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
