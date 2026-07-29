package irden.space.proxy.plugin.irden.d20.initiative.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import irden.space.proxy.plugin.player_manager.model.Player;

import java.util.*;

public record IrdenFightSnapshot(
        String fightName,
        String authorUuid,
        Map<String, PlayerInFight> playersInFight,
        List<String> queue,
        String currentPlayerUuidTurn,
        int turn,
        UUID snapshotVersion // для поддержания идемпотетности на клиенте, дабы меньше было расчётов
) {

    private static final int FORMATION_TURN = 1;

    public IrdenFightSnapshot {
        Objects.requireNonNull(fightName, "fightName");
        Objects.requireNonNull(authorUuid, "authorUuid");
        Objects.requireNonNull(playersInFight, "playersInFight");
        Objects.requireNonNull(queue, "queue");

        if (turn < FORMATION_TURN) {
            throw new IllegalArgumentException(
                    "Номер хода не может быть меньше " + FORMATION_TURN
            );
        }

        playersInFight = Map.copyOf(playersInFight);
        queue = List.copyOf(queue);

        if (new HashSet<>(queue).size() != queue.size()) {
            throw new IllegalArgumentException(
                    "Очередь содержит повторяющиеся UUID"
            );
        }

        if (!playersInFight.keySet().containsAll(queue)) {
            throw new IllegalArgumentException(
                    "Очередь содержит UUID отсутствующего участника"
            );
        }

        if (currentPlayerUuidTurn != null
                && !playersInFight.containsKey(currentPlayerUuidTurn)) {
            throw new IllegalArgumentException(
                    "Текущий участник отсутствует в бою: "
                            + currentPlayerUuidTurn
            );
        }

        if (!playersInFight.isEmpty() && currentPlayerUuidTurn == null) {
            throw new IllegalArgumentException(
                    "При наличии участников должен быть назначен текущий ход"
            );
        }
    }

    public static IrdenFightSnapshot start(
            String fightName,
            Player initiator,
            int initiative,
            FightEntityType fightEntityType
    ) {
        Objects.requireNonNull(initiator, "initiator");
        Objects.requireNonNull(fightEntityType, "fightEntityType");

        String initiatorUuid = initiator.uuid().toString();

        PlayerInFight initiatorInFight = new PlayerInFight(
                initiator.nickname(),
                initiatorUuid,
                initiative,
                fightEntityType
        );

        return new IrdenFightSnapshot(
                fightName,
                initiatorUuid,
                Map.of(
                        initiatorUuid,
                        initiatorInFight
                ),
                List.of(initiatorUuid),
                initiatorUuid,
                FORMATION_TURN,
                UUID.randomUUID()
        );
    }

    private static List<String> buildQueue(
            Map<String, PlayerInFight> players
    ) {
        return players.values().stream()
                .sorted(
                        Comparator
                                .comparingInt(PlayerInFight::initiative)
                                .reversed()
                                .thenComparing(PlayerInFight::uuid)
                )
                .map(PlayerInFight::uuid)
                .toList();
    }

    private static String firstPlayerUuid(List<String> queue) {
        return queue.isEmpty()
                ? null
                : queue.getFirst();
    }

    public IrdenFightSnapshot addPlayer(PlayerInFight player) {
        Objects.requireNonNull(player, "player");


        if (playersInFight.containsKey(player.uuid())) {
            return this;
        }

        Map<String, PlayerInFight> updatedPlayers =
                new HashMap<>(playersInFight);

        updatedPlayers.put(player.uuid(), player);

        List<String> updatedQueue = buildQueue(updatedPlayers);

        /*
         * Пока идёт первый ход, лидером становится участник
         * с максимальной инициативой.
         *
         * После первого хода новый игрок не перехватывает текущий ход.
         */
        String updatedCurrentPlayer = isFormationTurn()
                ? firstPlayerUuid(updatedQueue)
                : currentPlayerUuidTurn;

        return copyWith(
                updatedPlayers,
                updatedQueue,
                updatedCurrentPlayer,
                turn
        );
    }

    public IrdenFightSnapshot removePlayer(String playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");

        if (!playersInFight.containsKey(playerUuid)) {
            return this;
        }

        boolean currentPlayerRemoved =
                Objects.equals(currentPlayerUuidTurn, playerUuid);

        /*
         * Запоминаем позицию до удаления.
         *
         * После удаления следующий участник сдвинется
         * на этот же индекс.
         */
        int removedPlayerIndex = queue.indexOf(playerUuid);

        Map<String, PlayerInFight> updatedPlayers =
                new HashMap<>(playersInFight);

        updatedPlayers.remove(playerUuid);

        List<String> updatedQueue = buildQueue(updatedPlayers);

        /*
         * В бою больше никого нет.
         */
        if (updatedQueue.isEmpty()) {
            return copyWith(
                    updatedPlayers,
                    updatedQueue,
                    null,
                    turn
            );
        }

        String updatedCurrentPlayer;
        int updatedTurn = turn;

        if (isFormationTurn()) {
            /*
             * На первом ходу бой ещё формируется.
             * Текущим становится лидер по инициативе.
             */
            updatedCurrentPlayer = firstPlayerUuid(updatedQueue);
        } else if (!currentPlayerRemoved) {
            /*
             * Удалили не текущего участника -
             * текущий ход не меняется.
             */
            updatedCurrentPlayer = currentPlayerUuidTurn;
        } else {
            boolean roundCompleted = removedPlayerIndex == queue.size() - 1;

            int nextPlayerIndex = removedPlayerIndex < 0
                    ? 0
                    : removedPlayerIndex % updatedQueue.size();

            updatedCurrentPlayer = updatedQueue.get(nextPlayerIndex);

            if (roundCompleted) {
                updatedTurn++;
            }
        }

        return copyWith(
                updatedPlayers,
                updatedQueue,
                updatedCurrentPlayer,
                updatedTurn
        );
    }

    public IrdenFightSnapshot changeInitiative(
            String playerUuid,
            int newInitiative
    ) {
        Objects.requireNonNull(playerUuid, "playerUuid");

        PlayerInFight player = playersInFight.get(playerUuid);

        if (player == null) {
            throw new IllegalArgumentException(
                    "Участник не найден в бою: " + playerUuid
            );
        }

        if (player.initiative() == newInitiative) {
            return this;
        }

        Map<String, PlayerInFight> updatedPlayers =
                new HashMap<>(playersInFight);

        updatedPlayers.put(
                playerUuid,
                new PlayerInFight(
                        player.name(),
                        player.uuid(),
                        newInitiative,
                        player.entityType()
                )
        );

        List<String> updatedQueue = buildQueue(updatedPlayers);

        /*
         * На первом ходу изменение инициативы может изменить
         * того, чей ход сейчас.
         *
         * Начиная со второго хода текущий участник сохраняется.
         */
        String updatedCurrentPlayer = isFormationTurn()
                ? firstPlayerUuid(updatedQueue)
                : currentPlayerUuidTurn;

        return copyWith(
                updatedPlayers,
                updatedQueue,
                updatedCurrentPlayer,
                turn
        );
    }

    public IrdenFightSnapshot nextTurn() {
        if (queue.isEmpty()) {
            return this;
        }

        int currentIndex = queue.indexOf(currentPlayerUuidTurn);

        /*
         * Такая ситуация в валидном snapshot возникнуть не должна,
         * но безопасно начинаем с первого элемента.
         */
        int nextIndex = currentIndex < 0
                ? 0
                : (currentIndex + 1) % queue.size();

        boolean roundCompleted = nextIndex == 0;
        int updatedTurn = roundCompleted ? turn + 1 : turn;

        return copyWith(
                playersInFight,
                queue,
                queue.get(nextIndex),
                updatedTurn
        );
    }

    public IrdenFightSnapshot withCurrentPlayer(String playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");

        if (!playersInFight.containsKey(playerUuid)) {
            throw new IllegalArgumentException(
                    "Участник не найден в бою: " + playerUuid
            );
        }

        return copyWith(
                playersInFight,
                queue,
                playerUuid,
                turn
        );
    }


    public boolean containsPlayer(String playerUuid) {
        return playerUuid != null && playersInFight.containsKey(playerUuid);
    }

    @JsonIgnore
    public boolean isFormationTurn() {
        return turn == FORMATION_TURN
                && !queue.isEmpty()
                && Objects.equals(currentPlayerUuidTurn, queue.getFirst());
    }

    private IrdenFightSnapshot copyWith(
            Map<String, PlayerInFight> players,
            List<String> updatedQueue,
            String updatedCurrentPlayer,
            int updatedTurn
    ) {
        return new IrdenFightSnapshot(
                fightName,
                authorUuid,
                players,
                updatedQueue,
                updatedCurrentPlayer,
                updatedTurn,
                UUID.randomUUID()
        );
    }
}