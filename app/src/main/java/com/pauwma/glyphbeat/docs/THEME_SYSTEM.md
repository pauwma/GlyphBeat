# Theme System

## Overview

Modular animation theme system for the Glyph Matrix 25x25 LED display. Themes define frame-based animations with configurable parameters, per-frame timing, and state-specific visuals.

## Core Components

### AnimationTheme (`themes/base/AnimationTheme.kt`)
Abstract base class. All themes extend this.

**Required overrides:**
- `getFrameCount(): Int` — number of animation frames (4–60)
- `generateFrame(frameIndex: Int): IntArray` — pixel data for a specific frame
- `getThemeName(): String`

**Optional overrides:**
- `getAnimationSpeed(): Long` — ms per frame
- `getBrightness(): Int` — theme-specific brightness
- `getDescription(): String`

**Helpers:** `validateFrameIndex()`, `createEmptyFrame()`

### GlyphMatrixRenderer (`core/GlyphMatrixRenderer.kt`)
Drawing utilities and animation generators for the 25x25 matrix.

**Drawing:** `drawLine()`, `drawCircle()`, `drawDot()`, `fillGrid()`
**Generators:** `createRotatingLineFrames()`, `createPulseFrames()`, `createWaveFrames()`
**Utilities:** pixel format conversions, matrix shape calculations

### Existing Themes (`themes/animation/`)

| Theme | Description |
|---|---|
| `VinylTheme` | Spinning line simulating a vinyl record |
| `PulseTheme` | Expanding/contracting circle pulse |
| `WaveTheme` | Sine wave flowing across display |
| `WaveformTheme` | Audio waveform visualization |
| `PulseVisualizerTheme` | Pulse synced to audio |
| `MinimalTheme` | Minimal dot animation |
| `ShapeTheme` | Geometric shape morphing |
| `DancingDuckTheme` | Animated duck character |
| `CoverArtTheme` | Album artwork display (uses full 25x25 grid, no circular mask) |

### Track Control Themes (`themes/trackcontrol/`)
`MinimalArrowTheme`, `PulseRippleTheme` — used by Next/Previous track services.
Managed by `TrackControlThemeManager` and rendered via `TrackControlThemeRenderer`.

## Creating a Custom Theme

```kotlin
class CustomTheme(private val frameCount: Int = 10) : AnimationTheme() {
    override fun getFrameCount(): Int = frameCount
    
    override fun generateFrame(frameIndex: Int): IntArray {
        validateFrameIndex(frameIndex)
        val grid = createEmptyFrame()
        // Custom drawing logic using GlyphMatrixRenderer utils
        return grid
    }
    
    override fun getThemeName(): String = "Custom"
}
```

**Registration:** Add to `availableThemes` list in `MediaPlayerToyService`.

## Advanced Features

### Per-Frame Durations
Themes can specify individual display times per frame (50–2000ms) for variable-speed animations: rhythm patterns, dramatic pauses, quick flashes. Falls back to global `getAnimationSpeed()` if not specified.

### State-Specific Frames
Themes can provide different visuals per player state:

| State | Behavior |
|---|---|
| `PLAYING` | Normal animation loop |
| `PAUSED` | Shows paused frame or continues in demo mode |
| `OFFLINE` | Shows offline frame or demo mode |
| `LOADING` | Shows loading/buffering frame |
| `ERROR` | Shows error frame or fallback |

### Theme Metadata
Rich metadata system: author, version, category, tags, creation date, complexity level, loop mode, preview frame, audio reactive flag, fade transitions, compatibility version, frame format.

## Theme Preview System

### Preview Types
1. **Matrix-Shaped Preview** (default) — `GlyphMatrixPreview` component
   - Renders only active pixels within circular Glyph Matrix shape
   - Scales 25x25 matrix to preview size (typically 120dp)
   - Circular masking using actual Glyph shape definition
   - Solid grayscale rendering: 0-brightness = black (visible against #121212 surface), lit pixels scale to white
   - All matrix positions always drawn (GlyphMuseum style)

2. **Grid Preview** (exception, e.g. `CoverArtTheme`)
   - Full 25x25 grid without circular masking
   - For themes displaying rectangular content like album artwork

### Preview Behavior
- **Selected theme**: real-time animation at reduced scale
- **Unselected themes**: static first frame only (performance)
- **CoverArtTheme**: preview updates when playing track changes

### Visual Style
Theme cards use NothingTypeFont (`ntype82regular`) for titles with PostPreviewCard-style colors.

## User Interactions

| Input | Action |
|---|---|
| Short press | Switch to next theme |
| Long press | Toggle play/pause (media control) or toggle demo mode |
| Shake gesture | Skip to next track (configurable) |

### Shake Control
- Three sensitivity levels: Low, Medium, High (settings slider)
- 2-second cooldown prevents accidental multiple skips
- Only active when `MediaPlayerToyService` is running
- Toggle + sensitivity slider in main settings screen

## Performance Notes
- Themes pre-generate frames during initialization
- Frame count trades smoothness vs. performance
- Only selected theme animates in preview (others show static frame)
- Use `validateFrameIndex()` and `GlyphMatrixRenderer` utils for consistent rendering
