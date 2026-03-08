package gg.pufferfish.pufferfish.metrics;

import java.util.UUID;

public record WorldTickLoadSnapshot(
    String worldName,
    String dimensionKey,
    UUID worldUid,
    String bukkitWorldKey,
    double lastTickMs,
    double averageMs20,
    double averageMs100,
    double averageShare20,
    double averageShare100,
    double maxMs100,
    int loadedEntityCount,
    int blockEntityTickerCount,
    long pendingBlockTickCount,
    long pendingFluidTickCount,
    int loadedChunkCount,
    int tickingChunkCount,
    int pendingChunkTaskCount,
    int samples,
    int lastSeenTick
) {
}
