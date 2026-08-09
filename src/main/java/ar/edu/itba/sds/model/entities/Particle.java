package ar.edu.itba.sds.model.entities;

import ar.edu.itba.sds.model.Entity2D;

import java.util.Objects;

public class Particle implements Entity2D<Particle> {
    private final float x;
    private final float y;


    public Particle(float x, float y) {
        if (x < 0 || y < 0) throw new IllegalArgumentException();
        this.x = x;
        this.y = y;
    }

    @Override
    public float euclideanDistance(Particle other) {
        return (float) Math.sqrt((this.x-other.x)*(this.x-other.x) +
                        (this.y-other.y)*(this.y-other.y));
    }

    @Override
    public boolean collidesWith(Particle other) {
        return this.equals(other);
    }

    @Override
    public boolean existsIn(float minX, float minY, float maxX, float maxY) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }


    @Override
    public float getMinX() {
        return x;
    }

    @Override
    public float getMaxX() {
        return x;
    }

    @Override
    public float getMinY() {
        return y;
    }

    @Override
    public float getMaxY() {
        return y;
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
