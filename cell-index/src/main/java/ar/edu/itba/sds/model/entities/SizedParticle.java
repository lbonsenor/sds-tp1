package ar.edu.itba.sds.model.entities;

import ar.edu.itba.sds.model.Entity2D;

public class SizedParticle implements Entity2D<SizedParticle> {
    private float x;
    private float y;
    private float r;

    @Override
    public float euclidean_distance_squared(SizedParticle other) {
        return 0;
    }

    @Override
    public boolean collides_with(SizedParticle other) {
        return false;
    }

    @Override
    public boolean is_in(float x, float y) {
        return false;
    }
}
