package ar.edu.itba.sds.model;

import java.util.Optional;
import java.util.Set;

public interface Entity2D<T extends Entity2D<T>> {
    float euclideanDistance(T other, Optional<Float> contour);
    boolean collidesWith(T other);
    boolean existsIn(float minX, float minY, float maxX, float maxY);

    float getMinX();
    float getMaxX();
    float getMinY();
    float getMaxY();

    Set<T> getNeighbors();
}
