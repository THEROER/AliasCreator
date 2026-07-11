package dev.ua.theroer.commandflow.common.trace;

/**
 * Outcome classification of a single {@link TraceStep} in a debug trace, used to
 * colour the rendered output.
 */
public enum TraceStatus {
    /** The step passed / would execute. */
    OK,
    /** The step was skipped (e.g. a condition not applicable, blank action). */
    SKIP,
    /** The step failed / would block execution (condition denied, timeout). */
    FAIL,
    /** Informational note (placeholder resolution, routing target). */
    INFO
}
