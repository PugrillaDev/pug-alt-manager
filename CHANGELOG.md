# Changelog

All notable changes to Pug Alt Manager are documented here.

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
