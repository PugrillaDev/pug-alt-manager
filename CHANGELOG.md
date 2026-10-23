# Changelog

All notable changes to Pug Alt Manager are documented here.

## 1.2.2 - 2026-10-23

### Fixed

- Render player heads at full opacity independently of account-row selection and hover opacity
- Preserve the skin's own transparent hat pixels without making opaque hair translucent or washing out face colors
- Verify production-rendered pixels against a cached skin across normal, hovered, and selected account rows

## 1.2.1 - 2026-10-23

### Fixed

- Prevent a startup crash when restoring a saved account selection before the storage manager finishes construction
- Treat unexpected runtime failures as program errors instead of attempting database recovery or reporting account corruption
- Remove the misleading instruction to delete account data when loading fails

### Verified

- Regression test reproduces the 1.2 crash using real manager construction and a populated account database
- All 34 regression tests pass; the production Forge client starts and restarts with a populated account database and restores the selected account
- Test account storage is isolated from the user's APPDATA directory

## 1.2 - 2026-10-23

### Added

- Shared player-head cache by UUID, persistent texture metadata, Minecraft's cached skin images, background refresh, and failure cooldowns
- Session validation before account activation, expiry-aware refresh, and automatic recovery after invalid-session disconnects
- Explicit progress and failure messages for login and refresh operations
- Backup recovery for damaged account databases while preserving the damaged file

### Fixed

- Apply a session once after authentication finishes; ignore obsolete switch results and duplicate clicks
- Serialize refresh requests for each account and keep session changes on the Minecraft client thread
- Refresh Microsoft browser accounts without depending on the callback server's previous port; persist rotated refresh tokens
- Save the auto-refresh preference and other changed settings across restarts, including compatibility with older databases
- Open account lists at the top; use the displayed sort/filter order for scrolling and range selection
- Clear the stored search when Escape clears the search field, and avoid sorting the full list every tick
- Clear stale account selection after deletion and prevent operations on locked repositories
- Cancel account moves on Escape, prevent moves into locked repositories, and register imports only after parsing succeeds
- Use the checked account's session for ban checks, handle compression correctly, close timed-out checks, and display permanent bans correctly
- Avoid recording unrelated server disconnects as successful Hypixel ban checks
- Fix skin copying when no integrated server exists, bound HTTP requests, and handle error statuses before parsing response bodies
- Validate browser OAuth state, restrict the callback listener to loopback, and respond to failed callbacks
- Mask account secrets in input fields and remove access tokens and recursive repository output from diagnostics
- Accept valid usernames containing leading, trailing, or repeated underscores; preserve separators in pasted passwords
- Handle malformed cookie rows, HttpOnly cookies, Windows newlines, and lookalike domains
- Restore credential-login cookie state on every exit, release task locks on scheduling failures, and return UI changes to the client thread
- Validate stored collection and payload lengths before allocation
- Derive installer and verification artifact names from the project version; back up old Pug Alt Manager JARs outside the active mods folder

## 1.1 - 2026-09-10

### Security

- Protect Minecraft access tokens inside the password-encrypted account payload for encrypted repositories
- Prevent plaintext-token legacy databases from being copied into new backups before migration
- Mask repository creation and unlock password fields while they are rendered

### Fixed

- Stop retrying failed or unavailable player-skin requests every render cycle
- Keep all profile and skin discovery work off the render thread

## 1.0 - 2026-09-02

### Added

- Complete readable Java source for the original mod-owned classes
- Semantic class, field, method, and enum names
- Reconstructed lambda bodies and compiler-generated control flow
- Reproducible ForgeGradle 2.1 build for Minecraft 1.8.9
- Gradle wrapper, repository documentation, verification scripts, and contribution templates
- Pug Alt Manager branding and `dev.pugrilla` package namespace
- Feature-oriented account, authentication, client, GUI, network, skin, storage, and utility packages
- Non-destructive migration from legacy `.micsaltman` account storage to `.pugaltmanager`

### Verified

- Production JAR reobfuscation
- Original 78-class artifact footprint
- Removal of the original `f75` package and decompiler placeholders
- Successful isolated Forge client initialization
