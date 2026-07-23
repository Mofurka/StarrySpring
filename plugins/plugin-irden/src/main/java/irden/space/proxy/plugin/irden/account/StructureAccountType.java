package irden.space.proxy.plugin.irden.account;

import irden.space.proxy.plugin.irden.persistence.model.AccountOwnerType;

import java.util.Locale;


public enum StructureAccountType {
    FORTRESS(AccountOwnerType.BUILDING, "Крепость"),
    TAVERN(AccountOwnerType.BUILDING, "Таверна"),
    TEMPLE(AccountOwnerType.BUILDING, "Храм"),
    MARKET(AccountOwnerType.ORGANIZATION, "Рынок"),
    GUILD(AccountOwnerType.ORGANIZATION, "Гильдия"),
    SETTLEMENT(AccountOwnerType.LOCATION, "Поселение");

    private final AccountOwnerType ownerType;
    private final String displayName;

    StructureAccountType(AccountOwnerType ownerType, String displayName) {
        this.ownerType = ownerType;
        this.displayName = displayName;
    }

    public AccountOwnerType ownerType() {
        return ownerType;
    }

    public String displayName() {
        return displayName;
    }

    public String accountCode() {
        return name();
    }

    public static String toOwnerId(String name) {
        return name.trim().toUpperCase(Locale.ROOT);
    }
}
