# WellConnect - Community Water Well Monitoring

WellConnect is an Android application designed to help communities in Africa monitor and manage their water wells efficiently. The app facilitates communication between community members, tracks water consumption, and provides real-time information about nearby wells and users.

## Features

### Well Monitoring
- Real-time monitoring of water well status
- Track water consumption and usage patterns
- View well locations on an interactive map
- Configure well parameters and settings

### User Management
- Secure user authentication
- User profiles with customizable roles
- Water needs specification (farming, drinking, livestock)
- Priority-based water allocation system

### Community Features
- Find nearby users and wells
- Built-in compass navigation to wells
- Real-time user online status
- Distance calculations to nearby resources

### Communication
- Community message board
- Direct messaging between users
- Emergency alerts for well issues
- Water needs coordination

### Settings & Preferences
- Theme customization (Light/Dark/System)
- Language settings
- Location permissions management
- Profile customization

## Technical Details

### Requirements
- Android 7.0 (Nougat) or higher
- Gradle 8.11.1
- Kotlin 1.9.x
- Android Studio Arctic Fox or newer

### Architecture
- MVVM (Model-View-ViewModel) architecture
- Kotlin Coroutines for asynchronous operations
- Jetpack Compose for UI
- Hilt for dependency injection
- Kotlin Serialization for data handling

### Key Components
- `WellViewModel`: Manages well data and operations
- `UserViewModel`: Handles user authentication and profile management
- `NearbyUsersViewModel`: Manages nearby user discovery and location services
- `LocationUtils`: Provides location-based services and calculations
- `UserDataStore`: Handles data persistence and retrieval

## Setup Instructions

1. Clone the repository:
```bash
git clone https://github.com/yourusername/wellconnect.git
```

2. Open the project in Android Studio

3. Configure your local.properties file with required API keys:
```properties
MAPS_API_KEY=your_google_maps_api_key
```

4. Build and run the project:
```bash
./gradlew build
```

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/wellconnect/wellmonitoring/
│   │   │   ├── data/           # Data models and states
│   │   │   ├── network/        # API and network utilities
│   │   │   ├── ui/            # Compose UI components
│   │   │   ├── utils/         # Utility classes
│   │   │   └── viewmodels/    # ViewModels
│   │   └── res/               # Resources
├── build.gradle               # App-level build file
└── proguard-rules.pro        # ProGuard rules
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Thanks to all community members who provided feedback
- Special thanks to contributors and maintainers
- Built with support from [Your Organization]

## Contact

For support or queries, please contact:
- Email: support@wellconnect.com
- Website: www.wellconnect.com
