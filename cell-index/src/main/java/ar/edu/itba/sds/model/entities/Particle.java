package ar.edu.itba.sds.model.entities;

import ar.edu.itba.sds.model.Entity2D;

import java.util.Objects;

public class Particle implements Entity2D<Particle> {
    private float x;
    private float y;

    @Override
    public float euclidean_distance_squared(Particle other) {
        return (
                (this.x-other.x)*(this.x-other.x) +
                (this.y-other.y)*(this.y-other.y)
        );
    }

    @Override
    public boolean collides_with(Particle other) {
        return this.equals(other);
    }

    @Override
    public boolean is_in(float x, float y) {
        return Float.compare(x, this.x) == 0 && Float.compare(y, this.y) == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Particle particle = (Particle) o;
        return Float.compare(x, particle.x) == 0 && Float.compare(y, particle.y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
