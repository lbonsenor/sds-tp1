package ar.edu.itba.sds.model.flocking;

import ar.edu.itba.sds.model.entities.Entity2D;

import java.util.Random;

@Deprecated
@FunctionalInterface
public interface FlockingStrategy {
    float computeNewAngle(Entity2D particle, float eta, Random random);
}
