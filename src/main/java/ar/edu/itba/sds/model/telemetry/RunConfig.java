package ar.edu.itba.sds.model.telemetry;

import ar.edu.itba.sds.model.flocking.FlockingModel;
import ar.edu.itba.sds.model.flocking.FlockingModelConverter;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;

public class RunConfig {

    @CsvBindByName(column = "run_id")
    private String runId;

    @CsvCustomBindByName(column = "model", converter = FlockingModelConverter.class)
    private FlockingModel model; // standard / voter

    @CsvBindByName(column = "length")
    private int length; // l, grid/box side length

    @CsvBindByName(column = "cut_off")
    private float cutOff; // rc, neighbor cut-off distance

    @CsvBindByName(column = "min_radius")
    private float minRadius; // riMin, particle radius lower bound

    @CsvBindByName(column = "max_radius")
    private float maxRadius; // riMax, particle radius upper bound

    @CsvBindByName(column = "cell_grid_split")
    private int cellGridSplit; // m, CIM grid split factor

    @CsvBindByName(column = "n_particles")
    private int nParticles; // n

    @CsvBindByName(column = "periodic_boundary")
    private boolean periodicBoundary; // contour

    @CsvBindByName(column = "delta_t")
    private float deltaT; // simulation time step

    @CsvBindByName(column = "total_time")
    private float totalTime; // entireT, total simulated time

    @CsvBindByName(column = "eta")
    private float eta; // noise parameter

    @CsvBindByName(column = "seed")
    private long seed;

    public RunConfig() {
    }

    public RunConfig(String runId, FlockingModel model, int length, float cutOff, float minRadius,
                     float maxRadius, int cellGridSplit, int nParticles, boolean periodicBoundary,
                     float deltaT, float totalTime, float eta, long seed) {
        this.runId = runId;
        this.model = model;
        this.length = length;
        this.cutOff = cutOff;
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        this.cellGridSplit = cellGridSplit;
        this.nParticles = nParticles;
        this.periodicBoundary = periodicBoundary;
        this.deltaT = deltaT;
        this.totalTime = totalTime;
        this.eta = eta;
        this.seed = seed;
    }

    /**
     * Fraction of particles in the box: N / L^2. Not persisted as its own
     * column (it's derived from n_particles and length), but handy when you
     * need to bucket runs by the rho = 2, 4, 8 densities asked in the TP.
     */
    public double getDensity() {
        return (double) nParticles / (length * (double) length);
    }

    /**
     * Number of simulation steps: total_time / delta_t, matching the
     * `for (float t = 0; t < entireT; t += deltaT)` loop in ArgsParser.
     */
    public int getNSteps() {
        return (int) Math.ceil(totalTime / deltaT);
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public FlockingModel getModel() {
        return model;
    }

    public void setModel(FlockingModel model) {
        this.model = model;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public float getCutOff() {
        return cutOff;
    }

    public void setCutOff(float cutOff) {
        this.cutOff = cutOff;
    }

    public float getMinRadius() {
        return minRadius;
    }

    public void setMinRadius(float minRadius) {
        this.minRadius = minRadius;
    }

    public float getMaxRadius() {
        return maxRadius;
    }

    public void setMaxRadius(float maxRadius) {
        this.maxRadius = maxRadius;
    }

    public int getCellGridSplit() {
        return cellGridSplit;
    }

    public void setCellGridSplit(int cellGridSplit) {
        this.cellGridSplit = cellGridSplit;
    }

    public int getNParticles() {
        return nParticles;
    }

    public void setNParticles(int nParticles) {
        this.nParticles = nParticles;
    }

    public boolean isPeriodicBoundary() {
        return periodicBoundary;
    }

    public void setPeriodicBoundary(boolean periodicBoundary) {
        this.periodicBoundary = periodicBoundary;
    }

    public float getDeltaT() {
        return deltaT;
    }

    public void setDeltaT(float deltaT) {
        this.deltaT = deltaT;
    }

    public float getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(float totalTime) {
        this.totalTime = totalTime;
    }

    public float getEta() {
        return eta;
    }

    public void setEta(float eta) {
        this.eta = eta;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    @Override
    public String toString() {
        return "RunConfig{" +
                "runId='" + runId + '\'' +
                ", model=" + model +
                ", length=" + length +
                ", cutOff=" + cutOff +
                ", minRadius=" + minRadius +
                ", maxRadius=" + maxRadius +
                ", cellGridSplit=" + cellGridSplit +
                ", nParticles=" + nParticles +
                ", periodicBoundary=" + periodicBoundary +
                ", deltaT=" + deltaT +
                ", totalTime=" + totalTime +
                ", eta=" + eta +
                ", seed=" + seed +
                '}';
    }
}