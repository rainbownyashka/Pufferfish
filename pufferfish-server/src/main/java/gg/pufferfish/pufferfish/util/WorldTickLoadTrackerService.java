package gg.pufferfish.pufferfish.util;

import gg.pufferfish.pufferfish.metrics.WorldTickLoadService;
import gg.pufferfish.pufferfish.metrics.WorldTickLoadSnapshot;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class WorldTickLoadTrackerService implements WorldTickLoadService {

    public static final WorldTickLoadTrackerService INSTANCE = new WorldTickLoadTrackerService();

    private WorldTickLoadTrackerService() {
    }

    @Override
    public List<WorldTickLoadSnapshot> snapshot() {
        return WorldTickLoadTracker.INSTANCE.snapshot().stream()
            .map(WorldTickLoadTrackerService::toApiSnapshot)
            .toList();
    }

    @Override
    public List<WorldTickLoadSnapshot> snapshotTop(final int limit) {
        return WorldTickLoadTracker.INSTANCE.snapshotTop(limit).stream()
            .map(WorldTickLoadTrackerService::toApiSnapshot)
            .toList();
    }

    @Override
    public @Nullable WorldTickLoadSnapshot snapshot(final String dimensionKey) {
        final WorldTickLoadTracker.WorldLoadSnapshot snapshot = WorldTickLoadTracker.INSTANCE.snapshot(dimensionKey);
        return snapshot == null ? null : toApiSnapshot(snapshot);
    }

    private static WorldTickLoadSnapshot toApiSnapshot(final WorldTickLoadTracker.WorldLoadSnapshot snapshot) {
        return new WorldTickLoadSnapshot(
            snapshot.worldName(),
            snapshot.dimensionKey(),
            snapshot.lastTickMs(),
            snapshot.averageMs20(),
            snapshot.averageMs100(),
            snapshot.averageShare20(),
            snapshot.averageShare100(),
            snapshot.maxMs100(),
            snapshot.samples(),
            snapshot.lastSeenTick()
        );
    }
}
