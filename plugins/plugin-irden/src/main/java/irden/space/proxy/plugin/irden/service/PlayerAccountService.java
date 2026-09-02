package irden.space.proxy.plugin.irden.service;

import irden.space.proxy.plugin.irden.integration.persistence.model.PlayerAttributesEntity;
import irden.space.proxy.plugin.irden.integration.persistence.repository.PlayerAttributesRepository;
import irden.space.proxy.plugin.irden.persistence.model.account.AccountEntity;
import irden.space.proxy.plugin.irden.persistence.model.account.AccountOwnerType;
import irden.space.proxy.plugin.irden.service.exception.AccountNotFoundException;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.OptionalLong;


@Service
@RequiredArgsConstructor
public class PlayerAccountService {

    private final PlayerAttributesRepository playerAttributesRepository;
    private final AccountService accountService;

    @Transactional(readOnly = true)
    public OptionalLong findApplicationId(String playerUuid) {
        if (playerUuid == null || playerUuid.isBlank()) {
            return OptionalLong.empty();
        }

        return playerAttributesRepository.findByPlayerUuid(playerUuid)
                .map(PlayerAttributesEntity::getApplicationId)
                .map(OptionalLong::of)
                .orElseGet(OptionalLong::empty);
    }

    @Transactional
    public AccountEntity initPlayerMainAccount(String applicationId) {
        return accountService.createAccount(AccountOwnerType.CHARACTER, applicationId, applicationId, "MAIN");
    }

    @Transactional(readOnly = true)
    public AccountEntity getMainAccount(StarUuid playerUuid) throws AccountNotFoundException {
        return getMainAccount(playerUuid == null ? null : playerUuid.toString());
    }

    @Transactional(readOnly = true)
    public AccountEntity getMainAccount(String playerUuid) throws AccountNotFoundException {
        OptionalLong applicationId = findApplicationId(playerUuid);
        if (applicationId.isEmpty()) {
            throw new AccountNotFoundException(
                    "Player %s is not linked to the site, so it has no account".formatted(playerUuid)
            );
        }

        return accountService.getPlayerMainAccount(applicationId.getAsLong());
    }
}
