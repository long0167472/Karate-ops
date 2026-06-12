# KarateOps Homepage - Enhanced Design Spec (Smooth & Beautiful)

**Focus:** Modern, smooth, polished UI with subtle animations & beautiful interactions.

---

## 🎨 Visual Polish System

### Elevation & Shadows

```css
/* Depth hierarchy using shadows */
shadow-none: 0 0 0 rgba(0,0,0,0);          /* Flat elements */
shadow-sm:   0 1px 2px rgba(0,0,0,0.05);   /* Subtle lift */
shadow-md:   0 4px 6px rgba(0,0,0,0.07),   /* Cards, inputs */
             0 2px 4px rgba(0,0,0,0.05);
shadow-lg:   0 10px 15px rgba(0,0,0,0.1),  /* Modals, dropdowns */
             0 4px 6px rgba(0,0,0,0.05);
shadow-xl:   0 20px 25px rgba(0,0,0,0.1),  /* Floating actions */
             0 10px 10px rgba(0,0,0,0.04);
```

**Usage:**
- **Default state:** shadow-none (flat, clean)
- **Hover:** shadow-md (lift effect)
- **Active/Focus:** shadow-lg (emphasis)
- **Modal/Overlay:** shadow-xl + backdrop blur

### Transitions & Easing

```css
/* Global transition curve (smooth, not snappy) */
transition-fast:   all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
transition-base:   all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
transition-slow:   all 0.5s cubic-bezier(0.16, 1, 0.3, 1);

/* Easing functions */
ease-out:  cubic-bezier(0.16, 1, 0.3, 1);    /* Object leaving screen */
ease-in:   cubic-bezier(0.7, 0, 0.84, 0);    /* Object entering screen */
ease-in-out: cubic-bezier(0.4, 0, 0.2, 1);   /* Smooth back-and-forth */
```

**Why this easing?** Feels premium because it has slight overshoot (1.0 > 1 in curve), mimicking real-world motion.

---

## 🎭 Component Styles

### Header

```css
.header {
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border);
  backdrop-filter: blur(4px);              /* Subtle glass effect */
  position: sticky;
  top: 0;
  z-index: 100;
  
  /* Smooth scroll behavior */
  transition: box-shadow 0.3s ease-out;
}

.header:not(:at-top) {
  box-shadow: 0 4px 6px rgba(0,0,0,0.07);
}

.logo {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, #3366ff, #ff6b35);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.nav-item {
  position: relative;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-secondary);
  transition: color 0.3s ease-out;
}

.nav-item:hover {
  color: var(--text-primary);
}

.nav-item.active {
  color: var(--primary-brand);
}

.nav-item.active::after {
  content: '';
  position: absolute;
  bottom: -12px;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--primary-brand);
  border-radius: 1.5px;
  animation: slideIn 0.4s ease-out;
}

@keyframes slideIn {
  from {
    width: 0;
    left: 50%;
  }
  to {
    width: 100%;
    left: 0;
  }
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3366ff, #ff6b35);
  cursor: pointer;
  transition: all 0.3s ease-out;
  box-shadow: 0 2px 4px rgba(51, 102, 255, 0.2);
}

.avatar:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(51, 102, 255, 0.3);
}
```

---

### Tabs

```css
.tabs {
  display: flex;
  gap: 32px;
  border-bottom: 1px solid var(--border);
  padding: 0 24px;
  background: var(--bg-primary);
}

.tab {
  padding: 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-secondary);
  cursor: pointer;
  position: relative;
  transition: color 0.3s ease-out;
}

.tab:hover {
  color: var(--text-primary);
}

.tab.active {
  color: var(--primary-brand);
}

.tab.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--primary-brand);
  border-radius: 1.5px 1.5px 0 0;
  animation: tabSlide 0.4s ease-out;
}

@keyframes tabSlide {
  from {
    transform: scaleX(0);
    transform-origin: left;
  }
  to {
    transform: scaleX(1);
    transform-origin: left;
  }
}
```

---

### Financial Cards

```css
.card {
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 24px;
  
  /* Subtle gradient overlay for depth */
  position: relative;
  overflow: hidden;
  
  transition: all 0.3s ease-out;
}

.card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.05) 0%,
    rgba(255, 255, 255, 0) 100%
  );
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.3s ease-out;
}

.card:hover {
  transform: translateY(-4px);
  border-color: var(--primary-brand);
  box-shadow: 0 10px 15px rgba(51, 102, 255, 0.1);
}

.card:hover::before {
  opacity: 1;
}

.card-label {
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-secondary);
  margin-bottom: 12px;
  opacity: 0.7;
  transition: opacity 0.3s ease-out;
}

.card:hover .card-label {
  opacity: 1;
}

.card-value {
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--primary-brand), #ff6b35);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-family: 'Geist Mono', 'JetBrains Mono', monospace;
  margin: 12px 0;
  transition: transform 0.3s ease-out;
}

.card:hover .card-value {
  transform: scale(1.02);
}

.card-meta {
  font-size: 12px;
  color: var(--success-green);
  font-weight: 500;
}
```

---

### Calendar

```css
.calendar {
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: 16px;  /* Slightly rounder for softer feel */
  padding: 24px;
  margin-bottom: 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease-out;
}

.calendar:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.calendar-nav button {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--bg-secondary);
  color: var(--text-primary);
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  
  transition: all 0.2s ease-out;
  display: flex;
  align-items: center;
  justify-content: center;
}

.calendar-nav button:hover {
  background: var(--primary-brand);
  color: white;
  border-color: var(--primary-brand);
  transform: scale(1.08);
  box-shadow: 0 4px 8px rgba(51, 102, 255, 0.2);
}

.calendar-nav button:active {
  transform: scale(0.96);
}

.day {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  background: var(--bg-secondary);
  border: 1px solid transparent;
  
  transition: all 0.2s ease-out;
  position: relative;
  overflow: hidden;
}

.day::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--primary-brand);
  opacity: 0;
  z-index: -1;
  transition: opacity 0.2s ease-out;
}

.day:hover:not(.event) {
  background: var(--bg-primary);
  border-color: var(--primary-brand);
  transform: scale(1.05);
  box-shadow: 0 2px 6px rgba(51, 102, 255, 0.15);
}

.day.event {
  background: linear-gradient(135deg, var(--primary-brand), #1e52cc);
  color: white;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(51, 102, 255, 0.3);
  border: none;
}

.day.event:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 20px rgba(51, 102, 255, 0.4);
}

.day.event::after {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.1);
  opacity: 0;
  transition: opacity 0.2s ease-out;
}

.day.event:hover::after {
  opacity: 1;
}
```

---

### Notifications

```css
.notification {
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-left: 4px solid var(--primary-brand);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  display: flex;
  gap: 12px;
  align-items: flex-start;
  
  transition: all 0.3s ease-out;
  position: relative;
  overflow: hidden;
}

.notification::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    90deg,
    rgba(51, 102, 255, 0.05) 0%,
    transparent 100%
  );
  opacity: 0;
  transition: opacity 0.3s ease-out;
  pointer-events: none;
}

.notification:hover {
  transform: translateX(4px);
  border-left-color: var(--secondary-accent);
  box-shadow: 0 4px 12px rgba(51, 102, 255, 0.1);
  background: var(--bg-primary);
}

.notification:hover::before {
  opacity: 1;
}

.notification-icon {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  animation: iconBounce 0.4s ease-out;
}

@keyframes iconBounce {
  0% {
    transform: scale(0) rotate(-45deg);
    opacity: 0;
  }
  50% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1) rotate(0);
    opacity: 1;
  }
}

.notification-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
  transition: color 0.3s ease-out;
}

.notification:hover .notification-title {
  color: var(--primary-brand);
}

.notification-time {
  font-size: 12px;
  color: var(--text-secondary);
}
```

---

### Buttons

```css
.button {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  
  transition: all 0.2s ease-out;
  position: relative;
  overflow: hidden;
  
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

/* Primary Button */
.button-primary {
  background: linear-gradient(135deg, var(--primary-brand), #1e52cc);
  color: white;
  box-shadow: 0 4px 12px rgba(51, 102, 255, 0.2);
}

.button-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(51, 102, 255, 0.3);
}

.button-primary:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(51, 102, 255, 0.2);
}

/* Secondary Button */
.button-secondary {
  background: var(--bg-secondary);
  color: var(--primary-brand);
  border: 1px solid var(--primary-brand);
}

.button-secondary:hover {
  background: var(--primary-brand);
  color: white;
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(51, 102, 255, 0.2);
}

.button-secondary:active {
  transform: scale(0.98);
}

/* Ghost Button */
.button-ghost {
  background: transparent;
  color: var(--primary-brand);
  border: 1px solid transparent;
}

.button-ghost:hover {
  background: var(--bg-secondary);
  border-color: var(--primary-brand);
  transform: translateX(2px);
}

/* Ripple effect on click */
.button::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.button:active::after {
  animation: ripple 0.6s ease-out;
}

@keyframes ripple {
  to {
    width: 300px;
    height: 300px;
    opacity: 0;
  }
}
```

---

### Modal / Popup

```css
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal {
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 32px;
  max-width: 480px;
  width: 90%;
  box-shadow: 0 20px 25px rgba(0, 0, 0, 0.15);
  
  animation: modalSlideIn 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(-20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.modal-header {
  margin-bottom: 24px;
}

.modal-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.modal-body {
  margin-bottom: 24px;
}

.modal-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}
```

---

### Inputs & Forms

```css
.input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-primary);
  background: var(--bg-secondary);
  
  transition: all 0.2s ease-out;
}

.input:hover {
  border-color: var(--primary-brand);
  background: var(--bg-primary);
}

.input:focus {
  outline: none;
  border-color: var(--primary-brand);
  background: var(--bg-primary);
  box-shadow: 0 0 0 3px rgba(51, 102, 255, 0.1);
}

.input::placeholder {
  color: var(--text-secondary);
  opacity: 0.6;
}

/* Animated label */
.input-group {
  position: relative;
  margin-bottom: 16px;
}

.input-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 6px;
  transition: color 0.2s ease-out;
}

.input:focus + .input-label,
.input:not(:placeholder-shown) + .input-label {
  color: var(--primary-brand);
}
```

---

## 🎬 Animations Library

### Loading States

```css
/* Skeleton shimmer */
@keyframes shimmer {
  0% {
    background-position: -1000px 0;
  }
  100% {
    background-position: 1000px 0;
  }
}

.skeleton {
  background: linear-gradient(
    90deg,
    var(--bg-secondary) 0%,
    var(--bg-primary) 50%,
    var(--bg-secondary) 100%
  );
  background-size: 1000px 100%;
  animation: shimmer 2s infinite;
  border-radius: 8px;
}
```

### Entry Animations

```css
/* Stagger list items */
@keyframes listItemEnter {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.list-item {
  animation: listItemEnter 0.4s ease-out;
  animation-fill-mode: both;
}

.list-item:nth-child(1) { animation-delay: 0s; }
.list-item:nth-child(2) { animation-delay: 0.1s; }
.list-item:nth-child(3) { animation-delay: 0.2s; }
.list-item:nth-child(4) { animation-delay: 0.3s; }
.list-item:nth-child(5) { animation-delay: 0.4s; }
```

### Micro-interactions

```css
/* Icon pulse on hover */
@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}

.icon-hover:hover {
  animation: pulse 0.6s ease-in-out;
}

/* Checkmark animation */
@keyframes checkmark {
  0% {
    stroke-dashoffset: 100;
    opacity: 0;
  }
  50% {
    opacity: 1;
  }
  100% {
    stroke-dashoffset: 0;
  }
}

.checkmark-icon {
  animation: checkmark 0.6s ease-out;
}
```

---

## 📱 Responsive Adjustments

### Tablet (768px - 1023px)

```css
/* Reduce spacing */
.content { padding: 24px 16px; }
.card { padding: 20px; }

/* Single column cards */
.cards-grid {
  grid-template-columns: 1fr;
}

/* Larger touch targets */
.button { padding: 12px 24px; }
.day { font-size: 16px; }
```

### Mobile (< 768px)

```css
/* Header adjustments */
.header {
  height: 64px;
  padding: 0 16px;
}

.nav { display: none; } /* Show hamburger instead */

/* Content spacing */
.content { padding: 20px 16px; }
.section-title { font-size: 20px; }

/* Full-width inputs */
.input { width: 100%; }

/* Touch-friendly buttons */
.button {
  min-height: 44px;  /* Touch target minimum */
  padding: 12px 20px;
}

/* Simplified calendar */
.day { font-size: 13px; }
.calendar-nav button { width: 32px; height: 32px; }
```

---

## 🌓 Dark Mode Variables

```css
:root {
  /* Light mode */
  --bg-primary: #ffffff;
  --bg-secondary: #f7f8fa;
  --text-primary: #111218;
  --text-secondary: #6b7280;
  --border: #e5e7eb;
  --primary-brand: #3366ff;
  --secondary-accent: #ff6b35;
  --success-green: #34c759;
  --warning-amber: #ffcc00;
  --error-red: #ff3333;
}

@media (prefers-color-scheme: dark) {
  :root {
    --bg-primary: #1a1d23;
    --bg-secondary: #252a33;
    --text-primary: #f3f4f6;
    --text-secondary: #9ca3af;
    --border: #374151;
    --primary-brand: #66b3ff;
    --secondary-accent: #ff8855;
    --success-green: #52d273;
    --warning-amber: #ffd60a;
    --error-red: #ff6666;
  }
}
```

---

## ✨ Key Polish Details

1. **Gradient Text** - Logo, card values (premium feel)
2. **Subtle Glass Morphism** - Header blur (4px), modal backdrop (4px)
3. **Smooth Shadows** - Layered shadows for depth, not harsh
4. **Hover Elevation** - Cards lift, buttons float slightly
5. **Micro-animations** - Tab underline slides, icons bounce, buttons ripple
6. **Staggered Entries** - List items fade in sequentially
7. **High-Contrast States** - Focus rings visible, active states clear
8. **Consistent Timing** - 0.3s base, 0.2s for fast, 0.5s for slow
9. **Color Gradients** - Blue + Orange combination (not overdone)
10. **Touch Feedback** - Ripple effect on buttons, scale on press

---

## 🚀 Implementation Checklist

- [ ] Color variables defined
- [ ] Shadow system implemented
- [ ] Transition timing consistent
- [ ] Hover states on all interactive elements
- [ ] Focus states WCAG AA compliant
- [ ] Dark mode colors applied
- [ ] Animation keyframes defined
- [ ] Mobile breakpoints tested
- [ ] Touch targets ≥ 44px
- [ ] Loading states with shimmer
- [ ] Modal entrance animation smooth
- [ ] Button ripple effect working
- [ ] Calendar hover/click smooth
- [ ] Notification stagger animation
- [ ] Dark mode transitions smooth
