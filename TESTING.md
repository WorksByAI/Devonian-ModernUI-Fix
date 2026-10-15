# 1.0.1 verification

Minecraft 26.1.2, Java 25, Modern UI 3.13.0.5.

The current official Devonian release for Minecraft 26.1.x is [1.34.9](https://modrinth.com/mod/devonian/version/EQT06WHl), checked on 2026-10-15. The installed JAR matches the official Modrinth SHA-512 digest.

| Devonian | Loader | Original bypass reproduced | Patched Modern UI chain | Text engine disabled |
| --- | --- | --- | --- | --- |
| [1.32.9](https://modrinth.com/mod/devonian/version/hb3fQ1we) | 0.19.2 | Pass | Pass | Pass |
| [1.33.9](https://modrinth.com/mod/devonian/version/fqzglRSm) | 0.19.2 | Pass | Pass | Pass |
| [1.34.9](https://modrinth.com/mod/devonian/version/EQT06WHl) | 0.19.2 | Pass | Pass | Pass |
| 1.34.9 | 0.19.5 | — | Pass | — |

Each run contains 48 assertions: 480 in total. Verification uses the original released mixin classes, Fabric Loader, MixinExtras and Minecraft classes. It preserves text identity, obfuscated/bold styles, colors, shadow, fractional coordinates, bounds and cached preparation.

The Modern UI GPU/layout endpoint is replaced by a parameter-recording boundary. This proves the repaired call chain but does not constitute visual testing of the user's full modpack.

Additionally, six Java regression tests and three Python fixture tests pass. These check dependency declarations, operation-slot forwarding, rejection of unknown wrappers, distribution contents, original class preservation and unsupported-version rejection.

Original third-party JARs and extracted fixtures are local verification dependencies and are not included in the patch or source delivery. Official release URLs/digests and summarized results are recorded in `verification/official-releases.json` and `verification/verified-results.json`.
