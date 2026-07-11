package dev.ua.theroer.commandflow.common.trace;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects ordered {@link TraceStep}s during a dry-run of a command and renders
 * them to coloured MiniMessage lines.
 *
 * <p>The dry-run tracing platform/checker/dispatcher record what they would do
 * here instead of doing it, so a {@code /commandflow debug} shows the full
 * decision path — conditions, placeholder substitutions, routing, and actions —
 * without any side effects.</p>
 */
public final class TraceRecorder {
    private final List<TraceStep> steps = new ArrayList<>();

    /**
     * Records a step.
     *
     * @param step step to add
     */
    public void record(TraceStep step) {
        if (step != null) {
            steps.add(step);
        }
    }

    /**
     * Records a step from parts.
     *
     * @param label step label
     * @param detail detail (may be null)
     * @param status outcome
     */
    public void record(String label, String detail, TraceStatus status) {
        steps.add(TraceStep.of(label, detail, status));
    }

    /**
     * @return immutable snapshot of recorded steps
     */
    public List<TraceStep> steps() {
        return List.copyOf(steps);
    }

    /**
     * Renders the trace as MiniMessage lines (one per step, plus a header),
     * ready for the platform to parse and send.
     *
     * @param alias alias being traced
     * @return MiniMessage-formatted lines
     */
    public List<String> render(String alias) {
        List<String> lines = new ArrayList<>();
        lines.add("<gray>--- <white>debug</white> <yellow>" + escape(alias) + "</yellow> <gray>(dry-run) ---");
        if (steps.isEmpty()) {
            lines.add("<gray>(no steps)");
            return lines;
        }
        for (TraceStep step : steps) {
            lines.add(renderStep(step));
        }
        return lines;
    }

    private static String renderStep(TraceStep step) {
        String icon = switch (step.status()) {
            case OK -> "<green>✔</green>";
            case SKIP -> "<gray>○</gray>";
            case FAIL -> "<red>✘</red>";
            case INFO -> "<aqua>ℹ</aqua>";
        };
        StringBuilder line = new StringBuilder();
        line.append(icon).append(" <white>").append(escape(step.label())).append("</white>");
        if (step.detail() != null && !step.detail().isBlank()) {
            line.append(" <gray>").append(escape(step.detail()));
        }
        return line.toString();
    }

    // Neutralise stray MiniMessage tags in user-supplied text so a value like
    // "<red>" in a command shows literally rather than colouring the trace.
    private static String escape(String text) {
        return text == null ? "" : text.replace("<", "\\<");
    }
}
