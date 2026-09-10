package ar.edu.itba.sds.model.entities;

public class NewParticle {

    private static int nextId = 1;

    private final int id;
    private final float x;
    private final float y;
    private float vx;
    private float vy;
    private final float m;
    private final float r;
    private int collisionCount;
    private final float epsilon;

    public NewParticle(float x, float y, float vx, float vy, float m, float r, int collisionCount,float epsilon) {
        this(nextId++, x, y, vx,vy,m,r, collisionCount,epsilon);
    }

    public NewParticle(int id, float x, float y, float vx, float vy, float m, float r, int collisionCount,float epsilon) {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("Particle coordinates must be non-negative.");
        }
        this.id = id;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.m = m;
        this.r = r;
        this.collisionCount = collisionCount;
        this.epsilon = epsilon;
    }

    // --- Collisions ---

    // Hola, FLi desde la empresa.
    // En resumen tenemos una caja de W x L,
    // y el algoritmo predice el delta t que tiene que pasar para q la particula colisione con:
    // 1. Con la pared vertical. TODO: evaluar el caso especial, collision con "arco de gol".
    // 2. con la pared horizontal.
    // 3. Con otra particula

    //  TODO: evaluar el caso especial, collision con "arco de gol".
    public float collidesX(float W, float D){


        //DOC ORIGINAL: If the particle never collides with a vertical wall, return a negative
        //number (or +infinity = DOUBLE_MAX or NaN???)

        // vx = 0
        if (Float.compare(0.0f,vx)<=epsilon){
            return -1;
        }

        collisionCount++;
        // choque con pared der.
        if (vx > 0 ){
            return (W - r - x)/vx;
        }

        // choque con pared izq.
        return (r-x)/vx;
    }

    public float collidesY(float L){

        // vy = 0
        if (Float.compare(0.0f,vy)<=epsilon){
            return -1;
        }

        collisionCount++;

        // choque con pared arriba.
        if (vy > 0){
            return (L - r - y)/vy;
        }

        // choque con pared abajo.
        return (r-y)/vy;
    }


    // Esto esta dificil, porque tenes q considerar tmb q la otra particula tmb
    // puede rebotar, y cambiar de direccion?
    // TODO: preguntar en la clase consulta
    public float collidesWithParticle(NewParticle p){
        collisionCount++;
        return 0;
    }

    public void bounceX(){
        vx = -vx;
    }
    public void bounceY(){
        vy = -vy;
    }

    public int getCollisionCount(){
        return collisionCount;
    }


}
