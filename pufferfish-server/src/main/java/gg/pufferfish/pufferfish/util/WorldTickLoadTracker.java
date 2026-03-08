package gg.pufferfish.pufferfish.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.World;

public final class WorldTickLoadTracker {

    public static final WorldTickLoadTracker INSTANCE = new WorldTickLoadTracker();

    private static final int SHORT_WINDOW = 20;
    private static final int LONG_WINDOW = 100;
    private static final int STALE_TICK_TTL = 12000;

    private final Map<String, WorldStats> statsByDimension = new HashMap<>();
    private final Map<String, CurrentTickWorldSample> currentTickSamples = new HashMap<>();

    private int currentServerTick = -1;
    private long currentTickTotalNanos;

    private WorldTickLoadTracker() {
    }

    public synchronized void beginTick(final int serverTick) {
        this.currentServerTick = serverTick;
        this.currentTickTotalNanos = 0L;
        this.currentTickSamples.clear();
    }

    public synchronized void recordWorldTick(final ServerLevel level, final long durationNanos) {
        final String dimensionKey = level.dimension().location().toString();
        final World bukkitWorld = level.getWorld();
        final String worldName = bukkitWorld != null ? bukkitWorld.getName() : dimensionKey;

        final CurrentTickWorldSample previous = this.currentTickSamples.put(
            dimensionKey,
            new CurrentTickWorldSample(worldName, dimensionKey, Math.max(0L, durationNanos))
        );
        if (previous != null) {
            this.currentTickTotalNanos -= previous.durationNanos();
        }
        this.currentTickTotalNanos += Math.max(0L, durationNanos);
    }

    public synchronized void finishTick() {
        if (this.currentTickSamples.isEmpty()) {
            return;
        }

        final long totalNanos = Math.max(1L, this.currentTickTotalNanos);
        for (final CurrentTickWorldSample sample : this.currentTickSamples.values()) {
            this.statsByDimension
                .computeIfAbsent(sample.dimensionKey(), ignored -> new WorldStats(sample.worldName(), sample.dimensionKey()))
                .record(sample.worldName(), sample.durationNanos(), totalNanos, this.currentServerTick);
        }

        this.statsByDimension.values().removeIf(stats -> this.currentServerTick - stats.lastSeenTick > STALE_TICK_TTL);
        this.currentTickSamples.clear();
        this.currentTickTotalNanos = 0L;
    }

    public synchronized List<WorldLoadSnapshot> snapshot() {
        return this.statsByDimension.values().stream()
            .map(WorldStats::snapshot)
            .sorted(Comparator.comparingDouble(WorldLoadSnapshot::averageShare100).reversed())
            .toList();
    }

    public synchronized List<WorldLoadSnapshot> snapshotTop(final int limit) {
        if (limit <= 0) {
            return List.of();
        }
        final List<WorldLoadSnapshot> snapshots = new ArrayList<>(this.snapshot());
        return snapshots.size() <= limit ? snapshots : List.copyOf(snapshots.subList(0, limit));
    }

    public synchronized WorldLoadSnapshot snapshot(final String dimensionKey) {
        final WorldStats stats = this.statsByDimension.get(dimensionKey);
        return stats == null ? null : stats.snapshot();
    }

    public record WorldLoadSnapshot(
        String worldName,
        String dimensionKey,
        double lastTickMs,
        double averageMs20,
        double averageMs100,
        double averageShare20,
        double averageShare100,
        double maxMs100,
        int samples,
        int lastSeenTick
    ) {
    }

    private record CurrentTickWorldSample(String worldName, String dimensionKey, long durationNanos) {
    }

    private static final class WorldStats {
        private final String dimensionKey;
        private final RollingLongWindow nanos20 = new RollingLongWindow(SHORT_WINDOW);
        private final RollingLongWindow nanos100 = new RollingLongWindow(LONG_WINDOW);
        private final RollingDoubleWindow share20 = new RollingDoubleWindow(SHORT_WINDOW);
        private final RollingDoubleWindow share100 = new RollingDoubleWindow(LONG_WINDOW);

        private String worldName;
        private long lastTickNanos;
        private int lastSeenTick;

        private WorldStats(final String worldName, final String dimensionKey) {
            this.worldName = worldName;
            this.dimensionKey = dimensionKey;
        }

        private void record(final String latestWorldName, final long worldTickNanos, final long totalWorldNanos, final int serverTick) {
            this.worldName = latestWorldName;
            this.lastTickNanos = worldTickNanos;
            this.lastSeenTick = serverTick;
            this.nanos20.add(worldTickNanos);
            this.nanos100.add(worldTickNanos);
            final double share = (double) worldTickNanos * 100.0D / (double) Math.max(1L, totalWorldNanos);
            this.share20.add(share);
            this.share100.add(share);
        }

        private WorldLoadSnapshot snapshot() {
            return new WorldLoadSnapshot(
                this.worldName,
                this.dimensionKey,
                nanosToMillis(this.lastTickNanos),
                nanosToMillis(this.nanos20.average()),
                nanosToMillis(this.nanos100.average()),
                this.share20.average(),
                this.share100.average(),
                nanosToMillis(this.nanos100.max()),
                this.nanos100.size(),
                this.lastSeenTick
            );
        }
    }

    private static final class RollingLongWindow {
        private final long[] values;
        private int nextIndex;
        private int size;
        private long total;

        private RollingLongWindow(final int size) {
            this.values = new long[size];
        }

        private void add(final long value) {
            if (this.size < this.values.length) {
                this.values[this.nextIndex] = value;
                this.total += value;
                this.size++;
            } else {
                this.total -= this.values[this.nextIndex];
                this.values[this.nextIndex] = value;
                this.total += value;
            }
            this.nextIndex = (this.nextIndex + 1) % this.values.length;
        }

        private long average() {
            return this.size == 0 ? 0L : this.total / this.size;
        }

        private long max() {
            long max = 0L;
            for (int index = 0; index < this.size; index++) {
                max = Math.max(max, this.values[index]);
            }
            return max;
        }

        private int size() {
            return this.size;
        }
    }

    private static final class RollingDoubleWindow {
        private final double[] values;
        private int nextIndex;
        private int size;
        private double total;

        private RollingDoubleWindow(final int size) {
            this.values = new double[size];
        }

        private void add(final double value) {
            if (this.size < this.values.length) {
                this.values[this.nextIndex] = value;
                this.total += value;
                this.size++;
            } else {
                this.total -= this.values[this.nextIndex];
                this.values[this.nextIndex] = value;
                this.total += value;
            }
            this.nextIndex = (this.nextIndex + 1) % this.values.length;
        }

        private double average() {
            return this.size == 0 ? 0.0D : this.total / this.size;
        }
    }

    private static double nanosToMillis(final long nanos) {
        return nanos / 1_000_000.0D;
    }
}
