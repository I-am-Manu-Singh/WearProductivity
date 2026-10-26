---
name: Tonal Continuum
colors:
  surface: '#111318'
  surface-dim: '#111318'
  surface-bright: '#37393f'
  surface-container-lowest: '#0c0e13'
  surface-container-low: '#1a1b21'
  surface-container: '#1e2025'
  surface-container-high: '#282a2f'
  surface-container-highest: '#33353a'
  on-surface: '#e2e2e9'
  on-surface-variant: '#c4c6d4'
  inverse-surface: '#e2e2e9'
  inverse-on-surface: '#2e3036'
  outline: '#8e909e'
  outline-variant: '#434652'
  surface-tint: '#b3c5ff'
  primary: '#b3c5ff'
  on-primary: '#002b75'
  primary-container: '#769bff'
  on-primary-container: '#003080'
  inverse-primary: '#3059b9'
  secondary: '#62dbb2'
  on-secondary: '#003829'
  secondary-container: '#18a47e'
  on-secondary-container: '#003123'
  tertiary: '#ffb59e'
  on-tertiary: '#5e1700'
  tertiary-container: '#f17f5b'
  on-tertiary-container: '#681a00'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#dae1ff'
  primary-fixed-dim: '#b3c5ff'
  on-primary-fixed: '#001849'
  on-primary-fixed-variant: '#0940a0'
  secondary-fixed: '#80f8cd'
  secondary-fixed-dim: '#62dbb2'
  on-secondary-fixed: '#002117'
  on-secondary-fixed-variant: '#00513d'
  tertiary-fixed: '#ffdbd0'
  tertiary-fixed-dim: '#ffb59e'
  on-tertiary-fixed: '#3a0b00'
  on-tertiary-fixed-variant: '#7f2a0d'
  background: '#111318'
  on-background: '#e2e2e9'
  surface-variant: '#33353a'
typography:
  display-lg:
    fontFamily: Roboto Flex
    fontSize: 56px
    fontWeight: '700'
    lineHeight: 64px
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Roboto Flex
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 48px
    letterSpacing: -0.02em
  display-md:
    fontFamily: Roboto Flex
    fontSize: 44px
    fontWeight: '700'
    lineHeight: 52px
    letterSpacing: -0.01em
  display-md-mobile:
    fontFamily: Roboto Flex
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg:
    fontFamily: Roboto Flex
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Roboto Flex
    fontSize: 26px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: 0em
  headline-sm:
    fontFamily: Roboto Flex
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: 0em
  title-lg:
    fontFamily: Roboto Flex
    fontSize: 20px
    fontWeight: '500'
    lineHeight: 26px
    letterSpacing: 0em
  title-md:
    fontFamily: Roboto Flex
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: 0.01em
  title-sm:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  body-lg:
    fontFamily: Roboto Flex
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.02em
  body-md:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.02em
  body-sm:
    fontFamily: Roboto Flex
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0.03em
  label-lg:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.03em
  label-md:
    fontFamily: Roboto Flex
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.04em
  label-sm:
    fontFamily: Roboto Flex
    fontSize: 10px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.06em
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-sm: 0.5rem
  gutter-lg: 1.5rem
  margin: 1rem
  margin-sm: 0.75rem
  margin-lg: 2rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2.25rem
---

## Brand & Style

This design system embodies the Google Material Design 3 Expressive (M3 Expressive) philosophy, engineered specifically for continuous bi-directional ambient synchronization between wrist-worn hardware (Wear OS) and handheld devices (Android).

### Brand Personality & Philosophy
- **Fluid Continuity:** Interfaces feel like an unbroken flow of information traversing physical thresholds—from micro-glances on OLED wrist displays to immersive management on handheld canvases.
- **Vibrant Optimism & Warmth:** Stepping beyond utilitarian tracking into emotive lifestyle orchestration. Health metrics, active routines, and daily rituals feel vibrant, alive, and rewarding rather than punitive.
- **Adaptive Tactility:** Soft, pill-shaped physical metaphors, generous tap surfaces, and deliberate tonal shifts deliver immediate tactile confidence across touch and ambient haptic cues.

### Design Movement & Aesthetic Expression
The visual language merges **M3 Expressive Tonal Architecture** with **Tactile Organic Modernism**:
- Deep, luminous tonal containers replace rigid outlines and harsh drop shadows.
- Expressive, variable-grade typography balances energetic glanceable telemetry with structured narrative clarity.
- Micro-luminous accents and organic pill shapes mimic hardware contours across physical screens.

## Colors

The palette is anchored in an expressive dark baseline designed specifically for deep OLED battery efficiency on Wear OS, paired with rich dynamic tonal surfaces on mobile devices.

### Palette Architecture
- **Primary (`#769BFF` - Electric Cobalt / Periwinkle 80):** Represents synchronization, core system actions, connectivity states, and primary telemetry.
  - *Primary Container:* `#1D3B8A` (Deep Cobalt Night) with text on container `#DCE2FF`.
- **Secondary (`#62DBB2` - Radiant Mint / Sage 80):** Signifies biometric balance, completed rituals, physical readiness, and battery health.
  - *Secondary Container:* `#00513B` (Verdant Depth) with text on container `#85F8CD`.
- **Tertiary (`#FF8A65` - Warm Coral / Sunset 80):** Drives intermittent fasting countdowns, heart rate zones, streak incentives, and active routine timers.
  - *Tertiary Container:* `#601400` (Ember Core) with text on container `#FFDBCF`.
- **Neutral Base (`#111318` - Obsidian Surface):** Deep chromatic neutral preventing black-crush while ensuring high-contrast legibility.

### Tonal Surface Tiers
- **Surface Dim:** `#0E1015` (Receded Canvas)
- **Surface Standard:** `#111318` (Application Window)
- **Surface Container Lowest:** `#161820` (Sub-level grouping)
- **Surface Container Low:** `#1A1D26` (Standard flat card)
- **Surface Container:** `#20232E` (Interactive card default)
- **Surface Container High:** `#272A36` (Glanceable widget / Watch complication container)
- **Surface Container Highest:** `#313543` (Floating sheet, active control, dialogue)

### Functional & Glancing Roles
- **Active Fasting Arc:** Radial gradient mapping from `#FF8A65` (Warm Coral) through `#FFA877` to indicate phase shifts.
- **Wearable-to-Phone Sync Pulse:** Breathing glow ring combining `#769BFF` and `#62DBB2` for zero-latency peer transfer acknowledgment.

## Typography

This system utilizes `Roboto Flex` across all viewports to unlock variable axis attributes (`wght`, `wdth`, `opsz`) tailored for rapid glanceability on round smartwatch screens and rich editorial structure on handheld devices.

### Structural Typographic Rules
- **Biometric Digits & Glanceable Metrics:** Set numeric displays (heart rate, fasting remaining, cadence, battery percentages) using variable bold weights (`wght: 700`) with proportional tabular figures to prevent jitter during real-time data streaming.
- **Wear OS Display Limits:** Watch faces and companion tiles restrict text hierarchy strictly to `headline-sm`, `title-sm`, and `label-sm` to maintain instant spatial legibility at arm's length.
- **Title and Label Uppercase Constraint:** Only `label-sm` leverages full uppercase capitalization (with `0.06em` letter spacing) for category tags, battery percentages, and connectivity chips (e.g., `SYNCED`, `CHARGING`, `FASTING`).

## Layout & Spacing

Layouts follow an adaptable multi-device rhythm that flexes between wrist constraints and large multi-pane handheld screens.

### Grid & Composition Models
- **Handheld Companion (Phone):** 
  - Standard 4-column layout (`margin: 1rem`, `gutter: 1rem`) expanding to 8-column layout on foldable or tablet devices (`margin: 2rem`, `gutter: 1.5rem`).
  - Cards adopt fluid horizontal stacking with explicit vertical rhythm governed by `space-md` (`1rem`) and `space-lg` (`1.5rem`).
- **Wear OS Companion Screens:**
  - Radial inset bounding model: strict `16px` (`1rem`) horizontal screen padding conforming to circular viewport dead zones.
  - Single-column vertical stack with snap-scroll anchoring centered on cards.

### Spatial Flow & Dense Information Grouping
- Use `space-xs` (4px) strictly between co-dependent elements (e.g., metric icon to scalar reading).
- Use `space-sm` (8px) for badge-to-card inner paddings and horizontal chip clusters.
- Use `space-md` (16px) for interior card padding across companion summary items.
- Use `space-xl` (36px) to partition major routine sections (e.g., Fasting Trackers from Media Controls).

## Elevation & Depth

In alignment with Material 3 Expressive, elevation is primarily achieved via **tonal color stepping** rather than heavy drop shadows. Surfaces rise closer to the user by shifting upward through calibrated tonal container values.

### Tonal Elevation Architecture
- **Level 0 (Base Canvas):** `#111318` — Host app background. Zero shadow.
- **Level 1 (Card Baseline):** `#1A1D26` — Default metric containers, inactive routine summaries.
- **Level 2 (Interactive Floating Cards):** `#20232E` — Hovered, pressed, or interactive routine widgets.
- **Level 3 (Modal Sheets & Watch Inset Tiles):** `#272A36` — Bottom sheets, quick action dialogs, ambient watch card surfaces.
- **Level 4 (Active Luminous Components):** `#313543` with focused light accents.

### Luminous Ambient Glows
For critical real-time activities (such as active workout syncing, live fasting windows, and active audio streaming), physical depth is supplemented by low-opacity, chromatic ambient diffusion:
- **Active Fasting Window:** Subtle ambient glow under the widget: `0 8px 32px -4px rgba(255, 138, 101, 0.22)`.
- **Peer Device Syncing:** Subtle dual-gradient backlight: `0 4px 24px -2px rgba(118, 155, 255, 0.18)`.
- **Sensory Boundaries:** Outlines are reserved for inactive states using a low-contrast border (`rgba(255, 255, 255, 0.08)`) with zero shadow.

## Shapes

The system relies on a pill-shaped (`roundedness: 3`) design language that mirrors physical pebble smartwatch hardware, watch bezels, and tactile Android dynamic touch targets.

### Shape Hierarchy & Curvature
- **Full Pill (`9999px` / `rounded-full`):** Buttons, interactive filter chips, status badges, connectivity pills, and scrub bars.
- **Extra Large Containers (`3rem` / `48px`):** Top-level hero dashboard modules, such as active intermittent fasting rings and media playback blocks.
- **Large Surfaces (`2rem` / `32px`):** Secondary cards, daily stat summary modules, and wrist glance notifications.
- **Small Componentry (`1rem` / `16px`):** Inner metric sub-cards, nested data chips, and micro-dialogues.
- **Asymmetric Expressive Morphing:** Active routine widgets may dynamically morph their corner radii (e.g., transition from `32px` uniform to `48px 16px 48px 16px`) upon state transitions (such as triggering workout sessions or fasting milestones) to signal physical activation.

## Components

### Buttons & Interactive Controls
- **Primary Action Buttons:** Full-pill shape, solid `#769BFF` fill with `#00287D` text. Minimum tap target is 48px on handheld devices and 52px on Wear OS.
- **Tonal Assist Buttons:** Full-pill shape, `#272A36` surface container high with `#DCE2FF` primary text.
- **Haptic Press State:** Scaled downward by 3% (`scale(0.97)`) upon touch down, returning smoothly with standard decelerated curves.

### Interconnected Wearable-to-Phone Status Indicators
- **Connectivity Indicator:** An expressive double-pill element. Left chamber displays smartwatch icon and battery percentage; right chamber displays phone state with a pulsing connector dot (`#62DBB2` for active peer-to-peer sync, `#769BFF` for Wi-Fi/Cloud bridge).
- **Sync Pulse Badge:** A micro-badge with an animated 3-dot or dual-ring orbital spinner that expands when health telemetry sync is in progress.

### Intermittent Fasting Rings
- **Dual-Surface Gauge:** Outer stationary track set to `#1A1D26`; filled progress indicator using a continuous gradient starting at `#FF8A65` and concluding at `#FFA877` with rounded end-caps.
- **Glance Center Metrics:** Current elapsed time in `headline-md` tabular bold, flanked by target phase pill (`label-sm`: "AUTOPHAGY" / "FAT BURNING") backed by a 20% opacity tertiary container tint.

### Media Playback Widget
- **Tactile Waveform Card:** Enclosed in a 32px rounded container (`#20232E`).
- **Dynamic Color Bleed:** Blurred ambient background extracted from album artwork at 15% opacity over the base neutral.
- **Integrated Watch Handoff:** Compact icon button placed in the top right to instantly redirect media streaming output between watch speaker, Bluetooth earbuds, and smartphone.

### Expressive Status Chips
- Height: 32px (phone), 36px (Wear OS).
- Background: `#272A36` in resting state; switches to vibrant tonal containers (`#00513B` for mint goals reached; `#601400` for active timers).
- Icon placement: Centered left with 18px vector glyph; text in `label-md`.

### Cards & Telemetry Lists
- **Cards:** Background `#1A1D26`, padding `1.25rem`, no border. Corner radius: `2rem`.
- **List Dividers:** Replaced entirely by spacing (`space-sm`) and distinct sub-container tonal fills (`#20232E`) rather than hairline rules.
- **Selection Checks & Radios:** Pill-shaped toggle switches featuring an animated oversized thumb that stretches during drag interactions, colored in `#62DBB2` when active.