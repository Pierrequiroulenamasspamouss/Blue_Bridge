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

### Roadmap
#### Implemented
- Implemented a basic server to connect from anywhere (always free google EC2 instance for low userbase)
- Implemented a basic user authentication system
- Implemented a basic well monitoring system
- Implemented a basic map system
- Implemented a basic compass system
- Implemented a basic nearby users system

#### To implement
-Backporting to run on android 4.4 or 7 --> use older versions of Java to run with API 24(target) instead of 35(current)
- More robust server
- Real data
- Better UI
- server input sanitization
- different user types and privileges
- change language option
- Accept more users
- Web scraping to update the database server-side
- HTML frontpage for the server.
- Transition from http to https ? (not sure if it's possible with the free EC2 instance and Android)
- Fixes on the app:
    - User data security
    - Compass issues
    - Minor map issues



## Setup Instructions

1. Clone the repository:
```bash
git clone https://github.com/yourusername/wellconnect.git
```

2. Open the project in Android Studio

3. Configure the server it has to connect to :
```network_security_config.xml
<domain includeSubdomains="true">YOUR-DOMAIN</domain>
```
```strings.xml
<string name="GeneralIp">http://wellconnect.homeonthewater.com/</string>
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
Apache2 license for the server.

## Acknowledgments

- Thanks to all community members who provided feedback
- Special thanks to contributors and maintainers
- Built with support from the Kanard'eau organization.

## Contact

For support or queries, please contact:
- Email: pierresluse@gmail.com

or do a request on the gitHub project

- Website: http://wellconnect.homeonthewater.com


