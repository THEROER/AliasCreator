package dev.ua.theroer.commandflow.common.trace;

import org.jetbrains.annotations.Nullable;

/**
 * One recorded step in a debug trace: a labelled event with an optional detail
 * and a {@link TraceStatus}.
 *
 * @param label short step name (e.g. "condition PERMISSION", "action RUN")
 * @param detail human-readable detail (resolved command, comparison, target), may be null
 * @param status outcome classification
 */
public record TraceStep(String label, @Nullable String detail, TraceStatus status) {
    /**
     * Creates a step with no detail.
     *
     * @param label step label
     * @param status outcome
     * @return trace step
     */
    public static TraceStep of(String label, TraceStatus status) {
        return new TraceStep(label, null, status);
    }

    /**
     * Creates a step with a detail.
     *
     * @param label step label
     * @param detail detail text
     * @param status outcome
     * @return trace step
     */
    public static TraceStep of(String label, String detail, TraceStatus status) {
        return new TraceStep(label, detail, status);
    }
}
