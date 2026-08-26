package ar.edu.itba.sds.model.telemetry;

import com.opencsv.bean.CsvBindByName;

public class ParticlePoint {

    @CsvBindByName(column = "run_id")
    private String runId;

    @CsvBindByName(column = "time")
    private double t;

    @CsvBindByName(column = "particle_id")
    private int particleId;

    @CsvBindByName(column = "x")
    private double x;

    @CsvBindByName(column = "y")
    private double y;

    @CsvBindByName(column = "radius")
    private double radius;

    @CsvBindByName(column = "neighbors")
    private String neighbors;

    public ParticlePoint() {
    }

    public ParticlePoint(String runId, double t, int particleId, double x, double y, double radius, String neighbors) {
        this.runId = runId;
        this.t = t;
        this.particleId = particleId;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.neighbors = neighbors;
    }

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public double getT() { return t; }
    public void setT(double t) { this.t = t; }

    public int getParticleId() { return particleId; }
    public void setParticleId(int particleId) { this.particleId = particleId; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public double getRadius() { return radius; }
    public void setRadius(double radius) { this.radius = radius; }

    public String getNeighbors() { return neighbors; }
    public void setNeighbors(String neighbors) { this.neighbors = neighbors; }
}