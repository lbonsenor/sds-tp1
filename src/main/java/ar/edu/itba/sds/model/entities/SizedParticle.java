package ar.edu.itba.sds.model.entities;

import ar.edu.itba.sds.model.Entity2D;

import java.util.HashSet;
import java.util.Set;

public class SizedParticle implements Entity2D<SizedParticle> {
    private final float x;
    private final float y;
    private final float r;

    private final Set<SizedParticle> neighbors = new HashSet<>();
    private final Set<Cell<SizedParticle>> cells = new HashSet<>();

    public SizedParticle(float x, float y, float r) {
        if (x-r < 0 || y-r < 0) throw new IllegalArgumentException();
        this.x = x;
        this.y = y;
        this.r = r;
    }

    @Override
    public float euclideanDistance(SizedParticle other, boolean contour, int l) {
        float directX = (this.x - other.x)*(this.x - other.x);
        float directY = (this.y - other.y)*(this.y - other.y);

        if (!contour) return (float) Math.sqrt(directX+directY);

        float contourX = ((this.x-l) - other.x)*((this.x-l) - other.x);
        float contourY = ((this.y-l) - other.y)*((this.y-l) - other.y);

        return (float) Math.sqrt(Math.min(directX, contourX) + Math.min(directY, contourY));



//        float directDistance =  (float) ( Math.sqrt((this.x-other.x)*(this.x-other.x) +
//                        (this.y-other.y)*(this.y-other.y))) - this.r - other.r;
//
//        if(contour) {
//            float contourDistance = (float) (Math.sqrt((this.x- (other.x - l))*(this.x- (other.x - l)) +
//                    (this.y- (other.y - l))*(this.y-(other.y - l)))) - this.r - other.r;
//
//            return Math.min(directDistance, contourDistance);
//        }
//        return directDistance;

    }

    @Override
    public boolean collidesWith(SizedParticle other) {
        return euclideanDistance(other, false, 0) <= 0;
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

    @Override
    public Set<SizedParticle> getNeighbors() {
        return neighbors;
    }

    @Override
    public Set<Cell<SizedParticle>> getCells() {
        return cells;
    }

    @Override
    public String toString() {
        return "SizedParticle{" +
                "x=" + x +
                ", y=" + y +
                ", r=" + r +
                '}';
    }
}
