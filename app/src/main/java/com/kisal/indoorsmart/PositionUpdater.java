package com.kisal.indoorsmart;

import java.util.LinkedList;
import java.util.Queue;

public class PositionUpdater {
    private int previousStepCount = -1;
    private int stepDifference = 0;
    private final float[] position = {0.0f, 0.0f}; // Initial position {X, Y}

    private static final int WINDOW_SIZE = 10; // Number of samples for averaging
    private final Queue<Float> accelerationBuffer = new LinkedList<>();
    private float accelerationSum = 0.0f;

    private float orientation = 0.0f; // Stores current orientation in degrees

    public void updateStepCount(int newStepCount) {
        if (previousStepCount != -1) {
            stepDifference = newStepCount - previousStepCount;
            if (stepDifference > 0) {
                float strideLength = calculateStrideLength();
                updatePosition(stepDifference, strideLength);
            }
        }
        previousStepCount = newStepCount;
    }

    // Collect sample accelerations
    public void addAccelerationSample(float accelerationMagnitude) {
        if (accelerationBuffer.size() >= WINDOW_SIZE) {
            accelerationSum -= accelerationBuffer.poll(); // Remove oldest sample
        }
        accelerationBuffer.add(accelerationMagnitude);
        accelerationSum += accelerationMagnitude;
    }

    // Stride length calculation
    private float calculateStrideLength() {
        if (accelerationBuffer.isEmpty()) {
            return 0.75f; // Default stride length
        }
        float meanAcceleration = accelerationSum / accelerationBuffer.size();
        return (float) (0.98 * Math.cbrt(meanAcceleration));
    }

    // Update position using the logic from the screenshot
    private void updatePosition(int stepDifference, float strideLength) {
        float distance = stepDifference * strideLength;
        float X0 = position[0]; // Previous X position
        float Y0 = position[1]; // Previous Y position
        float θ = orientation;  // Angle in degrees

        double X1, Y1;

        if (0 <= θ && θ < 90) {
            X1 = X0 + distance * Math.sin(Math.toRadians(θ));
            Y1 = Y0 + distance * Math.cos(Math.toRadians(θ));
        } else if (90 <= θ && θ < 180) {
            X1 = X0 + distance * Math.cos(Math.toRadians(θ - 90));
            Y1 = Y0 - distance * Math.sin(Math.toRadians(θ - 90));
        } else if (180 <= θ && θ < 270) {
            X1 = X0 - distance * Math.sin(Math.toRadians(θ - 180));
            Y1 = Y0 - distance * Math.cos(Math.toRadians(θ - 180));
        } else { // 270 <= θ < 360
            X1 = X0 - distance * Math.cos(Math.toRadians(θ - 270));
            Y1 = Y0 + distance * Math.sin(Math.toRadians(θ - 270));
        }

        position[0] = (float) X1;
        position[1] = (float) Y1;
    }

    // Update orientation (angle) from sensor fusion
    public void updateOrientation(float newOrientation) {
        this.orientation = newOrientation; // Angle in degrees
    }

    public float[] getPosition() {
        return position;
    }

    public int getStepCount() {
        return stepDifference;
    }
}
