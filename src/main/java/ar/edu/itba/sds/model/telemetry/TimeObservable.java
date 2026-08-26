package ar.edu.itba.sds.model.telemetry;

import com.opencsv.bean.CsvBindByName;

public class TimeObservable {

    @CsvBindByName(column = "run_id")
    private String runId;

    @CsvBindByName(column = "model")
    private String model;

    @CsvBindByName(column = "density")
    private double density;

    @CsvBindByName(column = "eta")
    private double eta;

    @CsvBindByName(column = "time")
    private double t;

    @CsvBindByName(column = "va")
    private double va; // instantaneous polarization

    @CsvBindByName(column = "cluster_ratio")
    private double s; // fraction of particles in the largest cluster

    @CsvBindByName(column = "max_cluster_size")
    private int maxClusterSize; // largest cluster size, in number of particles

    public TimeObservable() {
    }

    public TimeObservable(String runId, String model, double density, double eta, double t,
                          double va, double s, int maxClusterSize) {
        this.runId = runId;
        this.model = model;
        this.density = density;
        this.eta = eta;
        this.t = t;
        this.va = va;
        this.s = s;
        this.maxClusterSize = maxClusterSize;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getDensity() {
        return density;
    }

    public void setDensity(double density) {
        this.density = density;
    }

    public double getEta() {
        return eta;
    }

    public void setEta(double eta) {
        this.eta = eta;
    }

    public double getT() {
        return t;
    }

    public void setT(double t) {
        this.t = t;
    }

    public double getVa() {
        return va;
    }

    public void setVa(double va) {
        this.va = va;
    }

    public double getS() {
        return s;
    }

    public void setS(double s) {
        this.s = s;
    }

    public int getMaxClusterSize() {
        return maxClusterSize;
    }

    public void setMaxClusterSize(int maxClusterSize) {
        this.maxClusterSize = maxClusterSize;
    }

    @Override
    public String toString() {
        return "TimeObservable{" +
                "runId='" + runId + '\'' +
                ", model='" + model + '\'' +
                ", density=" + density +
                ", eta=" + eta +
                ", t=" + t +
                ", va=" + va +
                ", s=" + s +
                ", maxClusterSize=" + maxClusterSize +
                '}';
    }
}