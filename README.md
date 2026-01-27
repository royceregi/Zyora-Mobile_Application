# 🚗 Zyora - Effortless Mobility on the Go

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform">
  <img src="https://img.shields.io/badge/Language-Kotlin-blue.svg" alt="Language">
  <img src="https://img.shields.io/badge/License-Unspecified-lightgrey.svg" alt="License">
</p>

A sleek, next-generation mobile application for seamless ride booking, on-demand cab service, and managing your journeys with safety and flexibility at the core.

---

## ✨ Features

### 🗺️ Intuitive Booking
- Fastest way to book a cab from your location
- Real-time ride updates with live driver tracking
- Secure OTP-based ride start and end

### 🧑‍✈️ Driver & Ride Management
- View driver details and ratings before your ride
- Option to call or chat with assigned drivers
- Ride history and e-receipts for your records

### 💸 Flexible Payment Options
- Pay with cash, card, or wallet
- Smart fare estimates before confirming booking
- Promo code and discount support

### 🛡️ Enhanced Security
- In-app SOS button for emergencies
- Share live ride status with trusted contacts
- 24/7 in-app support and feedback system

### 🎨 Personalized Experience
- Favorite locations (Home/Work) for quick bookings
- Light/Dark mode and primary color themes
- Language support and accessibility

---

## 🛠️ Technical Stack

### Core Technologies
- **Language**: Kotlin 100%
- **Min SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 15 (API 35)
- **Build System**: Gradle (Kotlin DSL)

### Key Libraries & Frameworks
- **Google Maps SDK**: Location and routing
- **Material Design 3**: Clean, modern UI components
- **AndroidX Core & AppCompat**
- **ViewModel & LiveData**: Responsive data flows
- **Retrofit/OkHttp**: Network and REST API calls
- **Room DB**: Local ride and user data storage

### Architecture
- **Pattern**: MVVM with repository abstraction
- **Data Models**: Ride, Driver, User, Payment
- **UI**: XML layouts (with Compose planned for future)
- **Adapters**: RecyclerView lists for rides and drivers

---

## 📱 App Structure

```
Zyora-Mobile_Application/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/royce/zyora/
│   │   │   │   ├── model/               # Data models
│   │   │   │   ├── ui/                  # Screens & fragments
│   │   │   │   ├── network/             # API, repository
│   │   │   │   ├── adapter/             # Recycler adapters
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── RideBookingFragment.kt
│   │   │   │   ├── RideHistoryFragment.kt
│   │   │   │   ├── ProfileFragment.kt
│   │   │   │   ├── PaymentFragment.kt
│   │   │   └── res/                     # Resources (layouts, icons, etc.)
│   │   ├── androidTest/                 # Instrumentation tests
│   │   └── test/                        # Unit tests
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── DESIGN_GUIDELINES.md
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 35
- Kotlin 1.9+

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/RoyceAbiel426/Zyora-Mobile_Application.git
   cd Zyora-Mobile_Application
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory

3. **Sync Gradle**
   - Android Studio will automatically sync project dependencies

4. **Run the app**
   - Connect a device or start an emulator
   - Press the "Run" button (▶️) or hit `Shift + F10`

---

## 🛒 Key Components

### MainActivity
The root activity orchestrates navigation between booking interface, ride history, profile, and payment.

### Data Models

**Ride**
```kotlin
data class Ride(
    val id: String,
    val pickup: String,
    val drop: String,
    val status: String,
    val driver: Driver,
    val fare: Double,
    val timestamp: String
)
```

**Driver**
```kotlin
data class Driver(
    val id: String,
    val name: String,
    val rating: Float,
    val phone: String,
    val vehicle: String
)
```

**Payment**
```kotlin
data class Payment(
    val method: String,
    val transactionId: String?,
    val amount: Double
)
```

---

## 🎨 Design Highlights

- Material Design 3 for intuitive navigation & visual clarity
- Card-based ride display for history and active bookings
- Floating action buttons for ride actions (SOS, call driver)
- Smooth transitions and helpful microinteractions
- Accessibility ready, multiple languages

---

## 🔐 User Flow

1. **Launch**: Welcome and quick tour
2. **Book a Ride**: Enter destination, confirm details, and book
3. **Track Ride**: Real-time updates as your cab arrives and during the trip
4. **Payments**: Pay securely and access e-receipts
5. **History/Profile**: View ride logs, update preferences, and manage payments

---

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumentation Tests
```bash
./gradlew connectedAndroidTest
```

---

## 📦 Dependencies

Key dependencies include:
- `com.google.android.material:material:1.12.0`
- `androidx.core:core-ktx:1.17.0`
- `com.google.maps.android:maps:3.4.0`
- `com.squareup.retrofit2:retrofit:2.9.0`
- `androidx.lifecycle:lifecycle-livedata-ktx:2.8.0`
- `androidx.recyclerview:recyclerview:1.3.2`

Check [build.gradle.kts](app/build.gradle.kts) for the full list.

---

## 🛣️ Roadmap

- [ ] Push notifications for trip status
- [ ] Multiple language support
- [ ] Wallet and reward system
- [ ] Scheduled rides (advance booking)
- [ ] Deep integration with location services
- [ ] Live trip sharing with friends/family
- [ ] Ratings and feedback analytics

---

## 👤 Author

**Royce Abiel**  
- GitHub: [@RoyceAbiel426](https://github.com/RoyceAbiel426)

---

## 🤝 Contributing

Contributions, bug reports, and feature requests are welcome! Please check the [issues page](https://github.com/RoyceAbiel426/Zyora-Mobile_Application/issues).

---

## ⭐ Show Your Support

Leave a ⭐️ if Zyora helped you get where you need to go!

---

<p align="center">Made with 🚗, code, and Kotlin</p>