package ar.edu.itba.sds.utils;

import ar.edu.itba.sds.model.entities.Entity2D;
import ar.edu.itba.sds.model.entities.SizedParticle;
import ar.edu.itba.sds.model.telemetry.ParticlePoint;

import java.util.*;
import java.util.stream.Collectors;

public class ParticleStateExporter {

    public List<ParticlePoint> extractPoints(String runId, float t, Collection<? extends Entity2D> particles) {
        List<Entity2D> particleList = new ArrayList<>(particles);
        Map<Entity2D, Integer> particleIndices = buildIndexMap(particleList);
        List<ParticlePoint> points = new ArrayList<>(particleList.size());

        for (int i = 0; i < particleList.size(); i++) {
            Entity2D p = particleList.get(i);

            double x = p.getX();
            double y = p.getY();
            double radius = p.getR();
            double theta = p.getAngle(); // Stored natively in radians

            double v = (p instanceof SizedParticle sp) ? sp.getV() : 0.0;
            double vx = v * Math.cos(theta); // Computes velocity using radians
            double vy = v * Math.sin(theta);
            String neighborsStr = formatNeighbors(p.getNeighbors(), particleIndices);

            ParticlePoint point = new ParticlePoint();
            point.setRunId(runId);
            point.setT(t);
            point.setParticleId(i);
            point.setX(x);
            point.setY(y);
            point.setVx(vx);
            point.setVy(vy);
            point.setTheta(theta); // Stored in telemetry as radians
            point.setRadius(radius);
            point.setNeighbors(neighborsStr);

            points.add(point);
        }
        return points;
    }

    private Map<Entity2D, Integer> buildIndexMap(List<Entity2D> particleList) {
        Map<Entity2D, Integer> indices = new IdentityHashMap<>(particleList.size());
        for (int i = 0; i < particleList.size(); i++) {
            indices.put(particleList.get(i), i);
        }
        return indices;
    }

    private String formatNeighbors(Set<Entity2D> neighbors, Map<Entity2D, Integer> particleIndices) {
        String ids = neighbors.stream()
                .map(particleIndices::get)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(" "));
        return ids.isEmpty() ? "none" : "[" + ids + "]";
    }
}