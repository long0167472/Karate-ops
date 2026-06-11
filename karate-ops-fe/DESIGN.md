# Design System: Karate Ops

## 1. Visual Theme and Atmosphere
A precise, kinetic operations interface for karate clubs and tournament crews. The public surface feels like a premium dojo control room: bright tatami canvas, charcoal typography, disciplined spacing, and measured motion that explains the workflow from club roster to live tatami.

Density: Daily App Balanced 5. Variance: Offset Asymmetric 7. Motion: Cinematic Choreography 7.

## 2. Color Palette and Roles
- **Tatami Canvas** (#F7F2E8) - Primary public and club workspace background.
- **Paper Surface** (#FFFDF7) - Elevated panels and dense operation surfaces.
- **Charcoal Ink** (#17191D) - Primary text and high contrast actions.
- **Muted Graphite** (#676157) - Body copy, helper text, and quiet metadata.
- **Rice Border** (rgba(50,43,32,0.12)) - Structural 1px borders.
- **Vermilion Command** (#A23A2A) - Single accent for CTAs, active states, focus rings, and motion highlights.

## 3. Typography Rules
- **Display:** Geist or Outfit - tight tracking, compact line height, controlled scale.
- **Body:** Geist or system sans fallback - relaxed leading, maximum 65 characters per line.
- **Mono:** JetBrains Mono or Cascadia Mono - all metrics, counters, IDs, and operational numbers.
- **Banned:** Inter as the primary design font, generic serif fonts, neon purple or blue gradients.

## 4. Component Stylings
- **Buttons:** One primary vermilion fill with light text. Secondary buttons use paper fill, charcoal text, and rice borders. Active state translates 1px down.
- **Cards:** Use cards only for repeated items, dashboard tiles, or focused work panels. Radius is 22 to 28px. No nested cards.
- **Inputs:** Label above input, clear focus ring in Vermilion Command, error text below.
- **Loaders:** Skeleton blocks matching the final panel shape. No circular spinners for product data.
- **Empty States:** Composed panels that tell the user what action unlocks the state.

## 5. Layout Principles
Public home uses a full-width asymmetric hero, a dense bento grid, a pinned story section, and a final action band. Product screens use restrained command hubs, sticky context where useful, and mobile-first single-column collapse below 768px. No horizontal scroll is acceptable.

## 6. Motion and Interaction
Use GSAP ScrollTrigger only on the public landing page, isolated from product UI. Use Framer Motion for product transitions already present in the app. Animate only transform and opacity. Respect `prefers-reduced-motion`.

## 7. Anti-Patterns
No emojis, no pure black, no neon glows, no purple default aesthetic, no three equal feature cards as the main layout, no section numbering labels, no scroll cue text, no generic placeholder names, no fake precise claims, and no duplicated dashboard routes for the same CLB job.
