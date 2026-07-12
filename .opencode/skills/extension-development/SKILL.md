---
name: extension-development
description: Explore, plan, implement, and validate Katari website extensions against the configured Entry SDK. Use for new extensions, website capability audits, mixed-entry source design, or implementation of an already approved extension plan.
---

# Extension development

Use two phases: read-only discovery first, implementation only after explicit user approval.

## Discover and plan

Remain in Plan mode when the active surface supports it. Otherwise keep this phase read-only. Do not scaffold files, download icons, or implement code during discovery, even when the initial request says to build the extension.

1. Read `katariSourceApiVersion` from `gradle.properties`. For an `sdk-*` version, inspect the `entry-source-api` source and build configuration at the matching tag in `https://github.com/katariapp/katari` using GitHub pages, raw files, or the GitHub API. Inventory the public types and capabilities from that exact tag instead of relying on a fixed list or the default branch. Use the JitPack dependency configured by this repository for compilation; do not require or clone a local Katari checkout. Stop and report the unavailable tag or files if the published contract cannot be inspected.
2. Verify direct HTTP access to the website. Record redirects, cookies, authentication, anti-bot behavior, and geographic restrictions. For an SPA, inspect its network/API endpoints; reject it only when usable content requires JavaScript execution and no reproducible HTTP/API path exists.
3. Investigate rate limits conservatively; never flood the website to discover a threshold. Check published rules, rate-limit headers, `429` responses, and `Retry-After`. Distinguish limits by host and endpoint family, including catalogue, details, images, and playback. Record the supported request interval, concurrency, and retry behavior, or clearly mark them unknown when they cannot be established safely.
4. Make the same requests the extension would need for popular content, latest content, search and filters, entry details, children, and media resolution. Probe images, playback streams, subtitles, and applicable optional capabilities exposed by the configured SDK. Include traffic patterns caused by browsing, library updates, and downloads. Do not infer support without request evidence.
5. Treat the extension, factory, module, and `UnifiedSource` as type-agnostic. For each listing API, identify every content kind, map each item to its `EntryType`, and determine whether one response page can contain mixed types. Do not split sources solely because a website serves multiple content types.
6. Verify that every returned `SEntry` sets `type` explicitly and preserves it through details refresh and child retrieval. Do not rely on the current default or assume the configured SDK's `EntryType` and `EntryMedia` variants are permanently exhaustive.
7. Select source bases by capability: use `EntryHttpSource` for neutral HTTP/catalogue behavior and `EntryImageHttpSource` when the source needs image loading. The same source may return other `EntryMedia` variants. Add optional interfaces only for capabilities the website supports.
8. For a mixed source, retain a stable per-child media discriminator in the URL or a namespaced persisted `memo` value so `getMedia()` can return the correct media for that child.
9. Check `REMOVED_SOURCES.md` and the public contribution/legal policies. Stop if the provider was removed or the proposed implementation would require credentials, redistributed site content, access-control bypasses, or code that cannot be published under the repository license.

Report the results as a table with these columns:

| Capability or API | Website evidence | Supported entry types | Mixed in one result | Rate-limit evidence and safe policy | Status | Constraints |
| --- | --- | --- | --- | --- | --- | --- |

Also report the proposed factory/source structure, base classes and optional interfaces, entry-type mapping, media-dispatch strategy, unsupported capabilities, and blockers. Stop after the report. Require a later explicit user approval before implementation, and do not implement while the active mode forbids mutations.

## Implement an approved plan

Proceed only when the thread contains the discovery report, the user explicitly approved it, material choices are resolved, and repository mutations are allowed.

1. Follow the nonstandard module layout and manifest requirements in `AGENTS.md`.
2. Implement the extension as logically separated files. Keep the factory limited to source construction and the source class focused on Entry SDK overrides and orchestration; the factory file alone does not count as decomposition. Put nontrivial models, filters, parsing and mapping, preferences, interceptors, and static configuration in responsibility-named files when those concerns exist. Do not create unused placeholders, split mechanically into one type per file, or collect unrelated code in a generic utilities file.
3. Use `configureSharedExtensionModule`. Point `extClass` to an `EntrySourceFactory` that returns `List<UnifiedSource>`. Do not add extension-level content-type metadata.
4. Keep mixed content in one source when that matches the website. Implement suspend Entry APIs, set every `SEntry.type`, and dispatch `getMedia()` from stable per-child data.
5. Configure client throttling from the discovery evidence for every affected host or endpoint family. Use existing network/interceptor helpers, respect `Retry-After`, and avoid concurrent retries that amplify throttling.
6. Add a sibling `repo-metadata.json`. Make every source's `id`, `lang`, `name`, and `baseUrl` match the factory output.
7. Fetch the website's actual icon only after approval. Convert it locally without opening or reading the downloaded source icon into the conversation. Create `ic_launcher.png` at 48, 72, 96, 144, and 192 pixels for mdpi through xxxhdpi.
8. Derive a new extension's version from `katariSourceApiVersion` as `<SDK major>.<SDK minor>.0`; increment only the patch component for later extension releases. Increment `versionCode` for every release. For example, `sdk-2.0.0` produces `2.0.0`.
9. Do not add publication credentials, signing material, private endpoints, copied catalog/media data, or provider secrets. Ensure all new source files have license-compatible provenance.

## Validate

1. Review the affected package against the extension structure rules in `AGENTS.md`; reject a source file that accumulates unrelated implementation concerns behind a trivial factory split.
2. Run `python3 scripts/validate_repo_metadata.py`.
3. Build the affected module normally against the SDK configured in `gradle.properties`.
4. Run relevant focused tests and `git diff --check`.
5. Report website restrictions and environment failures separately from implementation defects.
