package ar.edu.itba.sds.service;

import ar.edu.itba.sds.model.entities.Event;
import ar.edu.itba.sds.model.entities.NewParticle;
import ar.edu.itba.sds.model.entities.Obstacle;

import java.util.List;
import java.util.PriorityQueue;

public class CollisionSystem {

    private final PriorityQueue<Event> MinPQ;
    private final NewParticle[] particles;
    private final Obstacle[] obstacles;
    private float t;
    private final float W;
    private final float L;
    private final float Dmin;
    private final float Dmax;
    private final float hz;


    public CollisionSystem(NewParticle[] particles, Obstacle[] obstacles, float W, float L, float D, float hz){
        this.MinPQ = new PriorityQueue<>();
        this.particles = particles;
        this.obstacles = obstacles;
        this.W = W;
        this.L = L;
        this.Dmin = W/2 - D/2;
        this.Dmax = W/2 + D/2;
        this.hz = hz;
        t = 0f;
    }

    private void fillPQ(){

        for (int i =  0; i < particles.length; i ++){

            float dtX = particles[i].collidesX(L);
            float dtY = particles[i].collidesY(W);

            if (dtX>0){
                MinPQ.add(new Event(dtX+t,particles[i], (NewParticle) null));
            }
            if (dtY > 0 ){
                MinPQ.add(new Event(dtY+t, null,particles[i]));
            }

            // Las particulas que ya fueron calculadas entre si no deberian visitarse de nuevo.
            for (int j = i+1; j < particles.length; j ++){
                float dtP = particles[i].collidesWithParticle(particles[j]);
                if (dtP >0){
                    MinPQ.add(new Event(dtP,particles[i],particles[j]));
                }
            }

            for (int k =0 ; k < obstacles.length; k++){
                float dtO = particles[i].collidesWithObstacle(obstacles[k]);
                if (dtO>0){
                    MinPQ.add(new Event(dtO,particles[i],obstacles[k]));
                }
            }
        }
    }

    private void reDraw(){
        MinPQ.add(new Event((float) (t + 1.0 / hz), (NewParticle) null, (NewParticle) null));
    }

    private void executeMainFlow(){
        reDraw();
        while (!MinPQ.isEmpty()) {
            Event e = MinPQ.poll();

            // 1. VERIFICACIÓN DE VALIDEZ
            if (e.wasSuperveningEvent()) {
                continue; // Es un evento viejo/obsoleto, lo salteamos y pasamos al siguiente
            }

            // 2. AVANZAR EL TIEMPO Y PROCESAR EL EVENTO VÁLIDO
            float dt = e.getTime() - t;

            // Mover todas las partículas hasta el tiempo e.getTime()
            for (NewParticle p : particles) {
                p.move(dt);
            }
            t = e.getTime();

            // 3. EXECUTAR EL IMPACTO Y RE-PREDECIR
            NewParticle a = e.getParticle1();
            NewParticle b = e.getParticle2();
            Obstacle o = e.getO();

            if (a != null && o !=null){
                a.bounceObstacle(o);
            }
            else if (a != null && b != null) {
                // Choque entre dos partículas
                a.bounceParticle(b);
                // aca b == null. Pero lo escribimos explicitamente.
            } else if (a != null && b == null) {
                // Choque con pared vertical
                a.bounceX(Dmin,Dmax);
            } else if (a == null && b != null) {
                // Choque con pared horizontal
                b.bounceY();
            } else {
                // Evento de Redraw / Renderizado
                reDraw();
            }
            fillPQ();
        }
    }
}
