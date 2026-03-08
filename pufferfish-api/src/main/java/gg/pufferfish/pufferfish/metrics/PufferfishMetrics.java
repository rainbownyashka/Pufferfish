package gg.pufferfish.pufferfish.metrics;

import org.jetbrains.annotations.Nullable;

public final class PufferfishMetrics {

    private static volatile @Nullable WorldTickLoadService worldTickLoadService;

    private PufferfishMetrics() {
    }

    public static @Nullable WorldTickLoadService worldTickLoadService() {
        return worldTickLoadService;
    }

    public static void setWorldTickLoadService(final @Nullable WorldTickLoadService service) {
        worldTickLoadService = service;
    }
}
