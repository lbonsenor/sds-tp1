package ar.edu.itba.sds.model.entities;

public class NewParticleGravity extends NewParticle{

    private final float g;

    public NewParticleGravity(float x, float y, float vx, float vy, float m, float r, float epsilon, float g) {
        super(x, y, vx, vy, m, r, epsilon);
        this.g= g;
    }

    @Override
    public void move(float deltaTime) {
        super.x = x + vx * deltaTime;
        y = y + vy * deltaTime - g/2 * deltaTime * deltaTime;
        vy -= g * deltaTime;
    }

    //TODO: necesito chequear esto desp. Me queme un poco de SDS hoy.
    @Override
    public float collidesY(float W) {
        float minDt = Float.POSITIVE_INFINITY;

        // 1. Colisión con la Pared Inferior / Piso (y = 0)
        // Ecuación: a*dt^2 + b*dt + c = 0
        float a = -0.5f * g;
        float b =  this.vy;
        float cFloor = this.y - this.r; // Distancia actual al piso

        float dtFloor = solveQuadratic(a, b, cFloor);
        if (dtFloor > 0 && dtFloor < minDt) {
            minDt = dtFloor;
        }

        // 2. Colisión con la Pared Superior / Techo (y = W)
        float cCeil = this.y + this.r - W; // Distancia actual al techo

        float dtCeil = solveQuadratic(a, b, cCeil);
        if (dtCeil > 0 && dtCeil < minDt) {
            minDt = dtCeil;
        }

        return minDt;
    }

    // Método auxiliar para resolver a*x^2 + b*x + c = 0
    private float solveQuadratic(float a, float b, float c) {
        // Si no hay gravedad (g = 0), se simplifica a un movimiento lineal (b*dt + c = 0)
        if (a == 0) {
            if (b >= 0) return Float.POSITIVE_INFINITY; // No va en dirección a la pared
            return -c / b;
        }

        float discriminant = b * b - 4 * a * c;

        // Si el discriminante es negativo, la partícula no alcanza la pared
        if (discriminant < 0) {
            return Float.POSITIVE_INFINITY;
        }

        float sqrtDisc = (float) Math.sqrt(discriminant);

        // Las dos raíces posibles de la fórmula cuadrática
        float t1 = (-b + sqrtDisc) / (2 * a);
        float t2 = (-b - sqrtDisc) / (2 * a);

        // Queremos el tiempo futuro positivo más cercano (t > 0)
        if (t1 > 0 && t2 > 0) {
            return Math.min(t1, t2);
        } else if (t1 > 0) {
            return t1;
        } else if (t2 > 0) {
            return t2;
        }

        return Float.POSITIVE_INFINITY;
    }
}
