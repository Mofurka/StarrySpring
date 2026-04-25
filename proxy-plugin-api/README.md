# Permission API cheatsheet

Короткая памятка по работе с permissions в плагинах.

## 1. Регистрация permission'ов

```java
import irden.space.proxy.plugin.api.Permission;
import irden.space.proxy.plugin.api.PermissionRegistry;

public final class ChatPermissions {
    public static final Permission KICK = PermissionRegistry.register("starry.chat.kick");
    public static final Permission BAN = PermissionRegistry.register("starry.chat.ban");

    private ChatPermissions() {
    }
}
```

Пакетная регистрация:

```java
var permissions = PermissionRegistry.registerAll(
        "starry.chat.mute",
        "starry.chat.unmute"
);
```

## 2. Проверка прав у соединения

Через `PluginSessionContext`:

```java
if (!context.permissions().has(ChatPermissions.KICK)) {
    return;
}
```

Несколько прав:

```java
if (context.permissions().hasAny(ChatPermissions.KICK, ChatPermissions.BAN)) {
    // есть хотя бы одно право
}

if (context.permissions().hasAll(ChatPermissions.KICK, ChatPermissions.BAN)) {
    // есть оба права
}
```

Через helper-класс `Permissions`:

```java
import irden.space.proxy.plugin.api.Permissions;

if (!Permissions.has(context, ChatPermissions.KICK)) {
    return;
}

if (Permissions.hasAny(context, ChatPermissions.KICK, ChatPermissions.BAN)) {
    // shortcut
}
```

## 3. Built-in право `all`

В системе уже зарегистрировано встроенное право:

```java
PermissionRegistry.ALL
```

Если оно выдано сессии, проверка любого другого permission возвращает `true`.

```java
import irden.space.proxy.plugin.api.PermissionSet;
import irden.space.proxy.plugin.api.PermissionRegistry;

PermissionSet set = new PermissionSet();
set.grant(PermissionRegistry.ALL);

assert set.has(ChatPermissions.KICK);
```

То же самое короче:

```java
PermissionSet set = Permissions.allAccess();
```

## 4. Быстрое создание PermissionSet

```java
import irden.space.proxy.plugin.api.PermissionSet;
import irden.space.proxy.plugin.api.Permissions;

PermissionSet empty = Permissions.none();
PermissionSet moderator = Permissions.granted(ChatPermissions.KICK, ChatPermissions.BAN);
PermissionSet admin = Permissions.allAccess();
```

## 5. Что делает player-manager

`plugin-player-manager` больше не нужен для самой проверки прав.

Его зона ответственности сейчас:
- роли
- наследование ролей
- резолвинг string rules / wildcard
- запись итогового `PermissionSet` за сессией

Сама проверка прав делается через API:
- `Permission`
- `PermissionRegistry`
- `PermissionView`
- `PluginSessionContext.permissions()`
- `PermissionSet`

## 6. Wildcard rules

Wildcard (`starry.chat.*`) резолвятся при сборке effective permissions, а не в hot path проверки.

То есть в рантайме проверка остаётся быстрой:

```java
context.permissions().has(ChatPermissions.KICK)
```

