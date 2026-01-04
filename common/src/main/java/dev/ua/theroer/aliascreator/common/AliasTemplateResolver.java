package dev.ua.theroer.aliascreator.common;

import dev.ua.theroer.aliascreator.common.config.AliasTemplateArgument;
import dev.ua.theroer.aliascreator.common.config.AliasTemplateEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AliasTemplateResolver {
    private AliasTemplateResolver() {
    }

    public record Result(String commandLine, String errorMessage) {
        public static Result success(String commandLine) {
            return new Result(commandLine, null);
        }

        public static Result error(String message) {
            return new Result(null, message);
        }

        public boolean isSuccess() {
            return errorMessage == null;
        }
    }

    public static Result resolve(AliasTemplateEntry entry, String[] args, String senderName) {
        if (entry == null) {
            return Result.error("Unknown alias");
        }
        String target = entry.getTarget();
        if (target == null || target.isBlank()) {
            return Result.error("Alias target is empty");
        }

        List<AliasTemplateArgument> arguments = safeArgs(entry);
        Map<String, String> variables = new LinkedHashMap<>();
        if (senderName != null && !senderName.isBlank()) {
            variables.put("sender", senderName);
        }
        for (int i = 0; i < arguments.size(); i++) {
            AliasTemplateArgument argument = arguments.get(i);
            String name = normalizeArgumentName(argument, i);
            variables.put(name, "");
        }

        String[] rawArgs = args != null ? args : new String[0];
        int rawIndex = 0;
        for (int i = 0; i < arguments.size(); i++) {
            AliasTemplateArgument argument = arguments.get(i);
            String name = normalizeArgumentName(argument, i);

            if (argument.isGreedy()) {
                String joined = joinRemaining(rawArgs, rawIndex);
                if (joined.isBlank()) {
                    String fallback = resolveFallback(argument);
                    if (fallback == null) {
                        return Result.error("Missing argument: " + name);
                    }
                    variables.put(name, fallback);
                } else {
                    String resolved = resolveValue(argument, joined);
                    if (resolved == null) {
                        return Result.error(buildInvalidMessage(name, argument));
                    }
                    variables.put(name, resolved);
                }
                rawIndex = rawArgs.length;
                break;
            }

            String raw = rawIndex < rawArgs.length ? rawArgs[rawIndex] : null;
            if (raw == null || raw.isBlank()) {
                String fallback = resolveFallback(argument);
                if (fallback == null) {
                    return Result.error("Missing argument: " + name);
                }
                variables.put(name, fallback);
                continue;
            }

            String resolved = resolveValue(argument, raw);
            if (resolved == null) {
                return Result.error(buildInvalidMessage(name, argument));
            }
            variables.put(name, resolved);
            rawIndex++;
        }

        String replaced = applyVariables(target, variables);
        int consumed = Math.min(rawIndex, rawArgs.length);
        String[] tail = rawArgs.length > consumed
                ? slice(rawArgs, consumed, rawArgs.length)
                : new String[0];
        String commandLine = AliasCommandLine.buildCommandLine(replaced, tail);
        if (commandLine.isEmpty()) {
            return Result.error("Alias target is empty");
        }
        return Result.success(commandLine);
    }

    public static List<String> suggestions(AliasTemplateEntry entry, int argIndex, String prefix) {
        if (entry == null || argIndex < 0) {
            return List.of();
        }
        List<AliasTemplateArgument> arguments = safeArgs(entry);
        if (argIndex >= arguments.size()) {
            return List.of();
        }
        AliasTemplateArgument argument = arguments.get(argIndex);
        Set<String> values = new LinkedHashSet<>();
        if (argument.getValues() != null) {
            for (String value : argument.getValues()) {
                if (value != null && !value.isBlank()) {
                    values.add(value);
                }
            }
        }
        if (argument.getAliases() != null) {
            for (String alias : argument.getAliases().keySet()) {
                if (alias != null && !alias.isBlank()) {
                    values.add(alias);
                }
            }
        }
        if (values.isEmpty()) {
            return List.of();
        }
        String normalizedPrefix = prefix != null ? prefix.toLowerCase(Locale.ROOT) : "";
        List<String> filtered = new ArrayList<>();
        for (String value : values) {
            if (normalizedPrefix.isEmpty() || value.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix)) {
                filtered.add(value);
            }
        }
        return filtered;
    }

    private static String resolveValue(AliasTemplateArgument argument, String raw) {
        String trimmed = raw != null ? raw.trim() : "";
        if (trimmed.isEmpty()) {
            return null;
        }
        Map<String, String> aliases = argument.getAliases();
        if (aliases != null && !aliases.isEmpty()) {
            for (Map.Entry<String, String> entry : aliases.entrySet()) {
                String key = entry.getKey();
                if (key == null) {
                    continue;
                }
                if (key.equalsIgnoreCase(trimmed)) {
                    String mapped = entry.getValue();
                    if (mapped != null && !mapped.isBlank()) {
                        return mapped;
                    }
                }
            }
        }
        List<String> values = argument.getValues();
        if (values == null || values.isEmpty()) {
            return trimmed;
        }
        for (String value : values) {
            if (value != null && value.equalsIgnoreCase(trimmed)) {
                return value;
            }
        }
        return null;
    }

    private static String resolveFallback(AliasTemplateArgument argument) {
        if (argument.getDefaultValue() != null && !argument.getDefaultValue().isBlank()) {
            String resolved = resolveValue(argument, argument.getDefaultValue());
            return resolved != null ? resolved : argument.getDefaultValue();
        }
        return argument.isOptional() ? "" : null;
    }

    private static String buildInvalidMessage(String name, AliasTemplateArgument argument) {
        StringBuilder builder = new StringBuilder("Invalid value for ").append(name);
        List<String> allowed = new ArrayList<>();
        if (argument.getValues() != null) {
            for (String value : argument.getValues()) {
                if (value != null && !value.isBlank()) {
                    allowed.add(value);
                }
            }
        }
        if (argument.getAliases() != null) {
            for (String alias : argument.getAliases().keySet()) {
                if (alias != null && !alias.isBlank()) {
                    allowed.add(alias);
                }
            }
        }
        if (!allowed.isEmpty()) {
            builder.append(" (allowed: ").append(String.join(", ", allowed)).append(")");
        }
        return builder.toString();
    }

    private static String applyVariables(String target, Map<String, String> variables) {
        String result = target;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private static String normalizeArgumentName(AliasTemplateArgument argument, int index) {
        String name = argument != null ? argument.getName() : null;
        if (name == null || name.isBlank()) {
            return "arg" + index;
        }
        return name.trim();
    }

    private static List<AliasTemplateArgument> safeArgs(AliasTemplateEntry entry) {
        List<AliasTemplateArgument> args = entry.getArgs();
        return args != null ? args : List.of();
    }

    private static String joinRemaining(String[] args, int start) {
        if (args == null || start >= args.length) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }

    private static String[] slice(String[] source, int start, int end) {
        int length = Math.max(0, end - start);
        if (length == 0) {
            return new String[0];
        }
        String[] out = new String[length];
        System.arraycopy(source, start, out, 0, length);
        return out;
    }
}
