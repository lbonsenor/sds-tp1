package ar.edu.itba.sds.service;

import ar.edu.itba.sds.model.Entity2D;
import ar.edu.itba.sds.model.entities.Cell;
import ar.edu.itba.sds.model.entities.Particle;

import java.util.*;

public class CellIndexService2 <P extends Entity2D<P>> {
    public final int m;         // Size of the mxm matrix
    public final int l;         // Longitude
    public final float rc;        // Max neighbor distance

    public final float cellSize;

//    public final Map<P, List<Long>> cellsWithParticle = new HashMap<>();
    //public final Map<Integer,Map<Cell, List<P>>> particlesInCells = new HashMap<>();

    public final Map<Integer, List<Cell<P>>> map = new HashMap<>();


//    public final Map<P, List<P>> neighbors = new HashMap<>();

    public CellIndexService2(int m, int l, float rc, Collection<P> particles) {
        this.m = m;
        this.l = l;
        this.rc = rc;

        this.cellSize = (float) l / m;

        if (m < 1 || cellSize < rc || l <= 0) throw new IllegalArgumentException();
            //    L/M > rc +2*rmax

        for (int i = 0; i < m; i++ ) {
            map.put(i, new ArrayList<>());
            for (int j = 0; j < m; j++ ) {
                map.get(i).add(new Cell<>(
                        j * cellSize,
                        i * cellSize,
                        (j + 1) * cellSize,
                        (i + 1) * cellSize));
            }
        }

        for (P particle : particles) {
            processParticle(particle);
        }
    }

    public void calculateNeighbors(P particle) {

        Set<Cell<P>> cellsToVisit = new HashSet<>();
        for (Cell<P> cell : particle.getCells()){

            int cellColl = (int) (cell.getMinX() / m);
            int cellRow = (int) (cell.getMinY() / m);

            cellsToVisit.add(cell);
            top(cellColl, cellRow).ifPresent(cellsToVisit::add);
            topRight(cellColl, cellRow).ifPresent(cellsToVisit::add);
            bottomRight(cellColl, cellRow).ifPresent(cellsToVisit::add);
            right(cellColl, cellRow).ifPresent(cellsToVisit::add);
        }
        for (Cell<P> cell : cellsToVisit) {
            for (P others : cell.getParticles()) {
                if(others.equals(particle)) continue;
                if(particle.euclideanDistance(others) <= rc) {
                    particle.getNeighbors().add(others);
                    others.getNeighbors().add(particle);
                }
            }
        }
    }


    //Used when grid initialization
    private void processParticle(P particle) {

        // 1. Convert bounding box bounds directly to cell indices [0, m-1]
        int minCol = (int) (particle.getMinX() / cellSize);
        int maxCol = Math.min(m - 1, (int) (particle.getMaxX() / cellSize)); // Handles boundary x == L

        int minRow = (int) (particle.getMinY() / cellSize);
        int maxRow = Math.min(m - 1, (int) (particle.getMaxY() / cellSize)); // Handles boundary y == L

        for (int row = minRow; row <= maxRow; row++) {
            for (int col = minCol; col <= maxCol; col++) {
                Cell<P> cell = map.get(row).get(col);
                if (particle.existsIn(cell.getMinX(), cell.getMinY(), cell.getMaxX(), cell.getMaxY())) {
                    cell.getParticles().add(particle);
                    particle.getCells().add(cell);
                }
            }
        }
    }

    /*

    *  *  *  *
    *  *  *  *
    *  *  *  *
    *  *  *  *

     */

    private Optional<Cell<P>> top(int col, int row) {
        if(row <= 0) return Optional.empty();
        Cell<P> cell = map.get(row-1).get(col);
        return Optional.of(cell);
    }

    private Optional<Cell<P>> topRight(int col, int row) {
        if(row <= 0) return Optional.empty();
        if(col >= m-1) return Optional.empty();
        Cell<P> cell = map.get(row-1).get(col+1);
        return Optional.of(cell);
    }

    private Optional<Cell<P>> bottomRight(int col, int row) {
        if(row >= m - 1) return Optional.empty();
        if(col >= m - 1) return Optional.empty();
        Cell<P> cell = map.get(row+1).get(col+1);
        return Optional.of(cell);
    }

    private Optional<Cell<P>> left(int col, int row) {
        if(col <= 0) return Optional.empty();
        Cell<P> cell = map.get(row).get(col-1);
        return Optional.of(cell);
    }

    private Optional<Cell<P>> right(int col, int row) {
        if(col >= m - 1) return Optional.empty();
        Cell<P> cell = map.get(row+1).get(col);
        return Optional.of(cell);
    }

    public Map<Integer, List<Cell<P>>> getMap() {
        return map;
    }
}
