package ar.edu.itba.sds.model.entities;

public class NewParticle {

    private static int nextId = 1;

    private final int id;
    private float x;
    private float y;
    private float vx;
    private float vy;
    private final float m;
    private final float r;
    private int collisionCount;
    private final float epsilon;
    private boolean hasGoal;

    public NewParticle(float x, float y, float vx, float vy, float m, float r, float epsilon) {
        this(nextId++, x, y, vx,vy,m,r, 0,epsilon);
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
        this.hasGoal = false;
    }

    // --- Collisions ---

    // Caja de W vertical x L horizontal
    public float collidesX(float L){

        if (Float.compare(0.0f,vx)<=epsilon){
            return -1;
        }

        if (vx > 0 ){
            return (L - r - x)/vx;
        }
        return (r-x)/vx;
    }

    public float collidesY(float W){

        if (Float.compare(0.0f,vy)<=epsilon){
            return -1;
        }

        if (vy > 0){
            return (W - r - y)/vy;
        }
        return (r-y)/vy;
    }


    public float collidesWithParticle(NewParticle p){

        if (this ==p) return -1;

        float dx = p.x - this.x;
        float dy = p.y - this.y;
        float dvx = p.vx - this.vx;
        float dvy = p.vy - this.vy;
        float drdr = dx*dx + dy*dy;
        float dvdv = dvx * dvx + dvy * dvy;
        float dvdr = dvx*dx + dvy*dy;
        float sigma = this.r + p.r;
        float d = (dvdr * dvdr) - dvdv * (drdr - sigma * sigma);

        if (dvdr >= 0 || d <0 ){
            return -1;
        }

        return (float) (-(dvdr + Math.sqrt(d)) / dvdv);
    }

    // --- Bounce ---

    public void bounceX(float Dmin, float Dmax){

        // TODO: evaluar el caso especial, collision con "arco de gol".
        // dudo de esta impl
        if (y+r >= Dmin && y+r <=Dmax){
            this.hasGoal = true;
        }

        collisionCount++;
        vx = -vx;
    }

    public void bounceY(){
        collisionCount++;
        vy = -vy;
    }

    public void bounceParticle (NewParticle p){

        float dx = p.x - this.x;
        float dy = p.y - this.y;
        float dvx = p.vx - this.vx;
        float dvy = p.vy - this.vy;

        float dvdr = dvx*dx + dvy*dy;
        float sigma = this.r + p.r;


        float j = 2*this.m*p.m *(dvdr)/(sigma*(this.m+p.m));
        float jx = j * dx / sigma;
        float jy = j * dy / sigma;

        this.vx = vx + jx/m;
        this.vy = vy + jy/m;

        p.vx = p.vx - jx/p.m;
        p.vy = p.vy - jy/p.m;

        collisionCount++;
    }

    // --- Move ---

    public void move(float deltaTime) {
        x = x + vx * deltaTime;
        y = y + vy * deltaTime;
    }

    public int getCollisionCount(){
        return collisionCount;
    }
}
