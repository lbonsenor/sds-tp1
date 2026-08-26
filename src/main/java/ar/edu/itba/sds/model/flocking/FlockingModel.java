package ar.edu.itba.sds.model.flocking;

public enum FlockingModel {
    STANDARD,
    VOTER;

    public static FlockingModel fromCsv(String value) {
        if (value == null) {
            return null;
        }
        return FlockingModel.valueOf(value.trim().toUpperCase());
    }

    public String toCsv() {
        return this.name().toLowerCase();
    }

}
