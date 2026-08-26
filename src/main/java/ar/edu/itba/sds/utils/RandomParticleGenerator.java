package ar.edu.itba.sds.utils;

import ar.edu.itba.sds.model.Entity2D;
import ar.edu.itba.sds.model.entities.SizedParticle;

import java.util.*;

public class RandomParticleGenerator {

    private static final int MAX_ATTEMPTS_PER_PARTICLE = 10_000;
    private static final float V_MODULE = (float) 0.03;

    public static Set<SizedParticle> generate(int n, float l, float rMin, float rMax, int seed) {
        Set<SizedParticle> particles = new HashSet<>();

        // Cell size >= 2 * rMax guarantees checking only the 3x3 neighboring cells is sufficient
        float cellSize = 2 * rMax;
        int gridDim = Math.max(1, (int) Math.ceil(l / cellSize));

        @SuppressWarnings("unchecked")
        List<SizedParticle>[][] grid = new List[gridDim][gridDim];
        for (int i = 0; i < gridDim; i++) {
            for (int j = 0; j < gridDim; j++) {
                grid[i][j] = new ArrayList<>();
            }
        }

        Random random = new Random(seed);

        for (int i = 0; i < n; i++) {
            boolean placed = false;
            int attempts = 0;

            while (!placed && attempts < MAX_ATTEMPTS_PER_PARTICLE) {
                attempts++;

                // Uniformly sample within valid boundary bounds [r, l - r]
                float r = rMin + random.nextFloat() * (rMax - rMin);
                float x = r + random.nextFloat() * (l - 2 * r);
                float y = r + random.nextFloat() * (l - 2 * r);
                float angle = random.nextFloat() * 360;

                SizedParticle p = new SizedParticle(x, y, r,V_MODULE, angle);

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

    private static boolean hasCollisionGrid(Entity2D p, List<SizedParticle>[][] grid, int cellX, int cellY, int gridDim) {
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