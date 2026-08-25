package ar.edu.itba.sds.service;

import ar.edu.itba.sds.model.entities.SizedParticle;

import java.util.*;

public class OffLatticeService <T extends SizedParticle> {
    private final int M;
    private final float L;
    private final float rc;

    public OffLatticeService(int M, float L, float rc) {
        this.M = M;
        this.L = L;
        this.rc = rc;
    }

    public Set<SizedParticle> getNewStandardListOfParticles(float deltaTime, float eta, int seed, Collection<T> particles){

        Set<SizedParticle> toReturn = new LinkedHashSet<>();
        for (SizedParticle p: particles){
            toReturn.add(p.getNewPositionStandard(deltaTime,eta,seed));
        }
        return toReturn;
    }

    public Set<SizedParticle> getNewVotanteListOfParticles(float deltaTime,  float eta, int seed,Collection<T> particles){

        Set<SizedParticle> toReturn = new LinkedHashSet<>();
        for (SizedParticle p: particles){
            toReturn.add(p.getNewPositionVotante(deltaTime,eta,seed));
        }
        return toReturn;
    }

}
