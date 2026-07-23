package irden.space.proxy.plugin.irden.account;

import irden.space.proxy.plugin.command_handler.ArgumentParseException;
import irden.space.proxy.plugin.command_handler.ArgumentType;
import irden.space.proxy.plugin.command_handler.CommandArgumentContext;
import irden.space.proxy.plugin.command_handler.CommandContext;
import irden.space.proxy.plugin.irden.persistence.model.AccountEntity;
import irden.space.proxy.plugin.irden.service.AccountService;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;


public final class StructureAccountArgumentType implements ArgumentType<StructureAccountTarget> {

    public static final String TYPE_ARGUMENT = "type";

    private final Supplier<AccountService> accountServiceSupplier;

    private StructureAccountArgumentType(Supplier<AccountService> accountServiceSupplier) {
        this.accountServiceSupplier = Objects.requireNonNull(accountServiceSupplier, "accountServiceSupplier");
    }

    public static StructureAccountArgumentType structureAccount(AccountService accountService) {
        Objects.requireNonNull(accountService, "accountService");
        return new StructureAccountArgumentType(() -> accountService);
    }

    public static StructureAccountArgumentType structureAccount(Supplier<AccountService> accountServiceSupplier) {
        return new StructureAccountArgumentType(accountServiceSupplier);
    }

    @Override
    public StructureAccountTarget parse(String input) throws ArgumentParseException {
        throw new ArgumentParseException("Не удалось определить вид счёта");
    }

    @Override
    public StructureAccountTarget parse(CommandArgumentContext context, String input) throws ArgumentParseException {
        if (input == null || input.isBlank()) {
            throw new ArgumentParseException("Имя счёта не должно быть пустым");
        }

        StructureAccountType type = resolveType(context);
        if (type == null) {
            throw new ArgumentParseException("Сначала укажите вид счёта");
        }

        AccountService accountService = accountServiceSupplier.get();
        if (accountService == null) {
            throw new ArgumentParseException("Сервис счетов ещё не готов");
        }

        String name = input.trim();
        try {
            AccountEntity account = accountService.getAccount(
                    type.ownerType(),
                    StructureAccountType.toOwnerId(name),
                    type.accountCode()
            );
            return new StructureAccountTarget(type, name, account);
        } catch (IllegalArgumentException e) {
            throw new ArgumentParseException("Счёт " + type.displayName() + " «" + name + "» не найден");
        }
    }

    @Override
    public boolean supportsAutocomplete() {
        return true;
    }

    @Override
    public List<String> suggestions(CommandContext context, String prefix) {
        StructureAccountType type = resolveType(context);
        if (type == null) {
            return List.of();
        }

        AccountService accountService = accountServiceSupplier.get();
        if (accountService == null) {
            return List.of();
        }

        String normalizedPrefix = prefix == null ? "" : prefix.trim().toLowerCase(java.util.Locale.ROOT);
        return accountService.getAccountsByOwnerTypeAndCode(type.ownerType(), type.accountCode()).stream()
                .map(AccountEntity::getOwnerName)
                .filter(name -> name.toLowerCase(java.util.Locale.ROOT).startsWith(normalizedPrefix))
                .distinct()
                .toList();
    }

    @Override
    public String displayName() {
        return "account";
    }

    private static StructureAccountType resolveType(CommandArgumentContext context) {
        return context == null ? null : context.getOrDefault(TYPE_ARGUMENT, StructureAccountType.class, null);
    }

    private static StructureAccountType resolveType(CommandContext context) {
        return context == null ? null : context.getOrDefault(TYPE_ARGUMENT, StructureAccountType.class, null);
    }
}
