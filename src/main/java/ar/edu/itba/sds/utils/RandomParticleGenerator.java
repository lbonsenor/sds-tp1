package ar.edu.itba.sds.utils;

import ar.edu.itba.sds.model.entities.Entity2D;

import java.util.*;

public class RandomParticleGenerator {

    private static final int MAX_ATTEMPTS_PER_PARTICLE = 10_000;
    private static final float V_MODULE = 0.03f;

    @FunctionalInterface
    public interface ParticleFactory<T extends Entity2D> {
        T create(float x, float y, float r, float v, float angle);
    }

    public static <T extends Entity2D> Set<T> generate(
            int n,
            float l,
            float rMin,
            float rMax,
            int seed,
            ParticleFactory<T> factory
    ) {
        Set<T> particles = new HashSet<>();
        Random random = new Random(seed);

        // Point particles path (rMin == 0 and rMax == 0)
        if (rMax == 0) {
            for (int i = 0; i < n; i++) {
                float x = random.nextFloat() * l;
                float y = random.nextFloat() * l;
                float angle = (float) (random.nextDouble() * 2 * Math.PI); // Native radians [0, 2pi)

                particles.add(factory.create(x, y, 0.0f, V_MODULE, angle));
            }
            return particles;
        }

        // Hard-sphere particles initialization (if r > 0)
        float cellSize = 2 * rMax;
        int gridDim = Math.max(1, (int) Math.ceil(l / cellSize));

        @SuppressWarnings("unchecked")
        List<T>[][] grid = new List[gridDim][gridDim];
        for (int i = 0; i < gridDim; i++) {
            for (int j = 0; j < gridDim; j++) {
                grid[i][j] = new ArrayList<>();
            }
        }

        for (int i = 0; i < n; i++) {
            boolean placed = false;
            int attempts = 0;

            while (!placed && attempts < MAX_ATTEMPTS_PER_PARTICLE) {
                attempts++;

                float r = rMin + random.nextFloat() * (rMax - rMin);
                float x = r + random.nextFloat() * (l - 2 * r);
                float y = r + random.nextFloat() * (l - 2 * r);
                float angle = (float) (random.nextDouble() * 2 * Math.PI); // Native radians [0, 2pi)

                T p = factory.create(x, y, r, V_MODULE, angle);

                int cellX = Math.min((int) (x / cellSize), gridDim - 1);
                int cellY = Math.min((int) (y / cellSize), gridDim - 1);

                if (!hasCollisionGrid(p, grid, cellX, cellY, gridDim)) {
                    particles.add(p);
                    grid[cellX][cellY].add(p);
                    placed = true;
                }
            }

            if (!placed) {
                throw new IllegalStateException(
                        String.format("Failed to place particle %d after %d attempts. Packing density is too high.",
                                i + 1, MAX_ATTEMPTS_PER_PARTICLE)
                );
            }
        }

        return particles;
    }

    private static <T extends Entity2D> boolean hasCollisionGrid(
            T p,
            List<T>[][] grid,
            int cellX,
            int cellY,
            int gridDim
    ) {
        int minX = Math.max(0, cellX - 1);
        int maxX = Math.min(gridDim - 1, cellX + 1);
        int minY = Math.max(0, cellY - 1);
        int maxY = Math.min(gridDim - 1, cellY + 1);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (Entity2D other : grid[x][y]) {
                    if (p.collidesWith(other)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}