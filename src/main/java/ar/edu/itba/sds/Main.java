package ar.edu.itba.sds;

import ar.edu.itba.sds.model.ArgsParser;
import ar.edu.itba.sds.model.Entity2D;
import ar.edu.itba.sds.model.entities.Particle;
import ar.edu.itba.sds.model.entities.SizedParticle;
import ar.edu.itba.sds.service.CellIndexService2;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    // Dinamico: T | ID | PX | PY | Vecinos
    //  t0 0 1 1 nadie
    //  t0 1 3 3 nadie
    //  ...
    //  t1 0 1 1 nadie

    // Estatico: ID | PR |
    static void generateCSV(Collection<SizedParticle> particles, int t) {
        // Convert to List to establish a consistent ID system via indices
        List<SizedParticle> particleList = new ArrayList<>(particles);

        // 1. Generate an out folder
        File outDir = new File("out");
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        // 2. Populate Static Data (ID | PR)
        File staticFile = new File("out/static_data.csv");
        try (FileWriter staticWriter = new FileWriter(staticFile)) {
            staticWriter.write("ID,radius\n");
            for (int i = 0; i < particleList.size(); i++) {
                SizedParticle p = particleList.get(i);
                // Calculating radius since SizedParticle lacks a getR() method
                float r = (p.getMaxX() - p.getMinX()) / 2.0f;
                staticWriter.write(i + "," + r + "\n");
            }
        } catch (IOException e) {
            System.err.println("Error writing static CSV: " + e.getMessage());
        }

        // 3. Populate Dynamic Data (t | ID | PX | PY | Vecinos)
        File dynamicFile = new File("out/dynamic_data.csv");
        // Use append mode so we can add multiple timesteps 't' over time
        boolean appendMode = t > 0;
        try (FileWriter dynamicWriter = new FileWriter(dynamicFile, appendMode)) {
            if (t == 0) {
                dynamicWriter.write("t,ID,Xpos,Ypos,neighbours\n");
            }

            for (int i = 0; i < particleList.size(); i++) {
                SizedParticle p = particleList.get(i);
                // Calculating X and Y since SizedParticle lacks getX() and getY() methods
                float x = (p.getMaxX() + p.getMinX()) / 2.0f;
                float y = (p.getMaxY() + p.getMinY()) / 2.0f;

                // Find neighbor IDs based on their index in the particleList
                String neighborsStr = p.getNeighbors().stream()
                        .map(neighbor -> String.valueOf(particleList.indexOf(neighbor)))
                        .collect(Collectors.joining(" "));

                if (neighborsStr.isEmpty()) {
                    neighborsStr = "nadie";
                } else {
                    neighborsStr = "[" + neighborsStr + "]";
                }

                dynamicWriter.write(t + "," + i + "," + x + "," + y + "," + neighborsStr + "\n");
            }
        } catch (IOException e) {
            System.err.println("Error writing dynamic CSV: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        ArgsParser parser = new ArgsParser(args);
        final int m  = parser.getM();         // Size of the mxm matrix
        final int l = parser.getL();         // Longitude
        final float rc = parser.getRc();        // Max neighbor distance
        final float riMin = parser.getRiMin();
        final float riMax = parser.getRiMax();
        final int n = parser.getN();
        final boolean contour = parser.hasContour();
        final Set<SizedParticle> particles = new HashSet<>();


        // Create particles
        for (int i = 0; i < n; i++) {

            float r = (float) (riMin + Math.random() * (riMax - riMin));
            float x = Math.clamp((float) (Math.random() * l), r, l - r);
            float y = Math.clamp((float) (Math.random() * l), r, l - r);
            SizedParticle p = new SizedParticle(
                    x,
                    y,
                    r
            );
            // check if inside table
            if(!p.existsIn(0,0,l,l)) {
                i--;
                continue;
            }
            //check if new p collides with existing particles.
            if(!collidesWithOthers(p, particles)){
                particles.add(p);
            }
            else {
                i--;
            }
        }


        System.out.println("N particles: " +particles.size());
        System.out.println("Grid size: " + l + " x " + l);
        System.out.println("m: " + m);
        System.out.println("r: " + rc);

        final CellIndexService2<SizedParticle> serv = new CellIndexService2<>(m,l,rc,particles);

        LocalDateTime start = LocalDateTime.now();
        for (SizedParticle p : particles) {
            serv.calculateNeighbors(p, contour);
        }
        LocalDateTime end = LocalDateTime.now();
        generateDataCsv(start, end, n, l, m, rc, riMin, riMax);

        generateCSV(particles,0);

        for (SizedParticle p : particles) {
            System.out.println("Particle: " + p);
            System.out.println("Neighbors: " + p.getNeighbors());
        }
    }

    private static void generateDataCsv(LocalDateTime start, LocalDateTime end, int n, int l, int m, float rc, float riMin, float riMax) {
        long executionTimeMs = java.time.Duration.between(start, end).toMillis();
        System.out.println("Time taken to calculate neighbors: " + executionTimeMs + " ms");

        File outDir = new File("out");
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        File executionFile = new File("telemetry/execution_data.csv");
        // Check if the file exists before we open the FileWriter
        boolean fileExists = executionFile.exists();

        // Pass 'true' to FileWriter to enable append mode
        try (FileWriter execWriter = new FileWriter(executionFile, true)) {

            // Only write the CSV Headers if the file was just created
            if (!fileExists) {
                execWriter.write("N,L,M,Rc,riMin,riMax,density,execution_time_ms\n");
            }

            // Calculate density
            float density = (float) n / l;

            // Write data
            execWriter.write(n + "," +
                    l + "," +
                    m + "," +
                    rc + "," +
                    riMin + "," +
                    riMax + "," +
                    density + "," +
                    executionTimeMs + "\n");

        } catch (IOException e) {
            System.err.println("Error writing execution CSV: " + e.getMessage());
        }
    }


    private static boolean collidesWithOthers(SizedParticle newParticle, Set<SizedParticle> particles) {
        boolean collides = false;
        for (SizedParticle other :particles ) {
            if (newParticle.collidesWith(other)) {
                collides = true;
                break;
            }
        }
        return collides;
    }
}
