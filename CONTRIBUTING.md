# Contributing

Thank you for helping improve Pug Alt Manager.

## Development setup

1. Install a Java 8 JDK.
2. Set `JAVA_HOME` to that JDK.
3. Run `./gradlew setupDecompWorkspace` once when preparing an IDE workspace.
4. Run `./gradlew clean build` before submitting a change.
5. Run `scripts/verify.ps1` on Windows to perform the structural artifact checks.

The Gradle wrapper is pinned to 4.10.3 because ForgeGradle 2.1 is not compatible with modern Gradle or Java releases.

## Change guidelines

- Preserve compatibility with Minecraft 1.8.9 and Java 8.
- Preserve the on-disk account repository format unless a migration is included.
- Never commit account databases, access tokens, refresh tokens, cookies, or session codes.
- Keep network timeouts and error handling explicit.
- Use descriptive class and member names; do not reintroduce SRG-style placeholders for mod-owned code.
- Treat bundled file-chooser code as a compatibility component and test Windows and Swing paths when changing it.

## Pull requests

Describe the user-visible behavior, testing performed, and any storage or authentication implications. Keep unrelated cleanup out of focused fixes where practical.
