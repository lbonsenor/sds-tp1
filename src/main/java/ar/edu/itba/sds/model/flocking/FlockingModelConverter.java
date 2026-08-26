package ar.edu.itba.sds.model.flocking;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

public class FlockingModelConverter extends AbstractBeanField<FlockingModel, String> {

    @Override
    protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
        if (value == null || value.trim().isEmpty()) {
            if (required) {
                throw new CsvConstraintViolationException("Field 'model' is required but was empty");
            }
            return null;
        }
        try {
            return FlockingModel.fromCsv(value);
        } catch (IllegalArgumentException e) {
            throw new CsvDataTypeMismatchException(value, FlockingModel.class,
                    "Invalid model value: '" + value + "'. Expected 'standard' or 'voter'.");
        }
    }

    @Override
    protected String convertToWrite(Object value) {
        if (value == null) {
            return "";
        }
        return ((FlockingModel) value).toCsv();
    }
}