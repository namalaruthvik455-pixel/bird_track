
# BirdTrack Pro 🦜

BirdTrack Pro is a professional-grade field note and ornithology app designed for birdwatching and citizen science. This app enables users to record their birding trips, identify species using AI technology, and create a permanent record of their “Life List” with environmental information auto-captured.

![App Icon Concept](https://img.icons8.com/fluency/96/bird.png)

---

## 🌟 Key Features

### 🗺️ Expedition Management
*   **Field Notes:** Log every sighting with precise details, including species name, quantity, and behavioral notes.
*   **Expedition Logs:** Organize your outings into "Trips" with metadata like date, time, and location.
*   **Weather Integration:** Automatically captures temperature and wind speed at the moment of a sighting.

### 🤖 Intelligent Tools
*   **AI Species Identification:** Attach a photo and get instant species suggestions powered by on-device machine learning (TensorFlow Lite).
*   **Smart Search:** Filter your sightings by migratory status, conservation status, or song type.
*   **Hotspots Map:** Visualize your sightings and discover high-activity areas using integrated Google Maps.

### 🏆 Gamification
*   **Life List:** Automatically builds a unique list of every species you've documented.
*   **Achievements:** Earn badges like "Early Bird" for sunrise sightings or "Conservation Ally" for documenting endangered species.

### ☁️ Data & Sync
*   **Google Drive Backup:** Sync your database to your personal Google Drive for total data security.
*   **Offline-First:** Works perfectly in remote forests or wetlands without an internet connection.

---

## 🛠️ Technical Stack

- **UI:** Jetpack Compose with Material 3 ("Forest Modern" custom theme)
- **Database:** Room (SQLite) with Flow support
- **Background Tasks:** WorkManager for periodic cloud syncing
- **Maps:** Google Maps SDK for Android & Maps Compose
- **AI/ML:** TensorFlow Lite for image-based bird identification
- **Networking:** Retrofit & Google API Client

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 11
- A Google Maps API Key

### Setup
1. **Clone the repository:**
   ```bash
   git clone https://github.com/namalaruthvik455-pixel/bird_track
   ```
2. **Add your Maps API Key:**
   Open `app/src/main/AndroidManifest.xml` and replace `YOUR_API_KEY_HERE` with your actual key.
3. **Build & Run:**
   Sync the project with Gradle files and run it on an emulator or physical device.

---

## 📖 How to Use

### 1. Starting an Expedition
On the home screen, tap the **+** (Add Trip) button. Enter the name of the park or region you are visiting. The app will record the start time and date.

### 2. Logging a Bird
Inside your active trip, tap **Log Sighting**. 
- Type the species name (or use the **Attach Photo** button to get an AI suggestion).
- The app will automatically fetch the current weather and GPS coordinates.
- Save your sighting to add it to your **Life List**.

### 3. Syncing Your Data
Tap the **Cloud Sync** icon in the top bar. Log in with your Google account to back up your sightings. Background sync will keep your records safe once a day thereafter.

### 4. Viewing Hotspots
Tap the **Map icon** in the top bar to see where you've recorded the most activity. This helps you identify trends and plan future birding trips.

---

## 🎨 Branding
BirdTrack Pro uses a custom **Forest Modern** palette:
- **Primary:** Brand Dark Blue (`#2E4C70`)
- **Accent:** Brand Orange (`#DF6C4C`)
- **Nature:** Brand Leaf Green (`#7DA04A`)

---
