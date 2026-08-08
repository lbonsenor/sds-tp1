package ar.edu.itba.sds.service;

import ar.edu.itba.sds.model.Entity2D;

import java.util.*;

public class CellIndexService<P extends Entity2D<P>> {
    public final int m;         // Size of the mxm matrix
    public final int l;         // Longitude
    public final int rc;        // Max neighbor distance

    public final float cellSize;

    public final Map<P, List<Long>> cellsWithParticle = new HashMap<>();
    public final Map<Long, List<P>> particlesInCells = new HashMap<>();
    public final Map<P, List<P>> neighbors = new HashMap<>();

    public CellIndexService(int m, int l, int rc, Collection<P> particles) {
        this.m = m;
        this.l = l;
        this.rc = rc;

        this.cellSize = (float) l / m;

        if (m < 1 || cellSize < rc || l <= 0) throw new IllegalArgumentException();

        for (P particle : particles) {
            processParticle(particle);
        }
    }

    public void processParticle(P particle) {
        List<Long> occupiedCells = new ArrayList<>();

        // 1. Convert bounding box bounds directly to cell indices [0, m-1]
        int minCol = (int) (particle.getMinX() / cellSize);
        int maxCol = Math.min(m - 1, (int) (particle.getMaxX() / cellSize)); // Handles boundary x == L

        int minRow = (int) (particle.getMinY() / cellSize);
        int maxRow = Math.min(m - 1, (int) (particle.getMaxY() / cellSize)); // Handles boundary y == L

        // 2. Iterate only over the candidate cell range
        for (int row = minRow; row <= maxRow; row++) {
            float cellMinY = row * cellSize;
            float cellMaxY = (row + 1) * cellSize;

            for (int col = minCol; col <= maxCol; col++) {
                float cellMinX = col * cellSize;
                float cellMaxX = (col + 1) * cellSize;

                if (particle.existsIn(cellMinX, cellMinY, cellMaxX, cellMaxY)) {
                    long cellId = (long) row * m + col + 1;
                    occupiedCells.add(cellId);

                    particlesInCells.computeIfAbsent(cellId, _ -> new ArrayList<>()).add(particle);
                }
            }
        }

        cellsWithParticle.put(particle, occupiedCells);
    }

    public Set<Long> getCellsToCheck(P particle) {
        Set<Long> cellsToCheck = new HashSet<>();
        List<Long> occupiedCells = cellsWithParticle.getOrDefault(particle, Collections.emptyList());

        for (Long cell : occupiedCells) {
            cellsToCheck.add(cell);
            top(cell).ifPresent(c -> cellsToCheck.add((long) c));
            topRight(cell).ifPresent(c -> cellsToCheck.add((long) c));
            right(cell).ifPresent(c -> cellsToCheck.add((long) c));
            bottomRight(cell).ifPresent(c -> cellsToCheck.add((long) c));
        }

        return cellsToCheck;
    }

    private Optional<Integer> top(long cell) {
        long r = (cell - 1) / m;

        return (r < m - 1)
                ? Optional.of((int) (cell + m))
                : Optional.empty();
    }

    private Optional<Integer> right(long cell) {
        long col = (cell - 1) % m;

        return (col < m - 1)
                ? Optional.of((int) (cell + 1))
                : Optional.empty();
    }

    private Optional<Integer> topRight(long cell) {
        long r = (cell - 1) / m;
        long col = (cell - 1) % m;

        return (r < m - 1 && col < m - 1)
                ? Optional.of((int) (cell + m + 1))
                : Optional.empty();
    }

    private Optional<Integer> bottomRight(long cell) {
        long r = (cell - 1) / m;
        long col = (cell - 1) % m;

        return (r > 0 && col < m - 1)
                ? Optional.of((int) (cell - m + 1))
                : Optional.empty();
    }
}