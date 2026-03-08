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
- Full Gradle compile not finished yet in this session.
- `gradlew` wrapper required LF normalization for `bash` execution on this machine.
- Added repo-level `.gitattributes` with `eol=lf` to stop false WSL dirty state from CRLF-only working tree noise.
