package ar.edu.itba.sds.model;

import ar.edu.itba.sds.model.entities.Cell;

import java.util.Set;

public interface Entity2D<T extends Entity2D<T>> {
    // Since sqrt is costly, it is much more efficient to just operate with square values
    float euclideanDistance(T other);
    boolean collidesWith(T other);
    boolean existsIn(float minX, float minY, float maxX, float maxY);

    float getMinX();
    float getMaxX();
    float getMinY();
    float getMaxY();

    Set<T> getNeighbors();
    Set<Cell<T>> getCells();
}
