Indoor Navigation System
📌 Overview
This Android application is designed for indoor navigation using a sensor fusion scheme. It integrates data from multiple sensors, including the accelerometer, magnetometer, gyroscope, GPS, and pedometer, to estimate the user's position and display it on an indoor floor map using Canvas.

🎯 Features
Sensor Fusion: Combines multiple sensor inputs for accurate positioning.
Dead Reckoning Algorithm: Computes position updates based on movement and orientation changes + Previous location.
Floor Map Display: Visualizes the user’s position on an indoor floor plan.
Kalman Filter (Optional): Enhances positioning accuracy by filtering sensor noise.
🏗️ Tech Stack
Programming Language: Java
Platform: Android
Sensors Used:
Accelerometer
Magnetometer
Gyroscope
Without GPS
Pedometer
🚀 Installation & Setup
Clone the repository:
sh
Copy
Edit
git clone https://github.com/your-repo/IndoorNavigation.git
cd IndoorNavigation
Open the project in Android Studio.
Connect an Android device or use an emulator.
Build and run the application.
⚙️ Usage
Launch the app and grant necessary permissions.
Move around, and the app will track your position on the floor map.
Observe real-time updates based on sensor data.
📌 Future Enhancements
Add BLE Beacons or WiFi positioning for improved accuracy.
Enable multi-floor navigation.
🛠️ Contributing
Feel free to fork the project, create a new branch, and submit a pull request with improvements!

📄 License
This project is licensed under the MIT License.
