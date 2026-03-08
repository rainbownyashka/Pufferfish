package gg.pufferfish.pufferfish.metrics;

import java.util.List;
import org.jetbrains.annotations.Nullable;

public interface WorldTickLoadService {

    List<WorldTickLoadSnapshot> snapshot();

    List<WorldTickLoadSnapshot> snapshotTop(int limit);

    @Nullable WorldTickLoadSnapshot snapshot(String dimensionKey);
}
