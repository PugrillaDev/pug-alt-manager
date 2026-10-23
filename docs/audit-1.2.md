# Version 1.2 audit

The audit covered account models, Microsoft/Xbox/Minecraft authentication, session activation, client event hooks, all account and repository screens, player heads and skin operations, ban-check networking, storage/import/export, task scheduling, native file-dialog integration, and release/install scripts. Findings and fixes are listed in CHANGELOG.md.

## Session behavior

A login checks the cached token off the client thread. Refreshable accounts renew tokens that are expired, close to expiry, or explicitly rejected by Minecraft Services. Opaque tokens use a one-hour fallback age. A successful result changes the game session once, on the client thread. A newer switch, deleted account, or moved account prevents an obsolete result from activating. Browsing selection is independent of session activation. Authentication failures leave the current game session intact. Manual refresh updates the game session only if that account is still active.

Invalid-session disconnects trigger a refresh for the matching active, unlocked, refreshable account when automatic refresh is enabled. Reconnect after the refresh completes. Session-only accounts have no renewable credentials and must be replaced when their token expires. Revoked Microsoft refresh tokens, expired cookies, changed passwords, or interactive sign-in requirements require signing in again.

## Heads

The manager shares textures by UUID across repositories. UUID-to-texture metadata is stored under `.pugaltmanager/heads` in the account-storage directory. Minecraft's SkinManager persists the PNG data in its skin cache. Cached heads remain available while stale metadata refreshes in the background. Profile failures use a five-minute cooldown, and successful lookups have a 24-hour freshness window. Skin changes invalidate the shared entry. Profile/skin tasks use a separate worker pool so they cannot occupy all login workers.

## Compatibility and verification

The repository format remains compatible with 1.1. The account database appends an optional auto-refresh preference; older databases default to enabled. Existing password-encrypted and legacy repository tests remain part of the suite. No live personal account credentials are used in automated tests. Tests use controlled session results, local HTTP endpoints, and temporary storage directories. A production Forge smoke test verifies mod loading separately from the unit tests.

The inherited password-based Microsoft flow depends on Microsoft's interactive login page and cannot complete MFA or other user-interaction requirements. Browser sign-in is available for these cases. The legacy repository cipher is preserved for compatibility; this update does not migrate encryption formats.

## Release validation

The Java 8 production build and regression suite pass. An isolated Forge 11.15.1.2318 client loads the production JAR through load-complete. A test-only attached harness creates 80 fixture accounts and verifies the initial scroll position, reopening after scrolling, Escape search clearing, and preference persistence against the production classes. No fixture accounts or runtime test harness classes are packaged in the release.

## Session presentation and startup in 1.2.3

The green ACTIVE badge follows Minecraft's current profile UUID. Highlighting a row does not select a game identity, and session progress/errors stay with their account row. The account list reserves a footer for repository or other notices instead of drawing text over account entries.

Startup chooses the greatest nonzero last-used timestamp across all saved repositories. Never-used rows are excluded. If the latest account is locked or authentication fails, Minecraft keeps its existing session and does not try an unrelated account. A successful explicit switch saves its timestamp immediately; restoring at startup leaves it unchanged. The existing database format already stores these timestamps, so no account-file migration is required.

All 44 regression tests pass for 1.2.3. The production Forge client was also checked with isolated fixture accounts: the actual rendered screen shows the badge on the active account independently of the highlighted row, and refreshing/error messages remain in their own rows. Restarting that client restores the last-used account while a different never-used row remains highlighted. Failed fixture authentication does not mark that highlighted row active. These checks use fake credentials and do not verify live personal sign-in.
