# Indoor Navigation System Using Sensor Fusion

This Android-based Indoor Navigation System uses sensor fusion and a dead-reckoning algorithm to calculate the user's position and estimate movement within an indoor environment (Without GPS Signals). The system runs continuously in the background as a foreground service, providing real-time updates of the user's location.

## How It Works

1. **Sensor Fusion**: The system integrates data from the accelerometer, gyroscope, magnetometer, and pedometer to estimate the user’s position and orientation in real time.
   
2. **Dead Reckoning Algorithm**: Based on the user’s previous position and movement data (from sensors), the algorithm calculates the current location, considering direction, speed, and distance travelled.

3. **Kalman Filter Algorithm**: This filter reduces the noise in sensor data, improving the accuracy of the position estimation by accounting for sensor inaccuracies and environmental interference.

4. **Foreground Service**: The navigation service runs in the background, continuously tracking the user’s position and updating the indoor map as they move showing the path. The service starts when the user 
     activates the navigation command.
