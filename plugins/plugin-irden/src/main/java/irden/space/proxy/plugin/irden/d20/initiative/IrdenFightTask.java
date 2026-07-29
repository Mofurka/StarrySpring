package irden.space.proxy.plugin.irden.d20.initiative;

import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageFailedException;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageService;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageSessionClosedException;
import irden.space.proxy.plugin.irden.d20.initiative.model.IrdenFightSnapshot;
import irden.space.proxy.plugin.irden.d20.initiative.model.PlayerInFight;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.codec.variant.Variants;
import irden.space.proxy.protocol.util.MapVariantUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public final class IrdenFightTask implements Runnable {

    private static final Duration PAUSE = Duration.ofSeconds(3);

    private final AtomicReference<IrdenFightSnapshot> snapshotReference;
    private final PlayerManagerApi playerManagerApi;
    private final EntityMessageService entityMessageService;

    public IrdenFightTask(
            IrdenFightSnapshot initialSnapshot,
            PlayerManagerApi playerManagerApi,
            EntityMessageService entityMessageService
    ) {
        this.snapshotReference = new AtomicReference<>(
                Objects.requireNonNull(initialSnapshot, "initialSnapshot")
        );
        this.playerManagerApi = Objects.requireNonNull(
                playerManagerApi,
                "playerManagerApi"
        );
        this.entityMessageService = Objects.requireNonNull(
                entityMessageService,
                "entityMessageService"
        );
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;

        while ((current instanceof CompletionException
                || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }

        return current;
    }

    @Override
    public void run() {
        log.info(
                "Запущен поток боя: {}, virtual={}",
                snapshot().fightName(),
                Thread.currentThread().isVirtual()
        );

        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    publishSnapshot(snapshot());
                } catch (RuntimeException e) {
                    log.error(
                            "Ошибка при обработке боя {}",
                            snapshot().fightName(),
                            e
                    );
                }

                Thread.sleep(PAUSE);
            }
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        } finally {
            log.info("Поток боя {} завершён", snapshot().fightName());
        }
    }

    public IrdenFightSnapshot snapshot() {
        return snapshotReference.get();
    }

    public IrdenFightSnapshot addPlayerToFight(PlayerInFight playerInFight) {
        IrdenFightSnapshot updatedSnapshot = snapshotReference.updateAndGet(
                current -> current.addPlayer(playerInFight)
        );

        publishSnapshot(updatedSnapshot);
        return updatedSnapshot;
    }

    public IrdenFightSnapshot removePlayerFromFight(String playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");

        while (true) {
            IrdenFightSnapshot previousSnapshot =
                    snapshotReference.get();

            IrdenFightSnapshot updatedSnapshot =
                    previousSnapshot.removePlayer(playerUuid);

            /*
             * Участника в бою не было.
             */
            if (updatedSnapshot == previousSnapshot) {
                return previousSnapshot;
            }

            if (!snapshotReference.compareAndSet(
                    previousSnapshot,
                    updatedSnapshot
            )) {
                /*
                 * Кто-то параллельно изменил бой.
                 * Читаем свежий и повторяем операцию.
                 */
                continue;
            }

            publishSnapshot(updatedSnapshot);
            notifyCurrentPlayerLeave(playerUuid, "Вы были исключены из боя");

            boolean currentPlayerChanged = !Objects.equals(
                    previousSnapshot.currentPlayerUuidTurn(),
                    updatedSnapshot.currentPlayerUuidTurn()
            );

            if (currentPlayerChanged) {
                notifyCurrentPlayerTurn(updatedSnapshot);
            }

            return updatedSnapshot;
        }
    }

    public void notifyAllLeave(String message) {
        this.snapshotReference.get().queue().forEach(s ->
                this.notifyCurrentPlayerLeave(s, message)
        );
    }

    private void notifyCurrentPlayerLeave(String uuid, String message) {
        playerManagerApi
                .findPlayerByUuid(uuid, true)
                .ifPresent(player -> entityMessageService.sendToEntity(
                        player.sessionContext(),
                        player.entityId(),
                        "irden:fight:leave",
                        Variants.of(message != null ? message : "Вы были исключены из боя")));
    }

    private void notifyCurrentPlayerTurn(
            IrdenFightSnapshot snapshot
    ) {
        String currentPlayerUuid =
                snapshot.currentPlayerUuidTurn();

        if (currentPlayerUuid == null) {
            return;
        }

        playerManagerApi
                .findPlayerByUuid(currentPlayerUuid, true)
                .ifPresent(player -> {
                    var sessionContext = player.sessionContext();

                    if (sessionContext == null) {
                        log.debug(
                                "Не удалось уведомить игрока {} о ходе: " +
                                        "sessionContext отсутствует",
                                player.uuid()
                        );
                        return;
                    }

                    entityMessageService.sendToEntity(
                            sessionContext,
                            player.entityId(),
                            "irden:fight:turn:your",
                            Variants.of(snapshot.fightName())
                    ).exceptionally(failure -> {
                        Throwable cause = unwrap(failure);

                        log.warn(
                                "Не удалось уведомить игрока {} о его ходе: {}",
                                player.uuid(),
                                cause.getMessage(),
                                cause
                        );

                        return null;
                    });
                });
    }

    public IrdenFightSnapshot changeInitiative(
            String playerUuid,
            int newInitiative
    ) {
        IrdenFightSnapshot updatedSnapshot = snapshotReference.updateAndGet(
                current -> current.changeInitiative(playerUuid, newInitiative)
        );

        publishSnapshot(updatedSnapshot);
        return updatedSnapshot;
    }

    public IrdenFightSnapshot nextTurn() {
        IrdenFightSnapshot updatedSnapshot = snapshotReference.updateAndGet(
                IrdenFightSnapshot::nextTurn
        );

        publishSnapshot(updatedSnapshot);
        notifyCurrentPlayerTurn(updatedSnapshot);
        return updatedSnapshot;
    }

    public IrdenFightSnapshot setCurrentPlayer(String playerUuid) {
        IrdenFightSnapshot updatedSnapshot = snapshotReference.updateAndGet(
                current -> current.withCurrentPlayer(playerUuid)
        );

        publishSnapshot(updatedSnapshot);
        return updatedSnapshot;
    }

    private synchronized void publishSnapshot(IrdenFightSnapshot snapshot) {
        VariantValue variantValue = MapVariantUtils.objectToVariant(snapshot);

        for (PlayerInFight playerInFight : snapshot.playersInFight().values()) {
            playerManagerApi.findPlayerByUuid(playerInFight.uuid(), true)
                    .ifPresent(player -> {
                        var sessionContext = player.sessionContext();
                        var entityId = player.entityId();

                        if (sessionContext == null) {
                            log.debug(
                                    "Пропуск отправки обновления боя игроку {}: sessionContext отсутствует",
                                    player.uuid()
                            );
                            return;
                        }

                        entityMessageService.sendToEntity(
                                sessionContext,
                                entityId,
                                "irden:fight:update",
                                variantValue
                        ).exceptionally(failure -> {
                            Throwable cause = unwrap(failure);

                            if (cause instanceof EntityMessageSessionClosedException) {
                                log.warn(
                                        "Сессия игрока {} закрыта: {}",
                                        player.uuid(),
                                        cause.getMessage()
                                );
                                return null;
                            }

                            if (cause instanceof EntityMessageFailedException) {
                                log.debug(
                                        "Игрок {} ещё не подключён к бою, отправляю irden:fight:join",
                                        player.uuid()
                                );

                                entityMessageService.sendToEntity(
                                        sessionContext,
                                        entityId,
                                        "irden:fight:join",
                                        variantValue
                                ).exceptionally(joinFailure -> {
                                    Throwable joinCause = unwrap(joinFailure);
                                    log.warn(
                                            "Не удалось отправить вступление в бой игроку {}: {}",
                                            player.uuid(),
                                            joinCause.getMessage(),
                                            joinCause
                                    );
                                    return null;
                                });
                                return null;
                            }

                            log.error(
                                    "Не удалось отправить обновление боя игроку {}: {}",
                                    player.uuid(),
                                    cause.getMessage(),
                                    cause
                            );
                            return null;
                        });
                    });
        }
    }
}