# Pug Alt Manager

A readable, buildable Minecraft Forge 1.8.9 account manager maintained by PugrillaDev.

This project reconstructs the behavior and storage format of Mic's Alt Manager 1.0 as maintainable Java source, then organizes it under the `dev.pugrilla` namespace using feature-oriented packages. It includes reproducible build, verification, and local-install scripts.

## Features

- Session, credentials, cookie, and Microsoft browser account flows
- Multiple local account repositories with optional encryption
- Repository import and export
- Account search, sorting, and multi-selection
- Automatic session refresh and persistence
- Skin upload, skin stealing, and random-skin support
- Hypixel ban-status checks
- Native Windows file dialogs with a Swing fallback

## Requirements

- Windows, macOS, or Linux for building
- Java Development Kit 8
- Minecraft 1.8.9
- Minecraft Forge 11.15.1.2318

ForgeGradle 2.1 is intentionally retained because this project targets Minecraft 1.8.9. Newer Java versions are not supported by this build.

## Quick start

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-1.8'
.\gradlew.bat clean build
```

The production and source artifacts are generated in `build/libs/`:

- `pug-alt-manager-1.0.jar`
- `pug-alt-manager-1.0-sources.jar`

Convenience scripts are also available:

```powershell
.\scripts\build.ps1
.\scripts\verify.ps1
.\scripts\install.ps1
```

`install.ps1` builds the project and copies the production JAR into the selected Minecraft `mods` directory. It does not delete unrelated mods.

## Project layout

```text
.
├── .github/                 Issue and pull-request templates
├── docs/                    Architecture and reconstruction notes
├── gradle/wrapper/          Pinned Gradle 4.10.3 wrapper
├── scripts/                 Build, verification, and install helpers
├── src/main/java/dev/pugrilla/
│   ├── altmanager/
│   │   ├── account/         Account models and login strategies
│   │   ├── auth/            Microsoft, Xbox, and Minecraft authentication
│   │   ├── client/          Forge client events and Minecraft integration
│   │   ├── gui/             Screens, widgets, and renderers
│   │   ├── network/         HTTP contracts and ban-check networking
│   │   ├── skin/            Skin data and providers
│   │   ├── storage/         Persistence, repository codecs, and encryption
│   │   └── util/            Shared utilities
│   └── jnafilechooser/      Bundled native file-dialog compatibility code
├── src/main/resources/      Forge metadata and resources
├── build.gradle             ForgeGradle build definition
├── gradle.properties        Project and toolchain versions
└── settings.gradle          Gradle project identity
```

## Documentation

- [Architecture](docs/architecture.md)
- [Deobfuscation and verification](docs/deobfuscation.md)
- [Original class mapping](docs/class-mapping.md)
- [Contributing](CONTRIBUTING.md)
- [Security policy](SECURITY.md)
- [Project notice](NOTICE.md)

## Verification status

- All reconstructed source compiles with Java 8.
- ForgeGradle `reobfJar` produces an installable Minecraft 1.8.9 artifact.
- The artifact retains the original 78-class footprint.
- No original `f75` package or decompiler placeholder remains.
- An isolated Forge client smoke test completed the full mod-loading lifecycle.

Run `scripts/verify.ps1` to repeat the local structural checks.

## Provenance

Pug Alt Manager is maintained and packaged by PugrillaDev. The reconstruction was produced from `micsaltmanager-rel (8).jar`, SHA-256 `77BB5CEA319DBE12229558B74E7DA54F1581D5D77C3D2098B3AA437504142C63`.

This repository layout is not a claim of affiliation with Mojang, Microsoft, Forge, Hypixel, or the original author. See [NOTICE.md](NOTICE.md) before redistributing the code or binaries.
