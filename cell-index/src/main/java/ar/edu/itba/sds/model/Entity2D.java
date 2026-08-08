package ar.edu.itba.sds.model;

public interface Entity2D<T> {
    // Since sqrt is costly, it is much more efficient to just operate with square values
    float euclidean_distance_squared(T other);
    boolean collides_with(T other);
    boolean is_in(float x, float y);
}
