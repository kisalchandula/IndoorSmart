package com.kisal.indoorsmart;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import java.util.Timer;
import java.util.TimerTask;

public class SensorFusionService extends Service implements SensorEventListener {

    private SensorManager mSensorManager;
    private final float[] gyro = new float[3];
    private float[] gyroMatrix = new float[9];
    private final float[] gyroOrientation = new float[3];
    private final float[] magnet = new float[3];
    private final float[] accel = new float[3];
    private final float[] accMagOrientation = new float[3];
    private final float[] rotationMatrix = new float[9];
    private final float[] fusedOrientation = new float[3];
    public static final float EPSILON = 0.000000001f;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private boolean initState = true;
    public static final int TIME_CONSTANT = 30;
    private final Timer fuseTimer = new Timer();
    private int timestamp;

    // Initial position {x, y}
    private final PositionUpdater positionUpdater = new PositionUpdater();

    @Override
    public void onCreate() {
        super.onCreate();
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        gyroOrientation[0] = 0.0f;
        gyroOrientation[1] = 0.0f;
        gyroOrientation[2] = 0.0f;

        gyroMatrix[0] = 1.0f; gyroMatrix[1] = 0.0f; gyroMatrix[2] = 0.0f;
        gyroMatrix[3] = 0.0f; gyroMatrix[4] = 1.0f; gyroMatrix[5] = 0.0f;
        gyroMatrix[6] = 0.0f; gyroMatrix[7] = 0.0f; gyroMatrix[8] = 1.0f;

        initListeners();

        fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(), 5000, TIME_CONSTANT);
        Log.d("SensorFusionService", "Service started");
    }

    private void initListeners() {
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST);

        // Register the Step Counter sensor
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
        Log.d("SensorFusionService", "Service stopped");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_STEP_COUNTER:
                int newStepCount = (int) event.values[0];
                positionUpdater.updateStepCount(newStepCount);
                break;

            case Sensor.TYPE_ACCELEROMETER:
                float ax = event.values[0];
                float ay = event.values[1];
                float az = event.values[2];

                // Compute acceleration magnitude |A_k|
                float magnitude = (float) Math.sqrt(ax * ax + ay * ay + az * az);
                positionUpdater.addAccelerationSample(magnitude); // Pass acceleration data

                System.arraycopy(event.values, 0, accel, 0, 3);
                calculateAccMagOrientation();
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, magnet, 0, 3);
                break;

            case Sensor.TYPE_GYROSCOPE:
                gyroFunction(event);
                break;
        }

        // omit negative angles
        double theta;
        if (fusedOrientation[2] * 180 / Math.PI<0){
            theta = fusedOrientation[2] * 180 / Math.PI + 360;
        } else {
            theta = fusedOrientation[2] * 180 / Math.PI;
        }

        // send orientation to position update module
        positionUpdater.updateOrientation((float) (theta));

        // Send the data to the MainActivity
        sendSensorData();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void calculateAccMagOrientation() {
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
            SensorManager.getOrientation(rotationMatrix, accMagOrientation);
        }
    }

    private void sendSensorData() {

        // omit negative angles
        double theta;
        if (fusedOrientation[2] * 180 / Math.PI<0){
            theta = fusedOrientation[2] * 180 / Math.PI + 360;
        } else {
            theta = fusedOrientation[2] * 180 / Math.PI;
        }
        Intent intent = new Intent("SensorDataUpdate");

        intent.putExtra("orientation", theta);
        intent.putExtra("position", positionUpdater.getPosition());
        intent.putExtra("step", positionUpdater.getStepCount());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void gyroFunction(SensorEvent event) {
        // don't start until first accelerometer/magnetometer orientation has been acquired

        // initialisation of the gyroscope based rotation matrix
        if(initState) {
            float[] initMatrix;
            initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
            float[] test = new float[3];
            SensorManager.getOrientation(initMatrix, test);
            gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
            initState = false;
        }

        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        float[] deltaVector = new float[4];
        if(timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            System.arraycopy(event.values, 0, gyro, 0, 3);
            getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
        }

        // measurement done, save current time for next interval
        timestamp = (int) event.timestamp;

        // convert rotation vector into rotation matrix
        float[] deltaMatrix = new float[9];

        //The rotation vector can be converted into a matrix by calling the
        // conversion function getRotationMatrixFromVector from the SensoManager
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);

        /*The gyroMatrix is the total orientation calculated from all
        processed gyroscope measurements. The deltaMatrix holds the last rotation
        interval which needs to be applied to the gyroMatrix in the next step.
        This is done by multiplying gyroMatrix with deltaMatrix. This is equivalent
        to the Rotation of gyroMatrix about deltaMatrix*/
        // apply the new rotation interval on the gyroscope based rotation matrix
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);

        // get the gyroscope based orientation from the rotation matrix
        SensorManager.getOrientation(gyroMatrix, gyroOrientation);
    }

    private void getRotationVectorFromGyro(float[] gyroValues,
                                           float[] deltaRotationVector,
                                           float timeFactor)
    {
        float[] normValues = new float[3];

        // Calculate the angular speed of the sample
        float omegaMagnitude =
                (float)Math.sqrt(gyroValues[0] * gyroValues[0] +
                        gyroValues[1] * gyroValues[1] +
                        gyroValues[2] * gyroValues[2]);

        // Normalize the rotation vector if it's big enough to get the axis
        if(omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude;
            normValues[1] = gyroValues[1] / omegaMagnitude;
            normValues[2] = gyroValues[2] / omegaMagnitude;
        }

        // Integrate around this axis with the angular speed by the time step
        // in order to get a delta rotation from this sample over the time step
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        float thetaOverTwo = omegaMagnitude * timeFactor;
        float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
        deltaRotationVector[3] = cosThetaOverTwo;
    }

    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float)Math.sin(o[1]);
        float cosX = (float)Math.cos(o[1]);
        float sinY = (float)Math.sin(o[2]);
        float cosY = (float)Math.cos(o[2]);
        float sinZ = (float)Math.sin(o[0]);
        float cosZ = (float)Math.cos(o[0]);

        // rotation about x-axis (pitch)
        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;

        // rotation about y-axis (roll)
        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;

        // rotation about z-axis (azimuth)
        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;

        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }

    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }

    class calculateFusedOrientationTask extends TimerTask {
        private final KalmanFilter[] kalmanFilters = new KalmanFilter[3];

        public calculateFusedOrientationTask() {
            // Initialize one Kalman filter for each axis (yaw, pitch, roll)
            for (int i = 0; i < 3; i++) {
                kalmanFilters[i] = new KalmanFilter();
            }
        }

        public void run() {
            float dt = 0.03f; // Assume 33Hz update rate

            for (int i = 0; i < 3; i++) {
                fusedOrientation[i] = kalmanFilters[i].update(accMagOrientation[i], gyroOrientation[i], dt);
            }

            // Update gyro matrix with the fused orientation
            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);
        }
    }

}