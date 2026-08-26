package ar.edu.itba.sds.model.telemetry;

import com.opencsv.bean.CsvBindByName;

public class ClusterDetail {

    @CsvBindByName(column = "run_id")
    private String runId;

    @CsvBindByName(column = "t")
    private double t;

    @CsvBindByName(column = "cluster_id")
    private int clusterId;

    @CsvBindByName(column = "size")
    private int size; // number of particles in this cluster

    public ClusterDetail() {
    }

    public ClusterDetail(String runId, double t, int clusterId, int size) {
        this.runId = runId;
        this.t = t;
        this.clusterId = clusterId;
        this.size = size;
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

    public int getClusterId() {
        return clusterId;
    }

    public void setClusterId(int clusterId) {
        this.clusterId = clusterId;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "ClusterDetail{" +
                "runId='" + runId + '\'' +
                ", t=" + t +
                ", clusterId=" + clusterId +
                ", size=" + size +
                '}';
    }
}