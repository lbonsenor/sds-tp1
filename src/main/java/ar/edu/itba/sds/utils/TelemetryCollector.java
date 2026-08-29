package ar.edu.itba.sds.utils;

import ar.edu.itba.sds.model.telemetry.ClusterDetail;
import ar.edu.itba.sds.model.telemetry.ExecutionTime;
import ar.edu.itba.sds.model.telemetry.ParticlePoint;
import ar.edu.itba.sds.model.telemetry.RunConfig;
import ar.edu.itba.sds.model.telemetry.TimeObservable;

import java.util.ArrayList;
import java.util.List;

public class TelemetryCollector {

    private final List<ExecutionTime> executionTimes = new ArrayList<>();
    private final List<TimeObservable> timeObservables = new ArrayList<>();
    private final List<ClusterDetail> clusterDetails = new ArrayList<>();
    private final List<ParticlePoint> particlePoints = new ArrayList<>();

    public void recordExecutionTime(ExecutionTime time) {
        executionTimes.add(time);
    }

    public void recordObservable(TimeObservable observable) {
        timeObservables.add(observable);
    }

    public void recordClusterDetails(List<ClusterDetail> details) {
        clusterDetails.addAll(details);
    }

    public void recordParticlePoints(List<ParticlePoint> points) {
        particlePoints.addAll(points);
    }

    public void exportAll(RunConfig config) {
        CsvExporter.exportTelemetry(List.of(config), "run_config.csv", true);
        CsvExporter.exportTelemetry(executionTimes, "execution_times_cim.csv", true);
        CsvExporter.exportTelemetry(timeObservables, "time_observables.csv", true);
        CsvExporter.exportTelemetry(clusterDetails, "cluster_details.csv", true);
        CsvExporter.exportTelemetry(particlePoints, "particle_data.csv", true);
    }
}