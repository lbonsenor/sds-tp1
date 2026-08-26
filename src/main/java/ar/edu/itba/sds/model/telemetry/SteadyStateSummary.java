package ar.edu.itba.sds.model.telemetry;

import ar.edu.itba.sds.model.flocking.FlockingModel;
import ar.edu.itba.sds.model.flocking.FlockingModelConverter;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;

public class SteadyStateSummary {

    @CsvCustomBindByName(column = "model", converter = FlockingModelConverter.class)
    private FlockingModel model;

    @CsvBindByName(column = "density")
    private double density;

    @CsvBindByName(column = "eta")
    private double eta;

    @CsvBindByName(column = "steady_state_start_time")
    private double steadyStateStartTime;

    @CsvBindByName(column = "n_replicas")
    private int nReplicas;

    @CsvBindByName(column = "va_mean")
    private double vaMean;

    @CsvBindByName(column = "va_std")
    private double vaStd;

    @CsvBindByName(column = "s_mean")
    private double sMean;

    @CsvBindByName(column = "s_std")
    private double sStd;

    public SteadyStateSummary() {
    }

    public SteadyStateSummary(FlockingModel model, double density, double eta, double steadyStateStartTime,
                              int nReplicas, double vaMean, double vaStd, double sMean, double sStd) {
        this.model = model;
        this.density = density;
        this.eta = eta;
        this.steadyStateStartTime = steadyStateStartTime;
        this.nReplicas = nReplicas;
        this.vaMean = vaMean;
        this.vaStd = vaStd;
        this.sMean = sMean;
        this.sStd = sStd;
    }

    public FlockingModel getModel() {
        return model;
    }

    public void setModel(FlockingModel model) {
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

    public double getSteadyStateStartTime() {
        return steadyStateStartTime;
    }

    public void setSteadyStateStartTime(double steadyStateStartTime) {
        this.steadyStateStartTime = steadyStateStartTime;
    }

    public int getNReplicas() {
        return nReplicas;
    }

    public void setNReplicas(int nReplicas) {
        this.nReplicas = nReplicas;
    }

    public double getVaMean() {
        return vaMean;
    }

    public void setVaMean(double vaMean) {
        this.vaMean = vaMean;
    }

    public double getVaStd() {
        return vaStd;
    }

    public void setVaStd(double vaStd) {
        this.vaStd = vaStd;
    }

    public double getSMean() {
        return sMean;
    }

    public void setSMean(double sMean) {
        this.sMean = sMean;
    }

    public double getSStd() {
        return sStd;
    }

    public void setSStd(double sStd) {
        this.sStd = sStd;
    }

    @Override
    public String toString() {
        return "SteadyStateSummary{" +
                "model=" + model +
                ", density=" + density +
                ", eta=" + eta +
                ", steadyStateStartTime=" + steadyStateStartTime +
                ", nReplicas=" + nReplicas +
                ", vaMean=" + vaMean +
                ", vaStd=" + vaStd +
                ", sMean=" + sMean +
                ", sStd=" + sStd +
                '}';
    }
}