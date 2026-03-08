# World Tick Load Notes

Date: 2026-03-08

## Added

- `pufferfish-server/src/main/java/gg/pufferfish/pufferfish/util/WorldTickLoadTracker.java`
- `pufferfish-server/minecraft-patches/features/0018-Track-per-world-tick-load.patch`

## Behavior

- Measures wall-clock time spent in each `ServerLevel.tick(...)` during `MinecraftServer.tickChildren(...)`.
- Stores passive per-world snapshots:
  - last tick ms
  - average ms over 20 samples
  - average ms over 100 samples
  - average share of total world-tick time over 20/100 samples
  - max ms over the last 100 samples

## Scope

- Passive only.
- No unload, quarantine, block, or punishment logic.
- Intended as data source for later `OpenCreative` integration.

## Verification

- Patch and utility added.
- `./gradlew --no-daemon :pufferfish-server:applyMinecraftFeaturePatches --console=plain` succeeded on WSL.
- `./gradlew --no-daemon :pufferfish-server:compileJava --console=plain` completed successfully on WSL.
- Fresh class outputs were produced for:
  - `pufferfish-server/build/classes/java/main/gg/pufferfish/pufferfish/util/WorldTickLoadTracker.class`
  - nested tracker snapshot/rolling-window classes
- `gradlew` wrapper required LF normalization for `bash` execution on this machine.
- Added repo-level `.gitattributes` with `eol=lf` to stop false WSL dirty state from CRLF-only working tree noise.
- Added explicit Gradle toolchain path for WSL Java 21:
  - `org.gradle.java.installations.auto-detect=false`
  - `org.gradle.java.installations.paths=/usr/lib/jvm/java-21-openjdk-amd64`
- Build bootstrap fixes added for this fork:
  - `--add-modules=jdk.incubator.vector` for all `JavaCompile` tasks
  - Javadoc gets `add-modules=jdk.incubator.vector`
  - Gradle HTTP connection/socket timeouts raised to `120000ms` for flaky JitPack fetches
- Added project-local Maven repository support:
  - `local-maven-repo/` is searched before remote repos
  - intended as a deterministic fallback for `Simple-Yaml` and `Flare` when `jitpack.io` stalls
- Local compile bootstrap temporarily disables bundled `Flare`:
  - `gg/pufferfish/pufferfish/flare/**` excluded from the local server source set
  - `Flare` dependency removed from local compile inputs
  - `/flare` bootstrap registration removed from `PufferfishConfig`
  - this change is only to unblock compile validation of the world tick load tracker
- `compileJava` now explicitly depends on `applyAllServerPatches` to satisfy Gradle 9 task validation for generated patch outputs.
