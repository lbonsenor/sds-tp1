package ar.edu.itba.sds.service;

import ar.edu.itba.sds.model.entities.SizedParticle;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

public class OffLatticeService <T extends SizedParticle> {
    private final int M;
    private final float L;
    private final float rc;
//    private final List<T>[][] grid;
    private HashSet<T> particles =new HashSet<T>();

    public OffLatticeService(int M, float L, float rc, Collection<T> particles) {
        this.M = M;
        this.L = L;
        this.rc = rc;
        this.particles = (HashSet<T>) particles;
    }

    public LinkedList<SizedParticle> getNewStandardListOfParticles(float deltaTime, int eta, int seed){

        LinkedList<SizedParticle> toReturn = new LinkedList<>();
        for (SizedParticle p: particles){
            p.getNewPositionStandard(deltaTime,eta,seed);
        }
        return toReturn;
    }

    public LinkedList<SizedParticle> getNewVotanteListOfParticles(float deltaTime,  int eta, int seed){

        LinkedList<SizedParticle> toReturn = new LinkedList<>();
        for (SizedParticle p: particles){
            p.getNewPositionVotante(deltaTime,eta,seed);
        }
        return toReturn;
    }

}
