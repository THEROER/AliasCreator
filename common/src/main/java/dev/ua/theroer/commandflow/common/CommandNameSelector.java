package dev.ua.theroer.commandflow.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CommandNameSelector {
    private CommandNameSelector() {
    }

    public static List<String> select(Collection<String> rawNames) {
        return select(rawNames, null, false);
    }

    public static List<String> select(Collection<String> rawNames, Map<String, ?> identityByName) {
        return select(rawNames, identityByName, false);
    }

    public static List<String> select(Collection<String> rawNames, Map<String, ?> identityByName,
                                      boolean forceNamespaced) {
        if (rawNames == null || rawNames.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, Object> identities = normalizeIdentities(identityByName);
        Map<String, Group> groups = new LinkedHashMap<>();
        for (String raw : rawNames) {
            String cleaned = normalize(raw);
            if (cleaned.isEmpty()) {
                continue;
            }
            Object identity = identities.get(cleaned);
            int colon = cleaned.indexOf(':');
            if (colon > 0 && colon < cleaned.length() - 1) {
                String namespace = cleaned.substring(0, colon);
                String base = cleaned.substring(colon + 1);
                Group group = groups.computeIfAbsent(base, Group::new);
                group.addNamespaced(cleaned, namespace, identity);
            } else {
                Group group = groups.computeIfAbsent(cleaned, Group::new);
                group.setHasBase(identity);
            }
        }
        List<String> result = new ArrayList<>();
        for (Group group : groups.values()) {
            result.addAll(group.resolve(forceNamespaced));
        }
        return result;
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed.trim();
    }

    private static Map<String, Object> normalizeIdentities(Map<String, ?> identityByName) {
        Map<String, Object> identities = new LinkedHashMap<>();
        if (identityByName == null || identityByName.isEmpty()) {
            return identities;
        }
        for (Map.Entry<String, ?> entry : identityByName.entrySet()) {
            String key = normalize(entry.getKey());
            if (key.isEmpty()) {
                continue;
            }
            if (!identities.containsKey(key)) {
                identities.put(key, entry.getValue());
            }
        }
        return identities;
    }

    private static final class Group {
        private final String base;
        private final Set<String> namespaced = new LinkedHashSet<>();
        private final Set<String> namespaces = new LinkedHashSet<>();
        private Object baseIdentity;
        private Object namespacedIdentity;
        private boolean hasBase;
        private boolean hasDifferentIdentity;
        private boolean hasNamespaced;

        private Group(String base) {
            this.base = base;
        }

        void setHasBase(Object identity) {
            hasBase = true;
            if (baseIdentity == null) {
                baseIdentity = identity;
            }
            if (identity != null && namespacedIdentity != null && identity != namespacedIdentity) {
                hasDifferentIdentity = true;
            }
        }

        void addNamespaced(String full, String namespace, Object identity) {
            if (full == null || full.isEmpty()) {
                return;
            }
            hasNamespaced = true;
            namespaced.add(full);
            if (namespace != null && !namespace.isEmpty()) {
                namespaces.add(namespace);
            }
            if (identity == null) {
                return;
            }
            if (baseIdentity != null) {
                if (identity != baseIdentity) {
                    hasDifferentIdentity = true;
                }
                return;
            }
            if (namespacedIdentity == null) {
                namespacedIdentity = identity;
                return;
            }
            if (identity != namespacedIdentity) {
                hasDifferentIdentity = true;
            }
        }

        List<String> resolve(boolean forceNamespaced) {
            if (forceNamespaced && hasNamespaced) {
                return new ArrayList<>(namespaced);
            }
            if (!hasBase) {
                return new ArrayList<>(namespaced);
            }
            if (hasDifferentIdentity) {
                return new ArrayList<>(namespaced);
            }
            if (hasNamespaced && namespaces.size() > 1) {
                return new ArrayList<>(namespaced);
            }
            List<String> result = new ArrayList<>();
            result.add(base);
            return result;
        }
    }
}
