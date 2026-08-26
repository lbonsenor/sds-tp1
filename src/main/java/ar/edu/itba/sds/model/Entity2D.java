package ar.edu.itba.sds.model;

import java.util.Optional;
import java.util.Random;
import java.util.Set;

public interface Entity2D {
    float euclideanDistance(Entity2D other, Optional<Float> contour);
    boolean collidesWith(Entity2D other);
    boolean existsIn(float minX, float minY, float maxX, float maxY);

    float getMinX();
    float getMaxX();
    float getMinY();
    float getMaxY();

    float getR();
    float getX();
    float getY();

    Set<Entity2D> getNeighbors();

    float getAngle();

    Entity2D getNewPositionVotante(float deltaTime, float eta, Random random);

    Entity2D getNewPositionStandard(float deltaTime, float eta, Random random);
}
