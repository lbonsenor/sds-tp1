package ar.edu.itba.sds.service;

import ar.edu.itba.sds.model.entities.SizedParticle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class CellIndexService<T extends SizedParticle> {

    private final int M;
    private final float L;
    private final float rc;
    private final List<T>[][] grid;

    // Neighbor offsets for 2D symmetric Cell Index Method:
    private static final int[][] NEIGHBOR_OFFSETS = {
            {1, 0},
            {1, 1},
            {0, 1},
            {1, -1}
    };

    @SuppressWarnings("unchecked")
    public CellIndexService(int M, float L, float rc, Collection<T> particles) {
        this.M = M;
        this.L = L;
        this.rc = rc;
        float cellSize = L / M;
        this.grid = new ArrayList[M][M];

        for (int i = 0; i < M; i++) {
            for (int j = 0; j < M; j++) {
                grid[i][j] = new ArrayList<>();
            }
        }

        // Assign particles to cells
        for (T particle : particles) {
            float x = (particle.getMaxX() + particle.getMinX()) * 0.5f;
            float y = (particle.getMaxY() + particle.getMinY()) * 0.5f;

            int cellX = Math.min((int) (x / cellSize), M - 1);
            int cellY = Math.min((int) (y / cellSize), M - 1);
            grid[cellX][cellY].add(particle);
        }
    }

    /**
     * Executes Symmetric Cell Index Method.
     */
    public void calculateNeighbors(boolean contour) {
        for (int x = 0; x < M; x++) {
            for (int y = 0; y < M; y++) {
                List<T> currentCell = grid[x][y];
                int currentCellSize = currentCell.size();

                if (currentCellSize == 0) {
                    continue;
                }

                // 1. Intra-cell comparisons (within the same cell)
                for (int i = 0; i < currentCellSize; i++) {
                    T p1 = currentCell.get(i);
                    for (int j = i + 1; j < currentCellSize; j++) {
                        T p2 = currentCell.get(j);
                        checkAndAddNeighbor(p1, p2, contour);
                    }
                }

                // 2. Inter-cell comparisons (Symmetric adjacent cells)
                for (int[] offset : NEIGHBOR_OFFSETS) {
                    int nx = x + offset[0];
                    int ny = y + offset[1];

                    if (contour) {
                        nx = (nx + M) % M;
                        ny = (ny + M) % M;
                    } else if (nx < 0 || nx >= M || ny < 0 || ny >= M) {
                        continue;
                    }

                    List<T> neighborCell = grid[nx][ny];

                    for (int i = 0; i < currentCellSize; i++) {
                        T p1 = currentCell.get(i);
                        for (T p2 : neighborCell) {
                            checkAndAddNeighbor(p1, p2, contour);
                        }
                    }
                }
            }
        }
    }

    private void checkAndAddNeighbor(T p1, T p2, boolean contour) {
        float distance = p1.euclideanDistance(p2, contour ? Optional.of(L) : Optional.empty());

        if (distance <= rc) {
            p1.getNeighbors().add(p2);
            p2.getNeighbors().add(p1);
        }
    }
}