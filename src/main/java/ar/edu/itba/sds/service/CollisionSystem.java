package ar.edu.itba.sds.service;

import ar.edu.itba.sds.model.entities.Event;
import ar.edu.itba.sds.model.entities.NewParticle;

import java.util.List;
import java.util.PriorityQueue;

public class CollisionSystem {

    private final PriorityQueue<Event> MinPQ;
    private final List<NewParticle> particles;
    private float t;
    private final float W;
    private final float L;
    private final float Dmin;
    private final float Dmax;
    private final float hz;


    public CollisionSystem(List<NewParticle> particles, float W, float L, float D, float hz){
        this.MinPQ = new PriorityQueue<>();
        this.particles = particles;
        this.W = W;
        this.L = L;
        this.Dmin = W/2 - D/2;
        this.Dmax = W/2 + D/2;
        this.hz = hz;
        t = 0f;
    }

    private void fillPQ(){

        for (NewParticle p : particles){

            float dtX = p.collidesX(L,Dmin,Dmax);
            float dtY = p.collidesY(W);

            if (dtX>0){
                MinPQ.add(new Event(dtX+t,p,null));
            }
            if (dtY > 0 ){
                MinPQ.add(new Event(dtY+t, null,p));
            }

            // TODO: aca habria que ver como mejorar la eficiencia.
            // TODO: Las particulas que ya fueron calculadas entre si no deberian visitarse de nuevo.
            for (NewParticle p2 : particles){
                float dtP = p.collidesWithParticle(p2);
                if (dtP >0){
                    MinPQ.add(new Event(dtP,p,p2));
                }
            }
        }
    }

    private void reDraw(){
        MinPQ.add(new Event(t + hz, null,null));
    }



}
