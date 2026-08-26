package ar.edu.itba.sds.utils;

public class ArgsParser {
    int l = 20;
    float rc = 3;
    float riMin = 0.23f;
    float riMax = 0.26f;
    int m = (int) Math.floor(l / (rc + 2 * riMax));
    int n = 7;
    boolean contour = false;
    boolean hasM = false;

    float deltaT = 0.5f;
    float entireT = 5f;
    float eta = 2f;


    public ArgsParser(String[] args) {
        for (int i = 0; i < args.length; i++) {
        switch (args[i]) {
            case "-l":
            case "--length":
                if (i + 1 < args.length) {
                    try {
                        l = Integer.parseInt(args[++i]);
                    } catch (NumberFormatException e) {
                        System.err.println("Error: Length must be a valid integer.");
                        System.exit(1);
                    }
                }
                break;
            case "-rc":
            case "--cut-off":
                if (i + 1 < args.length) {
                    try {
                        rc = Float.parseFloat(args[++i]);
                    } catch (NumberFormatException e) {
                        System.err.println("Error: Cut-off must be a valid float.");
                        System.exit(1);
                    }
                }
                break;
            case "-ri-min":
            case "--min-radius":
                if (i + 1 < args.length) {
                    try {
                        riMin = Float.parseFloat(args[++i]);
                    } catch (NumberFormatException e) {
                        System.err.println("Error: Min radius must be a valid float.");
                        System.exit(1);
                    }
                }
                break;
            case "-ri-max":
            case "--max-radius":
                if (i + 1 < args.length) {
                    try {
                        riMax = Float.parseFloat(args[++i]);
                    } catch (NumberFormatException e) {
                        System.err.println("Error: Max radius must be a valid float.");
                        System.exit(1);
                    }
                }
                break;
            case "-m":
                if (i + 1 < args.length) {
                    try {
                        m = Integer.parseInt(args[++i]);
                        hasM = true;
                    } catch (NumberFormatException e) {
                        System.err.println("Error: m must be a valid integer.");
                        System.exit(1);
                    }
                }
                break;
            case "-n":
                if (i + 1 < args.length) {
                    try {
                        n = Integer.parseInt(args[++i]);
                    } catch (NumberFormatException e) {
                        System.err.println("Error: n must be a valid integer.");
                        System.exit(1);
                    }
                }
                break;
             case "-c":
             case "--contour":
                 if (i + 1 < args.length) {
                    contour = Boolean.parseBoolean(args[++i]);
                 }
                 break;
            }
        }
        if(!hasM) {
            m = (int) Math.floor(l / (rc + 2 * riMax));
        }
    }


    public int getN(){
        return n;
    }


    public int getL() {
        return l;
    }



    public float getRc() {
        return rc;
    }


    public float getRiMin() {
        return riMin;
    }



    public float getRiMax() {
        return riMax;
    }


    public int getM() {
        return m;
    }

    public boolean hasContour() {
        return contour;
    }

    public float getDeltaT(){
        return deltaT;
    }

    public float getEntireT(){
        return entireT;
    }

    public float getEta(){
        return eta;
    }

}
