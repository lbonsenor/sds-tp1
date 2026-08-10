package ar.edu.itba.sds;

import ar.edu.itba.sds.model.ArgsParser;
import ar.edu.itba.sds.model.Entity2D;
import ar.edu.itba.sds.model.entities.SizedParticle;
import ar.edu.itba.sds.service.CellIndexService2;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
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
        System.out.println("Time taken to calculate neighbors: " + java.time.Duration.between(start, end).toMillis() + " ms");



        for (SizedParticle p : particles) {
            System.out.println("Particle: " + p);
            System.out.println("Neighbors: " + p.getNeighbors());
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
