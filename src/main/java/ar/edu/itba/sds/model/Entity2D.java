package ar.edu.itba.sds.model;

public interface Entity2D<T> {
    // Since sqrt is costly, it is much more efficient to just operate with square values
    float euclideanDistanceSquared(T other);
    boolean collidesWith(T other);
    boolean existsIn(float minX, float minY, float maxX, float maxY);

    float getMinX();
    float getMaxX();
    float getMinY();
    float getMaxY();
}
