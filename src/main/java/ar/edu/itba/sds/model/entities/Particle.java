//package ar.edu.itba.sds.model.entities;
//
//import ar.edu.itba.sds.model.Entity2D;
//
//import java.util.HashSet;
//import java.util.Objects;
//import java.util.Optional;
//import java.util.Set;
//
//public class Particle implements Entity2D {
//    private final float x;
//    private final float y;
//
//    private final Set<Entity2D> neighbors = new HashSet<>();
//
//
//    public Particle(float x, float y) {
//        if (x < 0 || y < 0) throw new IllegalArgumentException();
//        this.x = x;
//        this.y = y;
//    }
//
//    @Override
//    public float euclideanDistance(Entity2D other, Optional<Float> contour) {
//        float directX = (this.x - other.x)*(this.x - other.x);
//        float directY = (this.y - other.y)*(this.y - other.y);
//
//        if (contour.isEmpty()) return (float) Math.sqrt(directX+directY);
//
//        float contourX = ((this.x-contour.get()) - other.x)*((this.x-contour.get()) - other.x);
//        float contourY = ((this.y-contour.get()) - other.y)*((this.y-contour.get()) - other.y);
//
//        return (float) Math.sqrt(Math.min(directX, contourX) + Math.min(directY, contourY));
//    }
//
//    @Override
//    public boolean collidesWith(Particle other) {
//        return this.equals(other);
//    }
//
//    @Override
//    public boolean existsIn(float minX, float minY, float maxX, float maxY) {
//        return x >= minX && x <= maxX && y >= minY && y <= maxY;
//    }
//
//
//    @Override
//    public float getMinX() {
//        return x;
//    }
//
//    @Override
//    public float getMaxX() {
//        return x;
//    }
//
//    @Override
//    public float getMinY() {
//        return y;
//    }
//
//    @Override
//    public float getMaxY() {
//        return y;
//    }
//
//    @Override
//    public Set<Entity2D> getNeighbors() {
//        return neighbors;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (o == null || getClass() != o.getClass()) return false;
//        Particle particle = (Particle) o;
//        return Float.compare(x, particle.x) == 0 && Float.compare(y, particle.y) == 0;
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(x, y);
//    }
//
//
//
//    @Override
//    public String toString() {
//        return "Particle{" +
//                "x=" + x +
//                ", y=" + y +
//                '}';
//    }
//}
