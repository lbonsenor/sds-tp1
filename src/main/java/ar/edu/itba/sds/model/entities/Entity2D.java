package ar.edu.itba.sds.model.entities;

import java.util.Set;

public interface Entity2D {
    // Distance calculation
    float euclideanDistance(Entity2D other);
    float euclideanDistance(Entity2D other, float contourLength);

    boolean collidesWith(Entity2D other);
    boolean existsIn(float minX, float minY, float maxX, float maxY);

    float getMinX();
    float getMaxX();
    float getMinY();
    float getMaxY();

    float getR();
    float getX();
    float getY();
    float getAngle();

    Set<Entity2D> getNeighbors();

    Entity2D move(float deltaTime, float newAngle);
    Entity2D move(float deltaTime, float newAngle, float contourLength);
}