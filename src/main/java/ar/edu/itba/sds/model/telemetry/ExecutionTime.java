package ar.edu.itba.sds.model.telemetry;

import ar.edu.itba.sds.model.flocking.FlockingModel;
import ar.edu.itba.sds.model.flocking.FlockingModelConverter;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;

public class ExecutionTime {

    @CsvCustomBindByName(column = "model", converter = FlockingModelConverter.class)
    private FlockingModel model;

    @CsvBindByName(column = "density")
    private double density;

    @CsvBindByName(column = "n_particles")
    private int nParticles;

    @CsvBindByName(column = "method")
    private String method; // e.g. "CIM" or "brute_force"

    @CsvBindByName(column = "execution_time_sec")
    private double executionTimeSec;

    @CsvBindByName(column = "n_steps")
    private int nSteps;

    @CsvBindByName(column = "box_length")
    private double boxLength;

    @CsvBindByName(column = "interaction_radius")
    private double interactionRadius;

    public ExecutionTime() {
    }

    public ExecutionTime(FlockingModel model, double density, int nParticles, String method,
                         double executionTimeSec, int nSteps, double boxLength, double interactionRadius) {
        this.model = model;
        this.density = density;
        this.nParticles = nParticles;
        this.method = method;
        this.executionTimeSec = executionTimeSec;
        this.nSteps = nSteps;
        this.boxLength = boxLength;
        this.interactionRadius = interactionRadius;
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

    public int getNParticles() {
        return nParticles;
    }

    public void setNParticles(int nParticles) {
        this.nParticles = nParticles;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public double getExecutionTimeSec() {
        return executionTimeSec;
    }

    public void setExecutionTimeSec(double executionTimeSec) {
        this.executionTimeSec = executionTimeSec;
    }

    public int getNSteps() {
        return nSteps;
    }

    public void setNSteps(int nSteps) {
        this.nSteps = nSteps;
    }

    public double getBoxLength() {
        return boxLength;
    }

    public void setBoxLength(double boxLength) {
        this.boxLength = boxLength;
    }

    public double getInteractionRadius() {
        return interactionRadius;
    }

    public void setInteractionRadius(double interactionRadius) {
        this.interactionRadius = interactionRadius;
    }

    @Override
    public String toString() {
        return "ExecutionTime{" +
                "model=" + model +
                ", density=" + density +
                ", nParticles=" + nParticles +
                ", method='" + method + '\'' +
                ", executionTimeSec=" + executionTimeSec +
                ", nSteps=" + nSteps +
                ", boxLength=" + boxLength +
                ", interactionRadius=" + interactionRadius +
                '}';
    }
}