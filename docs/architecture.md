# Architecture

Pug Alt Manager is a client-side Forge mod. Its code is organized beneath `dev.pugrilla.altmanager` by runtime responsibility so related APIs remain discoverable without flattening the entire application into one package.

## Runtime components

| Area | Primary classes | Responsibility |
|---|---|---|
| Forge entry point | `AltManager`, `ClientEventListener` | Mod initialization, Forge event registration, and top-level services |
| Accounts | `AbstractAccount` and account subclasses | Session creation, login behavior, rendering metadata, and account actions |
| Repositories | `AccountRepository`, `RepositoryEncryption` | Account grouping, selection, filtering, encryption, and repository state |
| Persistence | `StorageManager`, `FileStorageManager`, `RepositoryFileCodec` | Binary storage, auto-save, import/export, preferences, and ban-expiry state |
| Authentication | Request/response classes and `MinecraftServicesApi` | Mojang, Microsoft, Xbox Live, XSTS, and Minecraft Services calls |
| User interface | `AltManagerScreen` and screen/widget classes | Account, repository, preference, profile, and skin workflows |
| Networking | `BanCheckNetworkManager`, channel initializers, `HypixelBanHandler` | Short-lived login connection used to infer Hypixel ban status |
| Utilities | `AltManagerUtils`, `IOUtils`, `ColorUtils`, `MinecraftReflection` | Formatting, parsing, crypto helpers, rendering colors, and session injection |
| Native dialogs | `dev.pugrilla.jnafilechooser` | Windows common dialogs with a Swing fallback |

## Startup flow

1. Forge constructs `AltManager`.
2. `FileStorageManager` loads `%APPDATA%/.pugaltmanager/accounts.dat` and copies legacy `.micsaltman` data on first use when needed.
3. `AltManagerScreen` and the client event listener are created.
4. During `FMLInitializationEvent`, the event listener is registered on the Forge event bus.
5. GUI events expose the account manager from supported Minecraft screens.

## Storage model

The storage file is a versioned binary format with a magic header. It contains global preferences, repositories, account records, encrypted account payloads, ban-expiry entries, and UI selections. Repository export uses a separate versioned binary envelope.

The reconstruction intentionally preserves field order, enum ordinals, magic values, and encryption behavior so existing account databases remain compatible.

## Threading model

Network and long-running account operations use a fixed four-thread executor owned by `AltManager`. `AsyncTaskLock` tracks active work for GUI locking and error reporting. Minecraft state and GUI callbacks are scheduled back onto the client thread where the original implementation did so.

`FileStorageManager` also owns a periodic auto-save thread and a JVM shutdown hook.
