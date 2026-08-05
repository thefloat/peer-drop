---
name: Electric Dark
colors:
  surface: '#131313'
  surface-dim: '#131313'
  surface-bright: '#393939'
  surface-container-lowest: '#0e0e0e'
  surface-container-low: '#1b1b1c'
  surface-container: '#202020'
  surface-container-high: '#2a2a2a'
  surface-container-highest: '#353535'
  on-surface: '#e5e2e1'
  on-surface-variant: '#c1c6d7'
  inverse-surface: '#e5e2e1'
  inverse-on-surface: '#303030'
  outline: '#8b90a0'
  outline-variant: '#414755'
  surface-tint: '#adc6ff'
  primary: '#adc6ff'
  on-primary: '#002e69'
  primary-container: '#4b8eff'
  on-primary-container: '#00285c'
  inverse-primary: '#005bc1'
  secondary: '#c8c6c6'
  on-secondary: '#303030'
  secondary-container: '#474747'
  on-secondary-container: '#b6b5b4'
  tertiary: '#ffb595'
  on-tertiary: '#571e00'
  tertiary-container: '#ef6719'
  on-tertiary-container: '#4c1a00'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#d8e2ff'
  primary-fixed-dim: '#adc6ff'
  on-primary-fixed: '#001a41'
  on-primary-fixed-variant: '#004493'
  secondary-fixed: '#e4e2e1'
  secondary-fixed-dim: '#c8c6c6'
  on-secondary-fixed: '#1b1c1c'
  on-secondary-fixed-variant: '#474747'
  tertiary-fixed: '#ffdbcc'
  tertiary-fixed-dim: '#ffb595'
  on-tertiary-fixed: '#351000'
  on-tertiary-fixed-variant: '#7c2e00'
  background: '#131313'
  on-background: '#e5e2e1'
  surface-variant: '#353535'
typography:
  headline-lg:
    fontFamily: Hanken Grotesk
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Hanken Grotesk
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Hanken Grotesk
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Hanken Grotesk
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Geist
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.05em
  code-sm:
    fontFamily: Geist
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  gutter: 16px
  margin-mobile: 16px
  margin-desktop: 24px
---

## Brand & Style

This design system is engineered for high-performance communication, blending a sleek, immersive dark-mode aesthetic with high-energy accents. The personality is focused, sophisticated, and technologically advanced, designed to minimize eye strain during long-form digital interaction while maintaining a sense of momentum.

The design style is **Corporate Modern with Glassmorphism influences**. It utilizes deep, layered grays to create a sense of infinite depth, punctuated by vibrant "Electric Blue" highlights that signify action and urgency. The interface prioritizes clarity through high-contrast typography and subtle luminosity, ensuring that messages and critical UI elements feel elevated above the background.

## Colors

The palette is anchored in a three-tier grayscale system to establish hierarchy without relying on heavy borders. 

- **Primary (#007AFF):** Used for primary actions, active states, and focus indicators. This "Electric Blue" should feel luminous against the dark background.
- **Surface Tiers:** `#1E1E1E` is the standard surface for sidebars and input areas, while `#2D2D2D` is used for cards, message bubbles, and elevated components.
- **Contrast:** Text is kept at near-pure white for maximum legibility, while secondary metadata uses mid-tone grays to recede.

## Typography

The typography system leverages **Hanken Grotesk** for its sharp, contemporary geometry and exceptional readability in digital interfaces. It provides a human yet technical feel. For technical metadata, timestamps, and monospaced content, **Geist** is used to reinforce the "developer-grade" precision of the application.

- **Headlines:** Bold and tight-set to command attention.
- **Body:** Generous line-height to ensure long chat threads remain scannable.
- **Labels:** Uppercase application for Geist font labels is recommended for architectural navigation elements.

## Layout & Spacing

The layout follows a **Fluid Grid** model with a preference for high-density information display. 

- **Sidebar/Navigation:** Fixed width (280px - 320px) on desktop, transitioning to a full-screen drawer on mobile.
- **Chat Canvas:** Fluid width with a centered max-width of 900px for optimal line length in message bubbles.
- **Spacing Rhythm:** Based on a 4px baseline grid. Use 16px (md) for standard padding within cards and message clusters, and 8px (sm) for internal element grouping (e.g., avatar to username).

## Elevation & Depth

Depth is conveyed through **Tonal Layering** and **Subtle Glows** rather than traditional heavy shadows.

- **Level 0 (Background):** `#121212` – The base canvas.
- **Level 1 (Navigation/Sidebars):** `#1E1E1E` – Integrated into the base, no shadow.
- **Level 2 (Cards/Bubbles):** `#2D2D2D` – Uses a 1px inner border (`rgba(255,255,255,0.05)`) to define edges against the dark background.
- **Active State Glow:** Primary buttons and active chat indicators feature a soft `0px 4px 20px rgba(0, 122, 255, 0.25)` outer glow to simulate a "lit" appearance.
- **Overlays:** Modals and menus use a `Backdrop Blur (12px)` with a semi-transparent surface of `#2D2D2D` at 80% opacity.

## Shapes

The shape language is defined by a consistent **12px radius (rounded-md)**. This strikes a balance between the friendliness of a social app and the precision of a productivity tool.

- **Message Bubbles:** 12px for the outer corners, with the tail corner reduced to 4px to indicate directionality.
- **Buttons & Inputs:** Consistent 12px rounding for a unified interactive language.
- **Avatars:** Circular (Full rounded) to provide a soft organic contrast to the structured rectangular grid.

## Components

- **Buttons:** Primary buttons use the Electric Blue fill with white text. Ghost buttons use a 1px border of the primary color with a subtle hover glow.
- **Message Bubbles:** 
    - *Incoming:* `#2D2D2D` background with high-contrast white text.
    - *Outgoing:* Primary Blue background or a deep gray with a primary blue left-accent border.
- **Input Fields:** `#1E1E1E` fill with a `#2D2D2D` border. On focus, the border transitions to Primary Blue with a subtle 4px outer glow.
- **Chips/Status:** Use a "pill" shape (fully rounded) with a low-opacity background of the status color (e.g., green for online) and a high-saturated dot.
- **Lists:** Clean separation using vertical spacing rather than dividers. Hover states should use a subtle lightening of the background to `#333333`.
- **Scrollbars:** Minimalist, thin width (4px), using `#2D2D2D` for the track and `#444444` for the thumb.