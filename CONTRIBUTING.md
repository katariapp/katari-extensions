# Contributing

Contributions are welcome for existing extensions and, after prior discussion, new sources.

## Before opening a pull request

1. For a new source, open an issue describing the website, language, content types, and why it is suitable for public distribution.
2. Check `REMOVED_SOURCES.md`. Removed sources may not be reintroduced without explicit maintainer authorization.
3. Preserve published Android package identity, source IDs, and stored entry/child URLs.
4. Increase `versionCode` for every extension release and the final component of `versionName` for changes targeting the same Entry API family.
5. Keep each source's optional repository `supportedEntryTypes` metadata aligned with its runtime `SourceMetadata` declaration.
6. Keep the extension decomposed according to `AGENTS.md`.

## Validate locally

```bash
python3 scripts/validate_repo_metadata.py
./gradlew :src:all:rezka:assembleDebug
git diff --check
```

Replace the module path for another extension. Builds resolve the tagged Katari SDK configured in `gradle.properties` from JitPack; a local Katari checkout is not required.

## OpenCode workflow

The bundled `/develop-extension <website>` command invokes the `extension-development` skill. Its first phase is deliberately read-only: it inspects the configured SDK tag from Katari's public GitHub repository, website capabilities, media behavior, and safe request policy before proposing an implementation. Implementation requires explicit approval after that report.

## Pull requests

Pull requests must pass CI and receive approval from a reviewer with write permission. Maintainers may decline extensions that are unsafe, legally unsuitable, unmaintainable, duplicative, or incompatible with Katari's public SDK.
