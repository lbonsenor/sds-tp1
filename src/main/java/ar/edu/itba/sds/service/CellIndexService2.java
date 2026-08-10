package ar.edu.itba.sds.service;

import ar.edu.itba.sds.model.Entity2D;
import ar.edu.itba.sds.model.entities.Cell;

import java.util.*;

public class CellIndexService2<P extends Entity2D<P>> {
    public final int m;           // Size of the mxm matrix
    public final int l;           // Longitude
    public final float rc;        // Max neighbor distance
    public final float cellSize;

    private final Cell<P>[][] grid;

    public CellIndexService2(int m, int l, float rc, Collection<P> particles) {
        if (m < 1 || l <= 0 || ((float) l / m) < rc) {
            throw new IllegalArgumentException("Invalid grid parameters. Cell size must be >= rc.");
        }

        this.m = m;
        this.l = l;
        this.rc = rc;
        this.cellSize = (float) l / m;

        //noinspection unchecked
        this.grid = (Cell<P>[][]) new Cell[m][m];
        for (int row = 0; row < m; row++) {
            for (int col = 0; col < m; col++) {
                grid[row][col] = new Cell<>(
                        col * cellSize,
                        row * cellSize,
                        (col + 1) * cellSize,
                        (row + 1) * cellSize
                );
            }
        }

        for (P particle : particles) {
            processParticle(particle);
        }
    }

    public void calculateNeighbors(P particle, boolean contour) {
        Set<Cell<P>> cellsToVisit = new HashSet<>();

        for (Cell<P> cell : particle.getCells()) {
            int col = (int) Math.floor(cell.getMinX() / cellSize);
            int row = (int) Math.floor(cell.getMinY() / cellSize);

            cellsToVisit.add(cell);
            getNeighborCell(row - 1, col, contour).ifPresent(cellsToVisit::add);     // Top
            getNeighborCell(row - 1, col + 1, contour).ifPresent(cellsToVisit::add); // Top-Right
            getNeighborCell(row + 1, col + 1, contour).ifPresent(cellsToVisit::add); // Bottom-Right
            getNeighborCell(row, col + 1, contour).ifPresent(cellsToVisit::add);     // Right
        }

        for (Cell<P> cell : cellsToVisit) {
            for (P other : cell.getParticles()) {
                if (other.equals(particle)) continue;

                if (particle.euclideanDistance(other, contour, l) <= rc) {
                    particle.getNeighbors().add(other);
                    other.getNeighbors().add(particle);
                }
            }
        }
    }

    private void processParticle(P particle) {
        int minCol = Math.max(0, (int) (particle.getMinX() / cellSize));
        int maxCol = Math.min(m - 1, (int) (particle.getMaxX() / cellSize));

        int minRow = Math.max(0, (int) (particle.getMinY() / cellSize));
        int maxRow = Math.min(m - 1, (int) (particle.getMaxY() / cellSize));

        for (int row = minRow; row <= maxRow; row++) {
            for (int col = minCol; col <= maxCol; col++) {
                Cell<P> cell = grid[row][col];
                if (particle.existsIn(cell.getMinX(), cell.getMinY(), cell.getMaxX(), cell.getMaxY())) {
                    cell.getParticles().add(particle);
                    particle.getCells().add(cell);
                }
            }
        }
    }

    /**
     * Helper to retrieve neighbor cells handling periodic boundary conditions (contour)
     * or standard strict grid bounds.
     */
    private Optional<Cell<P>> getNeighborCell(int row, int col, boolean contour) {
        if (contour) {
            // Mathematical modulo to wrap negative coordinates properly
            row = (row % m + m) % m;
            col = (col % m + m) % m;
            return Optional.of(grid[row][col]);
        }

        // Standard boundary check for non-contour simulations
        if (row >= 0 && row < m && col >= 0 && col < m) {
            return Optional.of(grid[row][col]);
        }

        return Optional.empty();
    }

    public Cell<P>[][] getGrid() {
        return grid;
    }
}