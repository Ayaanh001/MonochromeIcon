# Monochrome Icon Maker

**Stop struggling with adaptive icon specs.** Monochrome Icon Maker is a specialized Android utility designed to save developers and designers hours of manual work. Instantly create perfect themed icons.

## ⚡ The Problem vs. The Solution

**The Struggle:**  
Creating an Android adaptive icon manually is a pain. You have to manage a **108dp x 108dp** total frame while ensuring your logo stays perfectly centered within the **72dp** safe zone. One wrong export and your icon gets clipped or looks off-center on the home screen.

**The Solution (1-Click):**  
With Monochrome Icon Maker, you just import your logo and we handle the rest. 
- **Already have a monochrome logo?** Just import it, turn off the filter, and export. It's instantly framed, padded, and sized to Google's exact 108dp/72dp specifications.
- **Need to create one?** Our smart algorithm extracts your foreground and generates the monochrome version for you.

## 🚀 Features

- **Automatic Framing & Sizing:** Instantly meet Google's official guidelines. We handle the 108dp base frame and 72dp safe zone automatically—no more manual padding math!
- **Smart Monochrome Conversion:** Automatically detects background colors and removes them, leaving a clean foreground for your icon.
- **Real-time Adaptive Previews:** See how your icon looks across different shapes:
  - Square
  - Rounded Square
  - Squircle (Android's default)
  - Circle
- **Material You Theming:** Preview your icons with various Material You color palettes in both Light and Dark modes.
- **Precision Adjustments:**
  - **Scale:** Fine-tune the size of your icon within the safe zone.
  - **Offset (X & Y):** Perfectly center or reposition your icon with pixel precision.
- **Multiple Export Formats:**
  - **PNG:** High-resolution raster export.
  - **SVG:** Vector export for maximum scalability and use in Android Studio as a Vector Drawable.

## 📸 How it Works

1. **Import:** Tap "Import Image" or share an image to the app.
2. **Refine:** 
   - Use the **"Monochrome" toggle** to convert a colored image.
   - **Toggle it OFF** if you're importing an existing monochrome asset to just use the framing features.
3. **Adjust:** Use the sliders to scale and position your icon. It's automatically locked into the 72dp safe zone.
4. **Preview:** Check it against all shapes and system themes.
5. **Export:** Save as PNG or SVG. It's now perfectly sized and ready to drop directly into your app's code.

## 🛠️ Tech Stack

- **UI:** Jetpack Compose with Material 3
- **Language:** Kotlin
- **Image Processing:** Custom bitmap manipulation for background removal and framing.
- **Architecture:** Modern Android development practices.

## 📦 Getting Started

### Prerequisites

- Android Studio Koala or newer
- Android SDK 36+
- A device running Android 8.0+

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/Ayaanh001/MonochromeIcon
   ```
2. Open the project in Android Studio.
3. Build and run the app on your device or emulator.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

*Made with ❤️ for Android Developers.*
