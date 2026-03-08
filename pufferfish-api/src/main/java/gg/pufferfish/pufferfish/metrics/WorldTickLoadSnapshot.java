package gg.pufferfish.pufferfish.metrics;

public record WorldTickLoadSnapshot(
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
