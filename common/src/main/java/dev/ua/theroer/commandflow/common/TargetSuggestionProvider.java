package dev.ua.theroer.commandflow.common;

import java.util.Collections;
import java.util.List;

public interface TargetSuggestionProvider {
    List<String> getSuggestions();

    static TargetSuggestionProvider empty() {
        return Collections::emptyList;
    }
}
