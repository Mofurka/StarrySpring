package irden.space.proxy.plugin.irden.d20.initiative;

import irden.space.proxy.plugin.irden.d20.initiative.model.IrdenFightSnapshot;

import java.util.Objects;

public record RunningFight(
        Thread thread,
        IrdenFightTask fightTask
) {

    public RunningFight {
        Objects.requireNonNull(thread, "thread");
        Objects.requireNonNull(fightTask, "fightTask");
    }

    public IrdenFightSnapshot snapshot() {
        return fightTask.snapshot();
    }
}