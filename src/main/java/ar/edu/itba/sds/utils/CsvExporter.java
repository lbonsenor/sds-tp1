package ar.edu.itba.sds.utils;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

public class CsvExporter {

    private static final String TELEMETRY_DIR = "telemetry";

    /**
     * Single generic method to export any record/bean type to CSV.
     * OpenCSV extracts headers automatically from class annotations (@CsvBindByName).
     */
    public static <T> void exportTelemetry(Collection<T> records, File targetFile) {
        ensureDirectoryExists(targetFile.getParent());

        try (Writer writer = new FileWriter(targetFile)) {
            StatefulBeanToCsv<T> beanToCsv = new StatefulBeanToCsvBuilder<T>(writer)
                    .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                    .withSeparator(',')
                    .build();

            beanToCsv.write(records.iterator());
        } catch (IOException | CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            System.err.println("Error exporting telemetry records: " + e.getMessage());
        }
    }

    /**
     * Convenient string-path helper overload.
     */
    public static <T> void exportTelemetry(Collection<T> records, String fileName) {
        exportTelemetry(records, new File(TELEMETRY_DIR, fileName));
    }

    private static void ensureDirectoryExists(String path) {
        if (path == null) return;
        File dir = new File(path);
        if (!dir.exists()) dir.mkdirs();
    }
}