package irden.space.proxy.plugin.irden.account;

import irden.space.proxy.plugin.irden.persistence.model.AccountEntity;

import java.util.Objects;


public record StructureAccountTarget(StructureAccountType type, String name, AccountEntity account) {

    public StructureAccountTarget {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(account, "account");
    }
}
