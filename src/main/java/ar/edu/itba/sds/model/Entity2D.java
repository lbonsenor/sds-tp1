package ar.edu.itba.sds.model;

import ar.edu.itba.sds.model.entities.SizedParticle;

import java.util.Optional;
import java.util.Random;
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
    T getNewPositionVotante(float deltaTime, float eta, Random random);
    T getNewPositionStandard(float deltaTime, float eta, Random random);
}
