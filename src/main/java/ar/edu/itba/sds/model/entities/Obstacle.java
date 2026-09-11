package ar.edu.itba.sds.model.entities;

public class Obstacle {
    private static int nextId = 1;
    private final int id;
    private final float x;
    private final float y;
    private final float m;
    private final float r;
    //    private int collisionCount;


    public Obstacle(int id, float x, float y, float m, float r) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.m = m;
        this.r = r;
    }

    public Obstacle(float x, float y, float r) {
       this(nextId++,x,y, Float.MAX_VALUE,r);
    }

    public float getX() {
        return x;
    }

    public float getY(){
        return y;
    }

    public float getR() {
        return r;
    }
}
