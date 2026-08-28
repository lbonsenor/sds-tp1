package ar.edu.itba.sds.model.flocking;

import ar.edu.itba.sds.model.entities.Entity2D;

import java.util.Random;

public class VicsekStandardStrategy implements FlockingStrategy {
    @Override
    public float computeNewAngle(Entity2D particle, float eta, Random random) {
        float sinAccum = (float) Math.sin(particle.getAngle());
        float cosAccum = (float) Math.cos(particle.getAngle());

        for (Entity2D p : particle.getNeighbors()) {
            sinAccum += (float) Math.sin(p.getAngle());
            cosAccum += (float) Math.cos(p.getAngle());
        }

        float deltaTheta = (random.nextFloat() - 0.5f) * eta;
        return (float) Math.atan2(sinAccum, cosAccum) + deltaTheta;
    }
}
