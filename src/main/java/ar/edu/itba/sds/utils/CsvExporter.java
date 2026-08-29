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
    public static <T> void exportTelemetry(Collection<T> records, File targetFile, boolean append) {
        ensureDirectoryExists(targetFile.getParent());

        // Check if file exists and is non-empty before opening the writer
        boolean fileExists = targetFile.exists() && targetFile.length() > 0;

        try (Writer writer = new FileWriter(targetFile, append)) {
            StatefulBeanToCsvBuilder<T> builder = new StatefulBeanToCsvBuilder<T>(writer)
                    .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                    .withSeparator(',');

            // If appending to an existing file, suppress writing the headers
            if (append && fileExists) {
                builder.withOrderedResults(true); // Ensures column alignment matches
            }

            StatefulBeanToCsv<T> beanToCsv = builder.build();
            beanToCsv.write(records.iterator());
        } catch (IOException | CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            System.err.println("Error exporting telemetry records: " + e.getMessage());
        }
    }

    /**
     * Convenient string-path helper overload.
     */
    public static <T> void exportTelemetry(Collection<T> records, String fileName, boolean append) {
        exportTelemetry(records, new File(TELEMETRY_DIR, fileName), append);
    }

    private static void ensureDirectoryExists(String path) {
        if (path == null) return;
        File dir = new File(path);
        if (!dir.exists()) dir.mkdirs();
    }
}