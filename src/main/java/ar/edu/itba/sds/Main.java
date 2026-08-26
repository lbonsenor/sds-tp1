package ar.edu.itba.sds;

import ar.edu.itba.sds.utils.ArgsParser;
import picocli.CommandLine;

public class Main {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new ArgsParser()).execute(args);
        System.exit(exitCode);
    }
}