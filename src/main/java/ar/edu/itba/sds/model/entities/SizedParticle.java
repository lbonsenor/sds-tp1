package ar.edu.itba.sds.model.entities;

import java.util.*;

public class SizedParticle implements Entity2D {
    private static int nextId = 1;

    private final int id;
    private final float x;
    private final float y;
    private final float r;
    private final float v;
    private final float angle;

    private final Set<Entity2D> neighbors = new HashSet<>();

    // Constructor with auto-assigned ID
    public SizedParticle(float x, float y, float r, float v, float angle) {
        this(nextId++, x, y, r, v, angle);
    }

    // Explicit ID constructor (used when moving/updating particles)
    public SizedParticle(int id, float x, float y, float r, float v, float angle) {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("Particle coordinates must be non-negative.");
        }
        this.id = id;
        this.x = x;
        this.y = y;
        this.r = r;
        this.v = v;
        this.angle = angle;
    }

    // --- Distance Overloads ---

    @Override
    public float euclideanDistance(Entity2D other) {
        float dx = Math.abs(this.x - other.getX());
        float dy = Math.abs(this.y - other.getY());
        return (float) Math.hypot(dx, dy) - (this.r + other.getR());
    }

    @Override
    public float euclideanDistance(Entity2D other, float length) {
        float dx = Math.abs(this.x - other.getX());
        float dy = Math.abs(this.y - other.getY());

        // Periodic boundary condition distance
        dx = Math.min(dx, length - dx);
        dy = Math.min(dy, length - dy);

        return (float) Math.hypot(dx, dy) - (this.r + other.getR());
    }

    @Override
    public SizedParticle move(float deltaTime, float newAngle) {
        float nextX = x + v * (float) Math.cos(angle) * deltaTime;
        float nextY = y + v * (float) Math.sin(angle) * deltaTime;
        return new SizedParticle(this.id, nextX, nextY, r, v, newAngle);
    }

    @Override
    public SizedParticle move(float deltaTime, float newAngle, float contourLength) {
        float rawX = x + v * (float) Math.cos(angle) * deltaTime;
        float rawY = y + v * (float) Math.sin(angle) * deltaTime;

        // Toroidal Wrapping
        float nextX = (rawX % contourLength + contourLength) % contourLength;
        float nextY = (rawY % contourLength + contourLength) % contourLength;

        return new SizedParticle(this.id, nextX, nextY, r, v, newAngle);
    }

    public static void resetIdCounter() {
        nextId = 1;
    }

    // --- Identity Overrides ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SizedParticle that = (SizedParticle) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    // --- Getters & Overrides ---

    public int getId() { return id; }

    @Override
    public boolean collidesWith(Entity2D other) {
        return euclideanDistance(other) <= 0;
    }

    @Override
    public boolean existsIn(float minX, float minY, float maxX, float maxY) {
        float closestX = Math.clamp(this.x, minX, maxX);
        float closestY = Math.clamp(this.y, minY, maxY);
        float dx = this.x - closestX;
        float dy = this.y - closestY;
        return (dx * dx + dy * dy) <= (this.r * this.r);
    }

    @Override public float getMinX() { return x - r; }
    @Override public float getMaxX() { return x + r; }
    @Override public float getMinY() { return y - r; }
    @Override public float getMaxY() { return y + r; }
    @Override public float getR() { return r; }
    @Override public float getX() { return x; }
    @Override public float getY() { return y; }
    @Override public float getAngle() { return angle; }
    public float getV() { return v; }
    @Override public Set<Entity2D> getNeighbors() { return neighbors; }

    @Override
    public String toString() {
        return "SizedParticle{" +
                "id=" + id +
                ", x=" + x +
                ", y=" + y +
                ", r=" + r +
                ", v=" + v +
                ", angle=" + angle +
                '}';
    }
}