package dev.ua.theroer.commandflow.common.action;

import java.util.List;
import java.util.UUID;

import dev.ua.theroer.commandflow.common.config.ActionEntry;
import dev.ua.theroer.commandflow.common.remote.PlaceholderQuery;
import dev.ua.theroer.commandflow.common.remote.RemoteCommand;
import dev.ua.theroer.commandflow.common.remote.RemoteDispatcher;
import dev.ua.theroer.commandflow.common.remote.RemoteTarget;
import net.kyori.adventure.text.Component;

/**
 * Runs a command's {@link ActionEntry} sequence step by step.
 *
 * <p>Steps run in order. Most execute synchronously; {@link ActionType#DELAY} and
 * {@link ActionType#QUERY} suspend the sequence and resume the remainder later —
 * a delay via the platform scheduler, a query when the cross-server reply arrives
 * (or on timeout). This lets a sequence interleave timed effects and cross-server
 * round-trips without blocking the server thread. A {@link ActionType#RUN} with a
 * {@code server} set is published to that backend instead of run locally.</p>
 */
public final class ActionSequenceExecutor {
    private ActionSequenceExecutor() {
    }

    /**
     * Runs an action sequence in the given context.
     *
     * @param actions ordered action steps (null/empty is a no-op)
     * @param context per-invocation context
     */
    public static void run(List<ActionEntry> actions, ActionContext context) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
        runFrom(actions, 0, context);
    }

    private static void runFrom(List<ActionEntry> actions, int index, ActionContext context) {
        for (int i = index; i < actions.size(); i++) {
            ActionEntry entry = actions.get(i);
            if (entry == null) {
                continue;
            }
            ActionType type = entry.getType() != null ? entry.getType() : ActionType.RUN;
            int resumeAt = i + 1;
            switch (type) {
                case DELAY -> {
                    long ticks = Math.max(0, entry.getTicks());
                    if (resumeAt < actions.size()) {
                        context.platform().runLater(ticks, () -> runFrom(actions, resumeAt, context));
                    }
                    return; // suspended
                }
                case QUERY -> {
                    runQuery(actions, resumeAt, entry, context);
                    return; // suspended until reply/timeout
                }
                default -> execute(type, entry, context);
            }
        }
    }

    private static void execute(ActionType type, ActionEntry entry, ActionContext context) {
        ActionPlatform platform = context.platform();
        switch (type) {
            case RUN -> runCommand(entry, context);
            case MESSAGE -> platform.message(context.sender(), context.render(entry.getText()));
            case BROADCAST -> platform.broadcast(context.render(entry.getText()));
            case SOUND -> {
                String sound = context.resolve(entry.getSound());
                if (!sound.isBlank()) {
                    platform.sound(context.sender(), sound, entry.getVolume(), entry.getPitch());
                }
            }
            case TITLE -> {
                Component title = context.render(entry.getTitle());
                Component subtitle = context.render(entry.getSubtitle());
                platform.title(context.sender(), title, subtitle,
                        entry.getFadeIn(), entry.getStay(), entry.getFadeOut());
            }
            case ACTIONBAR -> platform.actionBar(context.sender(), context.render(entry.getText()));
            case DELAY, QUERY -> {
                // handled in runFrom (suspending)
            }
        }
    }

    private static void runCommand(ActionEntry entry, ActionContext context) {
        String command = context.resolve(entry.getCommand());
        if (command.isBlank()) {
            return;
        }
        RunAs runAs = entry.getAs() != null ? entry.getAs() : RunAs.PLAYER;
        RemoteTarget routing = RemoteTarget.parse(entry.getServer());
        if (routing.isLocal()) {
            context.platform().dispatch(context.sender(), runAs, command);
            return;
        }
        UUID playerId = context.sender() != null ? context.sender().id() : null;
        String playerName = context.sender() != null ? context.sender().name() : null;
        RemoteCommand payload = new RemoteCommand(command, runAs.name(), playerId, playerName);
        context.remote().publish(routing, payload, playerId);
    }

    private static void runQuery(List<ActionEntry> actions, int resumeAt, ActionEntry entry, ActionContext context) {
        RemoteDispatcher remote = context.remote();
        RemoteTarget routing = RemoteTarget.parse(entry.getServer());
        String placeholder = entry.getPlaceholder();
        String into = entry.getInto();
        if (routing.isLocal() || placeholder == null || placeholder.isBlank() || !remote.isAvailable()) {
            // Nothing to query (or messaging off): store empty and continue.
            context.setVariable(into, "");
            runFrom(actions, resumeAt, context);
            return;
        }

        UUID playerId = context.sender() != null ? context.sender().id() : null;
        UUID correlation = UUID.randomUUID();
        PlaceholderQuery query = new PlaceholderQuery(correlation, placeholder, playerId, null);
        long timeoutTicks = Math.max(1, entry.getTimeoutTicks());

        boolean[] settled = { false };
        remote.query(routing, query, playerId, timeoutTicks * 50L,
                value -> {
                    if (settled[0]) {
                        return;
                    }
                    settled[0] = true;
                    context.setVariable(into, value);
                    runFrom(actions, resumeAt, context);
                },
                () -> onTimeout(settled, context));

        // Schedule the timeout on the platform clock: if no reply settles the
        // query first, stop the pipeline with a deny message.
        context.platform().runLater(timeoutTicks, () -> {
            if (remote.cancel(correlation)) {
                onTimeout(settled, context);
            }
        });
    }

    private static void onTimeout(boolean[] settled, ActionContext context) {
        if (settled[0]) {
            return;
        }
        settled[0] = true;
        context.platform().message(context.sender(),
                context.render("<red>Cross-server query timed out."));
    }
}
