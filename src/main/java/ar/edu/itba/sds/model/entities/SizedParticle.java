package ar.edu.itba.sds.model.entities;

import ar.edu.itba.sds.model.Entity2D;

public class SizedParticle implements Entity2D<SizedParticle> {
    private final float x;
    private final float y;
    private final float r;

    public SizedParticle(float x, float y, float r) {
        if (x-r < 0 || y-r < 0) throw new IllegalArgumentException();
        this.x = x;
        this.y = y;
        this.r = r;
    }

    @Override
    public float euclideanDistance(SizedParticle other) {
        return (float) ( Math.sqrt((this.x-other.x)*(this.x-other.x) +
                        (this.y-other.y)*(this.y-other.y))) - this.r - other.r;
    }

    @Override
    public boolean collidesWith(SizedParticle other) {
        return euclideanDistance(other) <= 0;
    }

    @Override
    public boolean existsIn(float minX, float minY, float maxX, float maxY) {
        // Find the point on the cell bounding box closest to the circle's center
        float closestX = Math.clamp(this.x, minX, maxX);
        float closestY = Math.clamp(this.y, minY, maxY);

        // Calculate the squared distance between the circle's center and this closest point
        float dx = this.x - closestX;
        float dy = this.y - closestY;

        return (dx * dx + dy * dy) <= (this.r * this.r);
    }

    @Override
    public float getMinX() {
        return x-r;
    }

    @Override
    public float getMaxX() {
        return x+r;
    }

    @Override
    public float getMinY() {
        return y-r;
    }

    @Override
    public float getMaxY() {
        return y+r;
    }
}
