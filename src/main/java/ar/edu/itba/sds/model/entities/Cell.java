package ar.edu.itba.sds.model.entities;

import ar.edu.itba.sds.model.Entity2D;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class Cell<T extends Entity2D<T>> {
    private final float minX;
    private final float minY;
    private final float maxX;
    private final float maxY;

    private final Set<T> particles;

    public Cell(float minX, float minY, float maxX, float maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        particles = new HashSet<>();
    }


    public float getMinX() {
        return minX;
    }

    public float getMinY() {
        return minY;
    }

    public float getMaxX() {
        return maxX;
    }

    public float getMaxY() {
        return maxY;
    }

    public Set<T> getParticles() {
        return particles;
    }








    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Cell<?> cell)) return false;
        return Float.compare(getMinX(), cell.getMinX()) == 0 && Float.compare(getMinY(), cell.getMinY()) == 0 && Float.compare(getMaxX(), cell.getMaxX()) == 0 && Float.compare(getMaxY(), cell.getMaxY()) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMinX(), getMinY(), getMaxX(), getMaxY());
    }
}
