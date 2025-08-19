# Glyph Beat 🎵

> Transform your Nothing Phone's Glyph Matrix into a dynamic music visualizer with stunning LED animations

[![GitHub Stars](https://img.shields.io/github/stars/pauwma/GlyphBeat?style=for-the-badge)](https://github.com/pauwma/GlyphBeat)
[![Latest Release](https://img.shields.io/github/v/release/pauwma/GlyphBeat?style=for-the-badge)](https://github.com/pauwma/GlyphBeat/releases)
[![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)](LICENSE)
[![Privacy Policy](https://img.shields.io/badge/Privacy-Policy-blue?style=for-the-badge)](https://pauwma.github.io/GlyphBeat/privacy.html)

## 🌟 Overview

**Glyph Beat** brings your Nothing Phone's Glyph interface to life with beautiful, music-reactive animations. Watch as your device's LED matrix dances to the rhythm of your favorite songs, creating a unique visual experience that's perfectly synchronized with your music.

### 🎯 Key Features

- 🎨 **10+ Unique Animation Themes** - From minimalist pulses to complex waveforms
- 🎵 **Real-Time Music Sync** - Animations react instantly to your music's rhythm and beats
- 🎮 **Glyph Toys Integration** - Quick access widgets for track control
- 🌈 **Customizable Settings** - Adjust brightness, timeout, and animation parameters
- 🚀 **Lightweight & Efficient** - Optimized for minimal battery impact
- 🔒 **Privacy-First** - No data collection, no ads, no tracking

## 📱 Animation Themes

### Core Animations
| Theme | Description |
|-------|------------|
| **🎵 Vinyl** | Classic spinning record animation that rotates with your music |
| **💿 Cover Art** | Displays album artwork in a pixelated matrix style |
| **🌊 Wave** | Smooth waveform that flows across the matrix |
| **📊 Waveform** | Audio spectrum visualization showing frequency bands |
| **✨ Pulse** | Rhythmic pulsing dots that beat with the bass |
| **🎯 Minimal** | Clean, simple animation for a subtle effect |
| **🦆 Dancing Duck** | A playful duck that dances to your tunes |
| **👾 Glyphy** | Animated character that grooves with the music |
| **💥 Pulse Visualizer** | Dynamic equalizer bars that respond to audio |

### Track Control Themes
| Theme | Description |
|-------|------------|
| **⏭️ Minimal Arrow** | Simple arrow indicators for track navigation |

## 🚀 Installation

### Requirements
- Nothing Phone (3) with Glyph Matrix support
- Android 14 or higher
- Notification Access permission

### Steps

1. **Download the APK**
   - Get the latest release from the [Releases page](https://github.com/pauwma/GlyphBeat/releases)

2. **Install the App**
   ```
   1. Open the downloaded APK
   2. Allow installation from unknown sources if prompted
   3. Complete the installation
   ```

3. **Grant Permissions**
   - Open Glyph Beat
   - Follow the tutorial to grant Notification Access
   - This allows the app to read media information

4. **Enable in Glyph Settings**
   ```
   Settings → Glyph Interface → Active Toys → Enable Glyph Beat
   ```

## 📖 Usage Guide

### Getting Started
1. **Launch Glyph Beat** and complete the initial setup tutorial
2. **Select your preferred theme** from the main screen
3. **Play music** from any app (Spotify, YouTube Music, etc.)
4. **Watch your Glyph Matrix** come alive with animations!

### Customization Options

#### Theme Settings
- **Brightness**: Adjust LED intensity (0-100%)
- **Timeout**: Set how long animations continue after music stops
- **Theme-specific parameters**: Each theme has unique customization options

#### Glyph Toys
Enable quick-access widgets for:
- Previous track control
- Next track control
- Media player toggle

### Tips & Tricks
- 🎧 Works with any media app that shows notifications
- 🔋 Enable "Battery Saver Mode" in settings for reduced power consumption
- 🎨 Try different themes for different music genres
- 📱 Shake your device to switch themes quickly (when enabled)

## 🔧 Technical Details

### Built With
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Repository pattern
- **Key Libraries**:
  - Glyph SDK for matrix control
  - MediaSession API for music detection
  - Coroutines for async operations
  - Material3 for modern UI components

### How It Works
1. **Media Detection**: Monitors system notifications for active media sessions
2. **Audio Analysis**: Processes audio metadata and playback state
3. **Animation Rendering**: Translates music data into matrix patterns
4. **Glyph Control**: Sends optimized commands to the LED matrix

## 🔐 Privacy & Security

Glyph Beat is designed with privacy as a core principle:

- ✅ **No Data Collection** - We don't collect any personal information
- ✅ **No Internet Access** - Except for optional donation links
- ✅ **Local Processing** - All animations are generated on-device
- ✅ **Open Source** - Full transparency in our code

[Read our full Privacy Policy](https://pauwma.github.io/GlyphBeat/privacy.html)

## 🤝 Contributing

We welcome contributions from the community!

### How to Contribute
1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Reporting Issues
- 🐛 [Report bugs](https://github.com/pauwma/GlyphBeat/issues/new?labels=bug)
- 💡 [Request features](https://github.com/pauwma/GlyphBeat/issues/new?labels=enhancement)
- 💬 [Ask questions](https://github.com/pauwma/GlyphBeat/discussions)

## 💖 Support Development

If you enjoy Glyph Beat, consider supporting its development:

<div align="center">

[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-FFDD00?style=for-the-badge&logo=buy-me-a-coffee&logoColor=black)](https://buymeacoffee.com/pauwma)
[![PayPal](https://img.shields.io/badge/PayPal-00457C?style=for-the-badge&logo=paypal&logoColor=white)](https://paypal.me/pauwma)
[![GitHub Sponsors](https://img.shields.io/badge/GitHub%20Sponsors-EA4AAA?style=for-the-badge&logo=github-sponsors&logoColor=white)](https://github.com/sponsors/pauwma)

</div>

## 📋 Compatibility

### Supported Devices
- ✅ Nothing Phone (3) - Full support with 25-pixel matrix
- ⚠️ Nothing Phone (2) - Limited support (different matrix layout)
- ❌ Nothing Phone (1) - Not supported (no matrix hardware)

### Android Requirements
- **Minimum**: Android 14 (API 34)
- **Recommended**: Latest Android version for best performance

## 🛠️ Troubleshooting

### Common Issues

| Problem | Solution |
|---------|----------|
| **Animations not showing** | Ensure Glyph Beat is enabled in Glyph Interface settings |
| **No music detection** | Check if Notification Access is granted in system settings |
| **Animations lag** | Try reducing brightness or using simpler themes |
| **Battery drain** | Enable Battery Saver mode in app settings |

### Need More Help?
- 📖 Check our [Wiki](https://github.com/pauwma/GlyphBeat/wiki)
- 💬 Join the [Discussions](https://github.com/pauwma/GlyphBeat/discussions)
- 📧 Contact: [contact+glyphbeat@pauwma.com](mailto:contact+glyphbeat@pauwma.com)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Nothing Technology** - For creating innovative hardware that inspires creativity
- **Nothing Community** - For the amazing support and feedback
- **Contributors** - Everyone who has helped improve Glyph Beat
- **Open Source Libraries** - The amazing tools that make this possible

---

<div align="center">

**Made with 🤍 for the Nothing Community**

[Website](https://pauwma.github.io/GlyphBeat) • [Privacy Policy](https://pauwma.github.io/GlyphBeat/privacy.html) • [Releases](https://github.com/pauwma/GlyphBeat/releases)

</div>