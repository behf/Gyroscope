package co.beh.gyroscope;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;


public class GyroscopeActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = GyroscopeActivity.class.getSimpleName();
    private SensorManager sensorManager;
    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];

    private final float[] mGyroscopeReading = new float[3];

    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];

    int counter;

    public static final String START_MARKER = "<";
    public static final String END_MARKER = ">";
    public static final String LEFT_MARKER = "L";
    public static final String RIGHT_MARKER = "R";
    public static final String MOTOR1_MARKER = "1";
    public static final String MOTOR2_MARKER = "2";

    public static final String MESSAGE_TEMPLATE = START_MARKER + "%s%s%d" + END_MARKER;


    float[] rotationMatrix = new float[16];
    float[] remappedRotationMatrix = new float[16];
    float[] orientations = new float[3];
    BluetoothAdapter bluetoothAdapter;
    TextView textX, textY, textZ;
    int Zdegree, Ydegree, Xdegree;
    private Sensor rotationVectorSensor;
    private int Zdegrees, Ydegrees, rightMspeed, leftMspeed;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManager =
                (SensorManager) getSystemService(SENSOR_SERVICE);
        rotationVectorSensor =
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        textX = findViewById(R.id.textX);
        textY = findViewById(R.id.textY);
        textZ = findViewById(R.id.textZ);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {

            Toast.makeText(getApplicationContext(), "Device doesnt Support Bluetooth", Toast.LENGTH_SHORT).show();

        }
        if (!bluetoothAdapter.isEnabled()) {

            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            startActivityForResult(enableAdapter, 0);

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
        // You must implement this callback in your code.
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.
        //
        // In this example, the sensor reporting delay is small enough such that
        // the application receives an update before the system checks the sensor
        // readings again.
/*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mSensorManager.registerListener(this, accSnsor,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mSensorManager.registerListener(this, magSensor,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }*/

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            sensorManager.registerListener(this, rotationVectorSensor,
                    150000);

        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Don't receive any more updates from either sensor.
        sensorManager.unregisterListener(this);
    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(
                    rotationMatrix, event.values);

            SensorManager.remapCoordinateSystem(rotationMatrix,
                    SensorManager.AXIS_X,
                    SensorManager.AXIS_Z,
                    remappedRotationMatrix);

            SensorManager.getOrientation(remappedRotationMatrix, orientations);

            for (int i = 0; i < 3; i++) {
                orientations[i] = (float) (Math.toDegrees(orientations[i]));
            }

//            Log.e(TAG, "1: " + orientations[0]);
//            Log.e(TAG, "2: " + orientations[1]);
//            Log.e(TAG, "3: " + orientations[2]);

            Zdegree = (int) (orientations[2] + 90);
            Xdegree = (int) orientations[1];
            Ydegree = (int) (orientations[0]);

            sendData(Xdegree, Zdegree);
//            counter++;
//            Zdegrees += Zdegree;
//            Ydegrees += Ydegree;
//            if (counter == 30) {
//                counter = 0;
//
////                calculate_duty_cycle(Zdegrees / 30);
//                Zdegrees = 0;
//                Ydegrees = 0;
//            }
//            calculate_duty_cycle(degree);
//            textX.setText("x: " + Xdegree);
//            textY.setText("y: " + Ydegree);
//            textZ.setText("z: " + Zdegree);




            /*if (orientations[2]+90 > 45) {
                getWindow().getDecorView().setBackgroundColor(Color.YELLOW);
            } else if (orientations[2]+90 < -45) {
                getWindow().getDecorView().setBackgroundColor(Color.BLUE);
            } else if (Math.abs(orientations[2]+90) < 10) {
                getWindow().getDecorView().setBackgroundColor(Color.WHITE);
            }*/

        }


    }

    void calculate_duty_cycle(int degree) {

        if (degree > 0) {
            try {
                textX.setText("dir: right");
                textY.setText("duty cycle: " + map(Math.abs(degree), 0, 90, 0, 255));
                ledControl.btSocket.getOutputStream().write(String.format(MESSAGE_TEMPLATE, MOTOR1_MARKER, RIGHT_MARKER, map(Math.abs(degree), 0, 90, 0, 100)).getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (degree < 0) {
            try {
                textX.setText("dir: left");
                textY.setText("duty cycle: " + map(Math.abs(degree), 0, 90, 0, 255));
                ledControl.btSocket.getOutputStream().write(String.format(MESSAGE_TEMPLATE, MOTOR1_MARKER, LEFT_MARKER, map(Math.abs(degree), 0, 90, 0, 100)).getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                textX.setText("stop");
                textY.setText("duty cycle: " + map(Math.abs(degree), 0, 90, 0, 255));
                ledControl.btSocket.getOutputStream().write(String.format(MESSAGE_TEMPLATE, MOTOR1_MARKER, RIGHT_MARKER, 1).getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    void sendData(int x, int z) {
        if (z < 5 && z > -5) {
            try {
                int speed = map(x, 0, 90, 0, 255);
                rightMspeed = map(x, 0, 90, 0, 100);
                leftMspeed = rightMspeed;

                textX.setText("right: " + speed);
                textY.setText("left: " + speed);
                textZ.setText("1");

                Log.d(TAG, "state: " + 1);
                Log.d(TAG, "x: " + x);
                Log.d(TAG, "z: " + z);
                Log.d(TAG, "speed: " + speed);
                Log.d(TAG, "************************************************");

                ledControl.btSocket.getOutputStream().write((START_MARKER + "B" + speed + END_MARKER).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (z < 0) { // slow down left motor to turn the car to left
            try {
                int rightspeed = map(x, 0, 90, 0, 255);
                int leftspeed = map((int)get_x(x, z), 0, 90, 0, 255);

                rightMspeed = map(rightspeed, 0, 255, 0, 100);
                leftMspeed = map(leftspeed, 0, 255, 0, 100);

                textX.setText("right: " + rightspeed);
                textY.setText("left: " + leftspeed);
                textZ.setText("2");

                Log.d(TAG, "state: " + 2);
                Log.d(TAG, "x: " + x);
                Log.d(TAG, "z: " + z);
                Log.d(TAG, "get_x(x, z): " + (int) get_x(x, z));
                Log.d(TAG, "map(get_x(x, z), 0, 90, 0, 255): " + leftspeed);
                Log.d(TAG, "map(x, 0, 90, 0, 255): " + rightspeed);
                Log.d(TAG, "************************************************");

                ledControl.btSocket.getOutputStream().write((START_MARKER + "L" + leftspeed + END_MARKER).getBytes());
                ledControl.btSocket.getOutputStream().write((START_MARKER + "R" + rightspeed + END_MARKER).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {  // slow down right motor to turn the car to right
            try {
                int leftspeed = map(x, 0, 90, 0, 255);
                int rightspeed = map((int)get_x(x, z), 0, 90, 0, 255);

                rightMspeed = map(rightspeed, 0, 255, 0, 100);
                leftMspeed = map(leftspeed, 0, 255, 0, 100);

                textX.setText("right: " + rightspeed);
                textY.setText("left: " + leftspeed);
                textZ.setText("3");

                Log.d(TAG, "state: " + 3);
                Log.d(TAG, "x: " + x);
                Log.d(TAG, "z: " + z);
                Log.d(TAG, "get_x(x, z): " + (int)get_x(x, z));
                Log.d(TAG, "map(x, 0, 90, 0, 255): " + leftspeed);
                Log.d(TAG, "map(get_x(x, z), 0, 90, 0, 255): " + rightspeed);
                Log.d(TAG, "************************************************");

                ledControl.btSocket.getOutputStream().write((START_MARKER + "L" + leftspeed + END_MARKER).getBytes());
                ledControl.btSocket.getOutputStream().write((START_MARKER + "R" + rightspeed + END_MARKER).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    static float get_x(float x, float z) {
        return  x  -  ( Math.abs(z) /  90) * x;
    }

    int map(int x, int in_min, int in_max, int out_min, int out_max) {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }


}