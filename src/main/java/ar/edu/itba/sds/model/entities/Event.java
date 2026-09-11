package ar.edu.itba.sds.model.entities;

public class Event implements Comparable<Event>{

    private final float t;
    private final NewParticle a;
    private final NewParticle b;
    private final int countA;
    private final int countB;


    public Event(float t, NewParticle a, NewParticle b){
        this.t = t;
        this.a = a;
        this.b = b;
        this.countA = (a != null) ? a.getCollisionCount() : -1;
        this.countB = (b != null) ? b.getCollisionCount() : -1;
    }

    public float getTime() {
        return t;
    }

    public NewParticle getParticle1(){
        return a;
    }

    public NewParticle getParticle2(){
        return b;
    }

    public boolean wasSuperveningEvent() {
        // Si la partícula 'a' existe y su contador actual no coincide, el evento es inválido
        if (a != null && a.getCollisionCount() != countA) return true;

        // Si la partícula 'b' existe y su contador actual no coincide, el evento es inválido
        if (b != null && b.getCollisionCount() != countB) return true;

        // Si los contadores coinciden, la trayectoria no ha cambiado y el evento es válido
        return false;
    }

    @Override
    public int compareTo(Event o) {
        return Float.compare(this.t, o.t);
    }
}
