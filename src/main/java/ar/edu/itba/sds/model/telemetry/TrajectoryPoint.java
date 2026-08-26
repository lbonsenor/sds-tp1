package ar.edu.itba.sds.model.telemetry;

import com.opencsv.bean.CsvBindByName;

public class TrajectoryPoint {

    @CsvBindByName(column = "run_id")
    private String runId;

    @CsvBindByName(column = "t")
    private double t;

    @CsvBindByName(column = "particle_id")
    private int particleId;

    @CsvBindByName(column = "x")
    private double x;

    @CsvBindByName(column = "y")
    private double y;

    @CsvBindByName(column = "vx")
    private double vx;

    @CsvBindByName(column = "vy")
    private double vy;

    @CsvBindByName(column = "theta")
    private double theta; // velocity angle, used to color the vector in the animation

    public TrajectoryPoint() {
    }

    public TrajectoryPoint(String runId, double t, int particleId, double x, double y,
                           double vx, double vy, double theta) {
        this.runId = runId;
        this.t = t;
        this.particleId = particleId;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.theta = theta;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public double getT() {
        return t;
    }

    public void setT(double t) {
        this.t = t;
    }

    public int getParticleId() {
        return particleId;
    }

    public void setParticleId(int particleId) {
        this.particleId = particleId;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getVx() {
        return vx;
    }

    public void setVx(double vx) {
        this.vx = vx;
    }

    public double getVy() {
        return vy;
    }

    public void setVy(double vy) {
        this.vy = vy;
    }

    public double getTheta() {
        return theta;
    }

    public void setTheta(double theta) {
        this.theta = theta;
    }

    @Override
    public String toString() {
        return "TrajectoryPoint{" +
                "runId='" + runId + '\'' +
                ", t=" + t +
                ", particleId=" + particleId +
                ", x=" + x +
                ", y=" + y +
                ", vx=" + vx +
                ", vy=" + vy +
                ", theta=" + theta +
                '}';
    }
}
