package irden.space.proxy.plugin.irden.d20.initiative;

import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.api.annotations.OnStop;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageContext;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageHandler;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageService;
import irden.space.proxy.plugin.irden.d20.initiative.model.FightEntityType;
import irden.space.proxy.plugin.irden.d20.initiative.model.IrdenFightSnapshot;
import irden.space.proxy.plugin.irden.d20.initiative.model.PlayerInFight;
import irden.space.proxy.plugin.irden.persistence.model.fight.IrdenFightSnapshotRecordEntity;
import irden.space.proxy.plugin.irden.persistence.repository.fight.FightSnapshotRecordRepository;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.codec.variant.Variants;
import irden.space.proxy.protocol.util.MapVariantUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class IrdenFightHandler {

    private final Map<String, RunningFight> fights = new ConcurrentHashMap<>();

    private final PlayerManagerApi playerManagerApi;
    private final EntityMessageService entityMessageService;
    private final FightSnapshotRecordRepository  fightSnapshotRecordRepository;

    //TODO: Убрать дубликаты

    @OnLoad
    public void onLoad() {
        fightSnapshotRecordRepository.findAll().forEach(fightSnapshotRecord -> {
            var initialSnapshot = fightSnapshotRecord.getSnapshot();
            var fightName = fightSnapshotRecord.getFightName();
            IrdenFightTask fightTask = new IrdenFightTask(
                    initialSnapshot,
                    playerManagerApi,
                    entityMessageService
            );

            Thread virtualThread = Thread.ofVirtual()
                    .name("irden:fight:" + fightName)
                    .unstarted(() -> {
                        try {
                            fightTask.run();
                        } finally {
                            fights.computeIfPresent(
                                    fightName,
                                    (_, currentFight) ->
                                            currentFight.fightTask() == fightTask
                                                    ? null
                                                    : currentFight
                            );
                        }
                    });

            RunningFight newFight = new RunningFight(
                    virtualThread,
                    fightTask
            );

            RunningFight concurrentlyCreatedFight =
                    fights.putIfAbsent(fightName, newFight);

            /*
             * Кто-то создал бой одновременно с нами.
             * Возвращаемся в начало цикла и добавляем игрока туда.
             */
            if (concurrentlyCreatedFight != null) {
                return;
            }

            synchronized (newFight) {
                /*
                 * Последнего участника могли удалить между putIfAbsent
                 * и запуском потока.
                 */
                if (fights.get(fightName) != newFight) {
                    return;
                }

                try {
                    virtualThread.start();
                } catch (RuntimeException e) {
                    fights.remove(fightName, newFight);
                    throw e;
                }
            }
        });
        fightSnapshotRecordRepository.deleteAll();
    }


    @Async
    public void startFight(
            String fightName,
            Player initiator,
            int initiative,
            FightEntityType fightEntityType
    ) {
        Objects.requireNonNull(fightName, "fightName");
        Objects.requireNonNull(initiator, "initiator");
        Objects.requireNonNull(fightEntityType, "fightEntityType");

        String initiatorUuid = initiator.uuid().toString();

        PlayerInFight playerInFight = new PlayerInFight(
                initiator.nickname(),
                initiatorUuid,
                initiative,
                fightEntityType
        );

        while (true) {
            RunningFight existingFight = fights.get(fightName);

            if (existingFight != null) {
                synchronized (existingFight) {
                    /*
                     * Проверяем, что это всё ещё актуальный бой.
                     */
                    if (fights.get(fightName) != existingFight) {
                        continue;
                    }

                    log.info(
                            "Бой \"{}\" уже существует. Добавляю игрока {}, {}",
                            fightName,
                            initiator.name(),
                            fightEntityType
                    );

                    existingFight.fightTask()
                            .addPlayerToFight(playerInFight);

                    return;
                }
            }

            log.warn(
                    "Игрок {} инициировал новый бой: {}, тип сущности: {}",
                    initiator.name(),
                    fightName,
                    fightEntityType
            );

            IrdenFightSnapshot initialSnapshot =
                    IrdenFightSnapshot.start(
                            fightName,
                            initiator,
                            initiative,
                            fightEntityType
                    );

            IrdenFightTask fightTask = new IrdenFightTask(
                    initialSnapshot,
                    playerManagerApi,
                    entityMessageService
            );

            Thread virtualThread = Thread.ofVirtual()
                    .name("irden:fight:" + fightName)
                    .unstarted(() -> {
                        try {
                            fightTask.run();
                        } finally {
                            fights.computeIfPresent(
                                    fightName,
                                    (_, currentFight) ->
                                            currentFight.fightTask() == fightTask
                                                    ? null
                                                    : currentFight
                            );
                        }
                    });

            RunningFight newFight = new RunningFight(
                    virtualThread,
                    fightTask
            );

            RunningFight concurrentlyCreatedFight =
                    fights.putIfAbsent(fightName, newFight);

            /*
             * Кто-то создал бой одновременно с нами.
             * Возвращаемся в начало цикла и добавляем игрока туда.
             */
            if (concurrentlyCreatedFight != null) {
                continue;
            }

            synchronized (newFight) {
                /*
                 * Последнего участника могли удалить между putIfAbsent
                 * и запуском потока.
                 */
                if (fights.get(fightName) != newFight) {
                    return;
                }

                try {
                    virtualThread.start();
                    return;
                } catch (RuntimeException e) {
                    fights.remove(fightName, newFight);
                    throw e;
                }
            }
        }
    }

    @OnStop
    public void stopAll() {
        for (RunningFight fight : fights.values()) {
            IrdenFightSnapshot snapshot = fight.snapshot();
            fightSnapshotRecordRepository.save(new IrdenFightSnapshotRecordEntity(snapshot));
            IrdenFightHandler.this.stopFight(fight.snapshot().fightName());
        }
    }


    @Async
    public void stopFight(String fightName) {
        while (true) {
            RunningFight runningFight = requireFight(fightName);

            synchronized (runningFight) {
                if (fights.get(fightName) != runningFight) {
                    continue;
                }

                if (!fights.remove(fightName, runningFight)) {
                    continue;
                }
                runningFight.fightTask().notifyAllLeave("Бой завершён");
                runningFight.thread().interrupt();

                log.info("Бой \"{}\" остановлен", fightName);
                return;
            }
        }
    }

    public IrdenFightSnapshot addPlayerToFight(
            String fightName,
            PlayerInFight playerInFight
    ) {
        return requireFight(fightName)
                .fightTask()
                .addPlayerToFight(playerInFight);
    }

    public IrdenFightSnapshot changeInitiative(
            String fightName,
            String playerUuid,
            int newInitiative
    ) {
        return requireFight(fightName)
                .fightTask()
                .changeInitiative(playerUuid, newInitiative);
    }

    @EntityMessageHandler("irden:fight:turn:next")
    public VariantValue nextTurn(EntityMessageContext context) {
        var fightName = Variants.asString(context.arg(0));
        if (fightName.isPresent()) {
            IrdenFightSnapshot fight = getFight(fightName.get());
            var playerBySessionId = playerManagerApi.getPlayerBySessionId(context.session().sessionId());
            if (playerBySessionId.isPresent() && fight.currentPlayerUuidTurn().equals(playerBySessionId.get().uuid().toString())) {
                this.nextTurn(fightName.get());
                return Variants.of("Ход передан");
            }
        }
        return Variants.of("Не твой ход.");
    }

    @EntityMessageHandler("irden:fight:leave")
    public VariantValue leaveFight(EntityMessageContext context) {
        var fightName = Variants.asString(context.arg(0));
        if (fightName.isPresent()) {
            var playerBySessionId = playerManagerApi.getPlayerBySessionId(context.session().sessionId());
            if (playerBySessionId.isPresent()) {
                this.removePlayerFromFight(fightName.get(), playerBySessionId.get().uuid().toString());
                return Variants.of("Вы покинули бой");
            }
        }
        return Variants.of("Не удалось покинуть бой.");
    }

    @EntityMessageHandler("irden:fight:check")
    public VariantValue ensureFight(EntityMessageContext context) {
        var fightName = Variants.asString(context.arg(0));
        if (fightName.isPresent()) {
            var playerBySessionId = playerManagerApi.getPlayerBySessionId(context.session().sessionId());
            if (playerBySessionId.isPresent()) {
                boolean inFight = isInFight(fightName.get(), playerBySessionId.get().uuid().toString());
                return Variants.of(inFight);
            }
        }
        return Variants.of(false);
    }

    //    ================================ FIGHT MANAGEMENT =======================================

    @EntityMessageHandler("irden:fight:manage:get")
    public VariantValue adminGetFight(EntityMessageContext context) {
        Optional<String> fightName = Variants.asString(context.arg(0));
        if (fightName.isPresent()) {
            IrdenFightSnapshot fight = getFight(fightName.get());
            return MapVariantUtils.objectToVariant(fight);
        }
        return null;
    }

    @EntityMessageHandler("irden:fight:manage:turn:next")
    public void adminTurnNext(EntityMessageContext context) {
        Optional<String> fightName = Variants.asString(context.arg(0));
        fightName.ifPresent(this::nextTurn);
    }

    @EntityMessageHandler("irden:fight:manage:player:kick")
    public void adminKickFromFight(EntityMessageContext context) {
        var fightName = Variants.asString(context.arg(0));
        var kickUuid = Variants.asString(context.arg(1));

        if (fightName.isPresent() && kickUuid.isPresent()) {
            this.removePlayerFromFight(fightName.get(), kickUuid.get());
        }
    }

    @EntityMessageHandler("irden:fight:manage:initiative:set")
    public void adminSetInitiative(EntityMessageContext context) {
        var fightName = Variants.asString(context.arg(0));
        var kickUuid = Variants.asString(context.arg(1));
        var initiativeToChange = Variants.asInt(context.arg(2));

        if (fightName.isPresent() && kickUuid.isPresent() && initiativeToChange.isPresent()) {
            this.changeInitiative(fightName.get(), kickUuid.get(), initiativeToChange.get());
        }
    }


    @EntityMessageHandler("irden:fight:manage:finish")
    public void adminFinishFight(EntityMessageContext context) {
        var fightName = Variants.asString(context.arg(0));
        fightName.ifPresent(this::stopFight);
    }

    //    ============================ END OF FIGHT MANAGEMENT ====================================


    public IrdenFightSnapshot nextTurn(String fightName) {
        return requireFight(fightName)
                .fightTask()
                .nextTurn();
    }

    public IrdenFightSnapshot setCurrentPlayer(
            String fightName,
            String playerUuid
    ) {
        return requireFight(fightName)
                .fightTask()
                .setCurrentPlayer(playerUuid);
    }

    public IrdenFightSnapshot removePlayerFromFight(
            String fightName,
            String playerUuid
    ) {
        Objects.requireNonNull(fightName, "fightName");
        Objects.requireNonNull(playerUuid, "playerUuid");

        while (true) {
            RunningFight runningFight = requireFight(fightName);

            /*
             * Этот же монитор нужно использовать при добавлении игрока.
             * Тогда добавление и удаление последнего участника
             * не смогут выполниться одновременно.
             */
            synchronized (runningFight) {

                if (fights.get(fightName) != runningFight) {
                    continue;
                }

                IrdenFightSnapshot updatedSnapshot =
                        runningFight.fightTask()
                                .removePlayerFromFight(playerUuid);

                if (!updatedSnapshot.playersInFight().isEmpty()) {
                    return updatedSnapshot;
                }

                if (fights.remove(fightName, runningFight)) {
                    log.info(
                            "В бою \"{}\" не осталось участников. Бой завершён",
                            fightName
                    );

                    runningFight.thread().interrupt();
                }

                return updatedSnapshot;
            }
        }
    }

    public IrdenFightSnapshot getFight(String fightName) {
        return requireFight(fightName).snapshot();
    }

    public IrdenFightSnapshot findUuidFightSnapshot(String playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");

        return fights.values().stream()
                .filter(fight -> fight.snapshot().containsPlayer(playerUuid)).findFirst().map(RunningFight::snapshot).orElse(null);
    }

    public boolean isInFight(String playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");

        return fights.values().stream()
                .anyMatch(fight -> fight.snapshot().containsPlayer(playerUuid));
    }


    public boolean isInFight(String fightName, String playerUuid) {
        Objects.requireNonNull(fightName, "fightName");
        Objects.requireNonNull(playerUuid, "playerUuid");

        RunningFight runningFight = fights.get(fightName);
        return runningFight != null
                && runningFight.snapshot().containsPlayer(playerUuid);
    }

    private RunningFight requireFight(String fightName) {
        RunningFight runningFight = fights.get(fightName);

        if (runningFight == null) {
            throw new IllegalStateException(
                    "Бой \"%s\" не существует или уже окончен"
                            .formatted(fightName)
            );
        }

        return runningFight;
    }
}