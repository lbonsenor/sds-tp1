package ar.edu.itba.sds.utils;

import ar.edu.itba.sds.model.entities.SizedParticle;

import java.util.HashSet;
import java.util.Set;

public class RandomParticleGenerator {

    /**
     * Generates N non-overlapping SizedParticles within a square domain of side L.
     *
     * @param n     Number of particles to generate
     * @param l     Domain side length
     * @param rMin  Minimum particle radius
     * @param rMax  Maximum particle radius
     * @return Set of non-overlapping SizedParticles
     */
    public static Set<SizedParticle> generate(int n, float l, float rMin, float rMax) {
        Set<SizedParticle> particles = new HashSet<>();

        while (particles.size() < n) {
            float r = (float) (rMin + Math.random() * (rMax - rMin));
            float x = Math.clamp((float) (Math.random() * l), r, l - r);
            float y = Math.clamp((float) (Math.random() * l), r, l - r);

            SizedParticle p = new SizedParticle(x, y, r);

            // Verify particle boundary and check against collisions with existing particles
            if (p.existsIn(0, 0, l, l) && !hasCollision(p, particles)) {
                particles.add(p);
            }
        }

        return particles;
    }

    private static boolean hasCollision(SizedParticle candidate, Set<SizedParticle> existingParticles) {
        for (SizedParticle other : existingParticles) {
            if (candidate.collidesWith(other)) {
                return true;
            }
        }
        return false;
    }
}