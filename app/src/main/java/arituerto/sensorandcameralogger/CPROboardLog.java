package arituerto.sensorandcameralogger;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.Toast;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.module.Bmi160Accelerometer;
import com.mbientlab.metawear.module.Bmi160Gyro;
import com.mbientlab.metawear.module.Bmm150Magnetometer;
import com.mbientlab.metawear.module.Bmp280Barometer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by arituerto on 11/29/16.
 */

public class CPROboardLog {

    private static String ACC_STRM = "ACCELEROMETER";
    private static String STP_STRM = "STEPS";
    private static String GYR_STRM = "GYROSCOPE";
    private static String MAG_STRM = "MAGNETOMETER";
    private static String BAR_STRM = "BAROMETER";

    private boolean loggingON = false;
    private File loggingDir;
    private MetaWearBoard board;
    private String boardID;
    private Map<String, Logger> mLoggersMap;

    private boolean logAcc;
    private boolean logStp;
    private boolean logGyr;
    private boolean logBar;
    private boolean logMag;

    public interface CPROboardLogListener {
        public void onCPROConfigured(int state, String boardID);
    }

    private CPROboardLogListener listener;

    public CPROboardLog(String inputID,
                        final MetaWearBoard board,
                        File loggingDir,
                        boolean logAcc,
                        boolean logStp,
                        boolean logGyr,
                        boolean logBar,
                        boolean logMag) {

        Log.i(boardID, "CPROboardLog created");

        this.boardID = inputID;
        this.board = board;
        this.mLoggersMap = new HashMap<String, Logger>();
        this.loggingDir = loggingDir;

        this.logAcc = logAcc;
        this.logStp = logStp;
        this.logGyr = logGyr;
        this.logBar = logBar;
        this.logMag = logMag;

        this.listener = null;
        if (listener != null) {
            listener.onCPROConfigured(0, boardID);
        }

    }

    public void registerListener(CPROboardLogListener listener) {
        this.listener = listener;
    }

    public void connect() {

        Log.i(boardID, "connect");

        board.setConnectionStateHandler(new MetaWearBoard.ConnectionStateHandler() {
            @Override
            public void connected() {
                Log.i(boardID, "Meta Board connected!");
                if (logAcc) {
                    configureAccelerometerAxisLogging();
                }
                if (logStp) {
                    configureAccelerometerStepDetectorLogging();
                }
                if (logGyr) {
                    configureGyroscopeLogging();
                }
                if (logBar) {
                    configureBarometerLogging();
                }
                if (logMag) {
                    configureMagnetometerLogging();
                }
                if (listener != null) {
                    listener.onCPROConfigured(1, boardID);
                }
            }

            @Override
            public void failure(int status, Throwable error) {
                Log.i(boardID, "Meta Board failed to connect!");
                if (listener != null) {
                    listener.onCPROConfigured(2, boardID);
                }
            }
        });

        board.connect();
    }

    public void disconnect() {

        Log.i(boardID, "disconnect");

        for (Map.Entry<String, Logger> iStringLogger : mLoggersMap.entrySet()) {
            try {
                iStringLogger.getValue().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        board.disconnect();
    }

    public void activateLogging() {
        loggingON = true;
        Log.i(boardID, "logging ON");
    }

    public void deactivateLogging() {
        loggingON = false;
        Log.i(boardID, "logging OFF");
    }

    public Map<String, Logger> getLoggersMap() {
        return mLoggersMap;
    }

    private void configureAccelerometerAxisLogging() {

        Log.i(boardID, "configureAccelerometerAxisLogging");

        String loggerFileName = loggingDir.getPath() + "/sensor_" + boardID + "_" + ACC_STRM + "_log.csv";
        String csvFormat = "// SYSTEM_TIME [ns], EVENT_TIMESTAMP [ms], EVENT_" + ACC_STRM + "_VALUES";
        try {
            Logger logger = new Logger(loggerFileName);
            mLoggersMap.put(ACC_STRM, logger);
            try {
                logger.log(csvFormat);
                Log.i(boardID, "Accelerometer logger OK");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            // BMI160Accelerometer Axis sampling
            board.getModule(Bmi160Accelerometer.class).
                    configureAxisSampling().
                    setOutputDataRate(Bmi160Accelerometer.OutputDataRate.ODR_100_HZ).
                    setFullScaleRange(Bmi160Accelerometer.AccRange.AR_4G).
                    commit();
            board.getModule(Bmi160Accelerometer.class).
                    routeData().
                    fromAxes().
                    stream(ACC_STRM).
                    commit().
                    onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            Log.i(boardID, "Accelerometer Completion Handler OK");
                            result.subscribe(ACC_STRM, new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message msg) {
                                    Log.i(boardID, "Accelerometer reading" + msg.getData(CartesianFloat.class).toString());
                                    if (loggingON) {
                                        String csvString = SystemClock.elapsedRealtimeNanos() + "," +
                                                msg.getTimestamp().getTimeInMillis() + "," +
                                                msg.getData(CartesianFloat.class).x() + "," +
                                                msg.getData(CartesianFloat.class).y() + "," +
                                                msg.getData(CartesianFloat.class).z();
                                        try {
                                            mLoggersMap.get(ACC_STRM).log(csvString);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            });

                        }
                    });
            // START BMI160Accelerometer
            board.getModule(Bmi160Accelerometer.class).
                    enableAxisSampling();
            board.getModule(Bmi160Accelerometer.class).
                    start();
        } catch (UnsupportedModuleException e) {
            Log.e(boardID, "No accelerometer present on this board", e);
        }
    }

    private void configureAccelerometerStepDetectorLogging() {

        Log.i(boardID, "configureAccelerometerStepDetectorLogging");

        String loggerFileName = loggingDir.getPath() + "/sensor_" + boardID + "_" + STP_STRM + "_log.csv";
        String csvFormat = "// SYSTEM_TIME [ns], EVENT_TIMESTAMP [ms], EVENT_" + STP_STRM + "_VALUES";
        try {
            Logger logger = new Logger(loggerFileName);
            mLoggersMap.put(STP_STRM, logger);
            try {
                logger.log(csvFormat);
//                Log.i(boardID, "Step Detector logger OK");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            // BMI160Accelerometer Axis sampling
            board.getModule(Bmi160Accelerometer.class).
                    configureStepDetection().
                    setSensitivity(Bmi160Accelerometer.StepSensitivity.SENSITIVE).
                    commit();
            board.getModule(Bmi160Accelerometer.class).
                    routeData().
                    fromStepDetection().
                    stream(STP_STRM).
                    commit().
                    onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            Log.i(boardID, "Step Detector Completion Handler OK");
                            result.subscribe(STP_STRM, new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message msg) {
//                                    Log.i(boardID, "Step detected");
                                    if (loggingON) {
                                        String csvString = SystemClock.elapsedRealtimeNanos() + "," +
                                                msg.getTimestamp().getTimeInMillis() + "," +
                                                msg.getData(Integer.class);
                                        try {
                                            mLoggersMap.get(STP_STRM).log(csvString);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            });
                        }
                    });
            // START BMI160Accelerometer
            board.getModule(Bmi160Accelerometer.class).
                    enableStepDetection();
            board.getModule(Bmi160Accelerometer.class).
                    start();
        } catch (UnsupportedModuleException e) {
            Log.e(boardID, "No step detector present on this board", e);
        }

    }

    private void configureGyroscopeLogging() {

        Log.i(boardID, "configureGyroscopeLogging");

        String loggerFileName = loggingDir.getPath() + "/sensor_" + boardID + "_" + GYR_STRM + "_log.csv";
        String csvFormat = "// SYSTEM_TIME [ns], EVENT_TIMESTAMP [ms], EVENT_" + GYR_STRM + "_VALUES";
        try {
            Logger logger = new Logger(loggerFileName);
            mLoggersMap.put(GYR_STRM, logger);
            try {
                logger.log(csvFormat);
                Log.i(boardID, "Gyroscope logger OK");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            // BMI160Accelerometer Axis sampling
            board.getModule(Bmi160Gyro.class).
                    configure().setOutputDataRate(Bmi160Gyro.OutputDataRate.ODR_100_HZ).
                    commit();
            board.getModule(Bmi160Gyro.class).
                    configure().
                    setFullScaleRange(Bmi160Gyro.FullScaleRange.FSR_500).
                    commit();
            board.getModule(Bmi160Gyro.class).
                    routeData().
                    fromAxes().
                    stream(GYR_STRM).
                    commit().
                    onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            Log.i(boardID, "Gyroscope Completion Handler OK");
                            result.subscribe(GYR_STRM, new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message msg) {
//                                    Log.i(boardID, "Gyroscope reading" + msg.getData(CartesianFloat.class).toString());
                                    if (loggingON) {
                                        String csvString = SystemClock.elapsedRealtimeNanos() + "," +
                                                msg.getTimestamp().getTimeInMillis() + "," +
                                                msg.getData(CartesianFloat.class).x() + "," +
                                                msg.getData(CartesianFloat.class).y() + "," +
                                                msg.getData(CartesianFloat.class).z();
                                        try {
                                            mLoggersMap.get(GYR_STRM).log(csvString);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            });
                        }
                    });
            // START BMI160Accelerometer
            board.getModule(Bmi160Gyro.class).
                    start();
        } catch (UnsupportedModuleException e) {
            Log.e(boardID, "No gyroscope present on this board", e);
        }

    }

    private void configureBarometerLogging() {

        Log.i(boardID, "configureBarometerLogging");

        String loggerFileName = loggingDir.getPath() + "/sensor_" + boardID + "_" + BAR_STRM + "_log.csv";
        String csvFormat = "// SYSTEM_TIME [ns], EVENT_TIMESTAMP [ms], EVENT_" + BAR_STRM + "_VALUES";
        try {
            Logger logger = new Logger(loggerFileName);
            mLoggersMap.put(BAR_STRM, logger);
            try {
                logger.log(csvFormat);
                Log.i(boardID, "Barometer logger OK");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            // BMI160Accelerometer Axis sampling
            board.getModule(Bmp280Barometer.class).
                    configure().
                    setPressureOversampling(Bmp280Barometer.OversamplingMode.STANDARD).
                    commit();
            board.getModule(Bmp280Barometer.class).
                    routeData().
                    fromAltitude().
                    stream(BAR_STRM).
                    commit().
                    onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            Log.i(boardID, "Barometer Completion Handler OK");
                            result.subscribe(BAR_STRM, new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message msg) {
//                                    Log.i(boardID, "Barometer reading" + msg.getData(Float.class).toString());
                                    if (loggingON) {
                                        String csvString = SystemClock.elapsedRealtimeNanos() + "," +
                                                msg.getTimestamp().getTimeInMillis() + "," +
                                                msg.getData(Float.class);
                                        try {
                                            mLoggersMap.get(BAR_STRM).log(csvString);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            });
                        }
                    });
            // START BMI160Accelerometer
            board.getModule(Bmp280Barometer.class).
                    start();
        } catch (UnsupportedModuleException e) {
            Log.e(boardID, "No barometer present on this board", e);
        }

    }

    private void configureMagnetometerLogging() {

        Log.i(boardID, "configureMagnetometerLogging");

        String loggerFileName = loggingDir.getPath() + "/sensor_" + boardID + "_" + MAG_STRM + "_log.csv";
        String csvFormat = "// SYSTEM_TIME [ns], EVENT_TIMESTAMP [ms], EVENT_" + MAG_STRM + "_VALUES";
        try {
            Logger logger = new Logger(loggerFileName);
            mLoggersMap.put(MAG_STRM, logger);
            try {
                logger.log(csvFormat);
                Log.i(boardID, "Magnetometer logger OK");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            // BMI160Accelerometer Axis sampling
            board.getModule(Bmm150Magnetometer.class).
                    configureBFieldSampling().
                    setOutputDataRate(Bmm150Magnetometer.OutputDataRate.ODR_10_HZ).
                    commit();
            board.getModule(Bmm150Magnetometer.class).
                    routeData().
                    fromBField().
                    stream(MAG_STRM).
                    commit().
                    onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            Log.i(boardID, "Magnetometer Completion Handler OK");
                            result.subscribe(MAG_STRM, new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message msg) {
//                                    Log.i(boardID, "Magnetometer reading" + msg.getData(CartesianFloat.class).toString());
                                    if (loggingON) {
                                        String csvString = SystemClock.elapsedRealtimeNanos() + "," +
                                                msg.getTimestamp().getTimeInMillis() + "," +
                                                msg.getData(CartesianFloat.class).x() + "," +
                                                msg.getData(CartesianFloat.class).y() + "," +
                                                msg.getData(CartesianFloat.class).z();
                                        try {
                                            mLoggersMap.get(MAG_STRM).log(csvString);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            });
                        }
                    });
            // START BMI160Accelerometer
            board.getModule(Bmm150Magnetometer.class).
                    enableBFieldSampling();
            board.getModule(Bmm150Magnetometer.class).
                    start();
        } catch (UnsupportedModuleException e) {
            Log.e(boardID, "No magnetometer present on this board", e);
        }

    }
}
