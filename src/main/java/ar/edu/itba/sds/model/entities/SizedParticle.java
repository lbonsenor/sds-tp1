package ar.edu.itba.sds.model.entities;

import ar.edu.itba.sds.model.Entity2D;

import java.util.*;

public class SizedParticle implements Entity2D<SizedParticle> {
    private final float x;
    private final float y;
    private final float r;
    private final float v;
    private final float angle;

    private final Set<SizedParticle> neighbors = new HashSet<>();

    public SizedParticle(float x, float y, float r, float v, float angle) {
        if (x-r < 0 || y-r < 0) throw new IllegalArgumentException();
        this.x = x;
        this.y = y;
        this.r = r;
        this.v = v;
        this.angle = angle;
    }

    @Override
    public float euclideanDistance(SizedParticle other, Optional<Float> contour) {
        float dx = Math.abs(this.x - other.x);
        float dy = Math.abs(this.y - other.y);

        if (contour.isPresent()) {
            float L = contour.get();
            dx = Math.min(dx, L - dx);
            dy = Math.min(dy, L - dy);
        }

        return (float) Math.sqrt(dx * dx + dy * dy) - (this.r + other.r);
    }

    @Override
    public boolean collidesWith(SizedParticle other) {
        float dx = this.x - other.x;
        float dy = this.y - other.y;
        float centerDistance = (float) Math.sqrt(dx * dx + dy * dy);

        // Collides if distance between centers is less than or equal to sum of radii
        return (centerDistance - (this.r + other.r)) <= 0;
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

    public SizedParticle getNewPositionStandard(float deltaTime, float eta, Random random) {

        float dx = v * (float) Math.cos(angle);
        float dy = v * (float) Math.sin(angle);

        float radius_sin_accum = (float) Math.sin(angle);
        float radius_cos_accum = (float) Math.cos(angle);
        int count = 1;

        for (SizedParticle p : neighbors){
            // if collidesWith este paso podria ser innecesario. Evaluar eso.
                radius_cos_accum = (float) (radius_cos_accum + Math.cos(p.angle));
                radius_sin_accum = (float) (radius_sin_accum + Math.sin(p.angle));
                count++;
        }
        float deltaTheta = (random.nextFloat() - 0.5f) * eta;
        float newAngle = (float)(Math.atan2(radius_sin_accum/count,radius_cos_accum/count))+deltaTheta;

        return new SizedParticle(x + dx * deltaTime,y + dy * deltaTime,r,v, newAngle);
    }

    public SizedParticle getNewPositionVotante(float deltaTime, float eta, Random random) {
        float dx = v * (float) Math.cos(angle);
        float dy = v * (float) Math.sin(angle);


        float deltaTheta = (random.nextFloat() - 0.5f) * eta;
        float newAngle = this.angle;
        if (!neighbors.isEmpty()) {
            newAngle = new ArrayList<>(neighbors)
                    .get(random.nextInt(neighbors.size())).getAngle() + deltaTheta;
        }
        return new SizedParticle(x + dx * deltaTime,y + dy * deltaTime,r,v, newAngle);
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

    public float getAngle() {
        return angle;
    }

    public float getV() {
        return v;
    }

    @Override
    public Set<SizedParticle> getNeighbors() {
        return neighbors;
    }

    @Override
    public String toString() {
        return "SizedParticle{" +
                "x=" + x +
                ", y=" + y +
                ", r=" + r +
                ", v=" + v +
                ", angle =" + angle +
                '}';
    }
}
