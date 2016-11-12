package arituerto.sensorandcameralogger;

/**
 * Created by arituerto on 30/10/16.
 */

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Logger {

    private BufferedOutputStream stream;
    private int nLogs = 0;

    public Logger(String filename) throws FileNotFoundException {
        this.stream = new BufferedOutputStream(new FileOutputStream(filename));
    }

    public Logger(File f) throws FileNotFoundException {
        this.stream = new BufferedOutputStream(new FileOutputStream(f));
    }

    public void log(String s) throws IOException {
        // logs string needs to include the timestamp
        this.stream.write(s.getBytes());
        this.stream.write(System.lineSeparator().getBytes());
        this.stream.flush();
        nLogs ++;
    }

    public void close() throws IOException {
        this.stream.close();
    }

    public int getnLogs() {
        return nLogs;
    }
}