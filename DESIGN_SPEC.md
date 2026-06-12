# KarateOps Homepage Design Spec

**File Figma:** https://www.figma.com/design/Eq8X3m5H6lYnjTyn75jUhx

---

## Design System

### Dials
- **DESIGN_VARIANCE:** 7 (Modern, asymmetric but accessible)
- **MOTION_INTENSITY:** 6 (Vibrant, smooth transitions)
- **VISUAL_DENSITY:** 5 (Breathing space, moderate information density)

### Color Palette

#### Light Mode
- **Primary Brand:** `#3366FF` (Vibrant Blue) - CTAs, active states
- **Secondary Accent:** `#FF6B35` (Energetic Orange) - Highlights, badges
- **Success:** `#34C759` (Green) - Confirmations, status positive
- **Warning:** `#FFCC00` (Amber) - Alerts, pending states
- **Error:** `#FF3333` (Red) - Errors, cancellations
- **Background/Primary:** `#FFFFFF`
- **Background/Secondary:** `#F7F8FA` (Subtle gray)
- **Text/Primary:** `#111218` (Near-black)
- **Text/Secondary:** `#6B7280` (Medium gray)
- **Border:** `#E5E7EB` (Light gray)

#### Dark Mode
- **Primary Brand:** `#66B3FF` (Lighter blue)
- **Secondary Accent:** `#FF8855` (Lighter orange)
- **Success:** `#52D273` (Lighter green)
- **Warning:** `#FFD60A` (Lighter amber)
- **Error:** `#FF6666` (Lighter red)
- **Background/Primary:** `#1A1D23`
- **Background/Secondary:** `#252A33` (Subtle dark gray)
- **Text/Primary:** `#F3F4F6`
- **Text/Secondary:** `#9CA3AF`
- **Border:** `#374151`

### Typography

#### Font Stack
- **Primary:** Geist Sans (or Inter if unavailable)
- **Mono (for code/numbers):** Geist Mono (or JetBrains Mono)

#### Type Scales
| Type | Size | Weight | Line Height | Letter Spacing |
|------|------|--------|-------------|----------------|
| Display/H1 | 32-36px | 700 | 1.2 | -0.02em |
| H2 | 24px | 700 | 1.3 | -0.01em |
| H3 | 20px | 600 | 1.4 | 0 |
| Body Large | 16px | 400 | 1.5 | 0 |
| Body Regular | 14px | 400 | 1.5 | 0 |
| Caption/Small | 12px | 400 | 1.4 | 0.02em |
| Mono (numbers) | 14px | 500 | 1.5 | 0 |

### Spacing System
| Token | Value |
|-------|-------|
| xs | 4px |
| sm | 8px |
| md | 16px |
| lg | 24px |
| xl | 32px |
| 2xl | 48px |

**Layout Container:** `max-w-1440px` with side padding of `24px` (md)

### Border Radius
- **Buttons, Pills:** `24px` (full pill)
- **Cards, Inputs:** `12px`
- **Small elements:** `8px`
- **Disabled:** `0px` (sharp)

---

## Layout Overview

### Viewport
- **Desktop:** 1440px wide
- **Tablet:** 768px
- **Mobile:** 360px

### Main Container Structure

```
┌─ ROOT PAGE ─────────────────────────────┐
│                                          │
│  ┌─ HEADER ──────────────────────────┐  │
│  │ Logo  [Clb của tôi] [Xem giải đấu]   │
│  │                          [Avatar]│  │
│  └────────────────────────────────────┘  │
│                                          │
│  ┌─ CONTENT AREA ─────────────────────┐  │
│  │                                    │  │
│  │  [TAB: Clb của tôi]  [TAB: Giải]  │  │
│  │  ───────────────────────────────  │  │
│  │                                    │  │
│  │  ┌─ SECTION 1: FINANCIAL INFO ──┐ │  │
│  │  │ • Balance Card               │ │  │
│  │  │ • Payment Status Card        │ │  │
│  │  └──────────────────────────────┘ │  │
│  │                                    │  │
│  │  ┌─ SECTION 2: SCHEDULE ────────┐ │  │
│  │  │ • Calendar Grid              │ │  │
│  │  │ • Days with events shown     │ │  │
│  │  │ • Click day → leave request  │ │  │
│  │  └──────────────────────────────┘ │  │
│  │                                    │  │
│  │  ┌─ SECTION 3: NOTIFICATIONS ───┐ │  │
│  │  │ • List of announcements      │ │  │
│  │  │ • Timestamps                 │ │  │
│  │  └──────────────────────────────┘ │  │
│  │                                    │  │
│  │  [ADMIN VIEW: Club List]           │  │
│  │                                    │  │
│  └────────────────────────────────────┘  │
│                                          │
└──────────────────────────────────────────┘
```

---

## Section Designs

### HEADER

**Height:** 72px (desktop), 64px (mobile)

**Content:**
- Left: Logo (KarateOps, 32px height) + "KarateOps" wordmark
- Center/Right: Tab navigation + User avatar
- Sticky to top with `background: BG/Primary`, `box-shadow: 0 1px 3px rgba(0,0,0,0.08)`

**Elements:**
```
[Logo] KarateOps    [Clb của tôi]  [Xem các giải đấu]           [Avatar]
                    ← Active tab underline (3px, Primary blue)
```

**Dark mode:** Background shifts to BG/Primary dark, text to Text/Primary dark, shadow darker.

---

### TAB NAVIGATION

**Position:** Below header, sticky
**Style:** Clean underline indicator (3px height, Primary blue)
**Spacing:** 24px between tabs
**Font:** H3 (20px, 600 weight)
**Interaction:** Smooth transition (0.3s ease)

---

### SECTION 1: FINANCIAL INFO (Clb của tôi tab)

**Position:** Top of content, below tabs
**Height:** Auto (280-320px)
**Layout:** 2-column grid (desktop), 1-column (mobile < 768px)

**Card 1: Account Balance**
```
┌──────────────────────────────┐
│ Số dư tài khoản              │ (Caption, Text/Secondary)
│                              │
│ 450,000 ₫                   │ (Display, 32px, Primary blue, Mono font)
│                              │
│ ↑ +50,000 ₫ tháng này        │ (Small, Success green)
└──────────────────────────────┘
Background: BG/Secondary
Border: 1px Border color
Border-radius: 12px
Padding: 24px
```

**Card 2: Payment Status**
```
┌──────────────────────────────┐
│ Trạng thái thanh toán        │ (Caption, Text/Secondary)
│                              │
│ ✓ Đã thanh toán tháng này   │ (Body, Success green)
│                              │
│ Hạn chót: 30 Tháng 6, 2026  │ (Small, Text/Secondary)
└──────────────────────────────┘
Background: BG/Primary / BG/Secondary (subtle)
Padding: 24px
```

**Spacing between cards:** 16px (gap/md)

---

### SECTION 2: CLUB SCHEDULE & CALENDAR (Clb của tôi tab)

**Position:** Middle of content
**Height:** 400-500px
**Title:** "Lịch tập câu lạc bộ" (H2, 24px, 700 weight)

**Layout:** Calendar grid (month view)

```
┌──────────────────────────────────────────┐
│ Lịch tập câu lạc bộ          [← → ]    │ ← Month nav buttons
├──────────────────────────────────────────┤
│ T  H  B  T  T  N  CN                     │ (Weekday headers, Caption, bold)
├──────────────────────────────────────────┤
│ 1  2  3  4  5  6  7                      │
│ 8  9  10 11 12 13 14                     │
│ 15 16 17 18 19 20 21        ← Days with │
│ 22 23 24 25 26 27 28           events   │
│ 29 30                          shown in  │
│                                Primary   │
└──────────────────────────────────────────┘
```

**Day cell:**
- Default: `32px × 32px`, centered number (Body Regular), Text/Primary
- Has event: Background Primary blue (3:primary_blue), text white
- Today: Border 2px Primary blue
- Hover: Background BG/Secondary, cursor pointer
- Click → Modal popup (see below)

**Interactive State:**
- Hover a day with event: Slight scale (1.05), shadow
- Click → Leave request modal opens

**Leave Request Modal (Popup)**
```
┌────────────────────────────────┐
│ Xin nghỉ - Thứ 6, 15 Tháng 6  │ (H3)
├────────────────────────────────┤
│                                │
│ Lý do:                         │ (Label)
│ [________________________]     │ (Text input)
│                                │
│ ☐ Xin nghỉ dài hạn           │ (Checkbox)
│                                │
│           [Hủy]  [Xác nhận]   │ (Buttons)
└────────────────────────────────┘

Modal size: 400px wide, 300px high
Backdrop: Scrim (rgba(0,0,0,0.3))
Border-radius: 12px
```

**Long-term leave state (checkbox checked):**
```
┌────────────────────────────────┐
│ Xin nghỉ dài hạn              │
├────────────────────────────────┤
│                                │
│ Ngày bắt đầu:                  │ (Label)
│ [June 15, 2026    ▼]           │ (Date picker)
│                                │
│ Ngày kết thúc:                 │
│ [   Chưa rõ thời hạn ▼]        │ (Dropdown: date or "Chưa rõ")
│                                │
│ Lý do:                         │
│ [_______________________]      │
│                                │
│           [Hủy]  [Xác nhận]   │
└────────────────────────────────┘
```

---

### SECTION 3: NOTIFICATIONS / ANNOUNCEMENTS (Clb của tôi tab)

**Position:** Below schedule
**Title:** "Thông báo từ câu lạc bộ" (H2, 24px, 700 weight)
**Height:** Auto (200-400px depending on items)

**List structure:**
```
┌─ NOTIFICATION ITEM ────────────────────┐
│                                        │
│ [🔔] Lịch tập sẽ thay đổi tuần sau  │ (Announcement title, Body Large)
│      Cập nhật lúc 14:30              │ (Timestamp, Caption, Text/Secondary)
│                                        │
└────────────────────────────────────────┘

┌─ NOTIFICATION ITEM ────────────────────┐
│                                        │
│ [📢] Giải đấu mới đã được tạo        │
│      Cập nhật lúc 10:15              │
│                                        │
└────────────────────────────────────────┘
```

**Notification item specs:**
- Background: BG/Secondary (subtle)
- Padding: 16px
- Border-radius: 12px
- Border-left: 4px Primary blue (accent)
- Spacing between items: 12px
- Icon: 24px × 24px, left-aligned
- Title: Body Large, Text/Primary
- Timestamp: Caption, Text/Secondary, right-aligned (or below title on mobile)

**Interaction:** Hover → slight background shift, cursor pointer (future: click to view detail)

---

### SECTION 4: ADMIN CLUB VIEW (Only for CLUB_MANAGER role)

**Position:** Replaces Sections 1-3
**Title:** "Quản lý câu lạc bộ" (H2)

**Club list grid:** 2-column (desktop), 1-column (tablet/mobile)

```
┌─ CLUB CARD ────────────────────┐
│                                │
│ [Avatar] Karate Club 1         │ (H3, 20px, 600 weight)
│          Founder: Mr. A        │ (Caption, Text/Secondary)
│                                │
│ Members: 45  Events: 12        │ (Body Regular, Text/Secondary)
│                                │
│        [Quản lý →]             │ (Button, size: small, secondary style)
│                                │
└────────────────────────────────┘
```

**Card specs:**
- Background: BG/Primary / BG/Secondary
- Border: 1px Border color
- Border-radius: 12px
- Padding: 24px
- Hover: Shadow elevation, slight scale (1.02)
- Click → Navigate to club management detail page

---

### SECTION 5: ADMIN GLOBAL VIEW (Only for ADMIN_GLOBAL role)

**Same as ADMIN CLUB VIEW but shows all clubs across the organization**, with additional filter/search:

```
┌──────────────────────────────────────┐
│ Tất cả câu lạc bộ                    │
│                                      │
│ [Search input: "Tìm kiếm..."]       │
│                                      │
│ Sắp xếp: [Theo tên ▼] [Theo số nhân] │
└──────────────────────────────────────┘

[Club cards grid below, same as admin club view]
```

---

### TAB 2: TOURNAMENTS (Xem các giải đấu)

**Position:** Below header/tabs (same container as Tab 1)

**Layout:** List or card grid

**Tournament item:**
```
┌─ TOURNAMENT CARD ──────────────────┐
│                                    │
│ [Trophy icon] Giải đấu quốc tế 2026│ (H3, 20px, Primary blue)
│                                    │
│ Mô tả: Giải đấu karate quy mô       │ (Body Regular, Text/Secondary, max 2 lines)
│ lớn dành cho các vận động viên...  │
│                                    │
│ Trạng thái: Đang diễn ra           │ (Badge: Success green if active)
│ Ngày thi đấu: 15-20 Tháng 6        │ (Caption)
│                                    │
│              [Xem chi tiết →]       │ (Button, secondary)
│                                    │
└────────────────────────────────────┘
```

**Card Grid:** 2-column (desktop), 1-column (tablet), full width (mobile)
**Spacing between cards:** 16px
**Card height:** 280-320px

**Status badges:**
- **Active:** Success green (`#34C759`)
- **Coming Soon:** Warning amber (`#FFCC00`)
- **Completed:** Text/Secondary (gray)

---

## Interaction & Motion

### Transitions
- **Standard:** 0.3s ease (cubic-bezier(0.16, 1, 0.3, 1))
- **Hover state:** Color fade + slight shadow
- **Modal entrance:** Fade in + scale (0.95 → 1.0, 0.3s spring)

### Micro-interactions
1. **Button hover:** Color shifts to darker shade, shadow-sm appears
2. **Button active:** 2px inset shadow (simulates press)
3. **Calendar day hover:** Background tint, scale 1.05
4. **Card hover:** Shadow-md, translateY -2px
5. **Notification item hover:** Background change + left border color shift to Secondary

### Loading states
- Placeholder shimmer on cards (using CSS gradient animation)
- Skeleton loaders for list items

---

## Responsive Breakpoints

### Desktop (≥ 1024px)
- 2-column grids
- Full header nav
- Sidebar (future)
- All content visible

### Tablet (768px - 1023px)
- 1-2 column adaptive grids
- Hamburger nav (future)
- Reduced padding

### Mobile (< 768px)
- 1-column stacks
- Full-width cards
- Hamburger nav
- Reduced font sizes (H1: 28px, H2: 20px)
- Padding: 16px sides (sm)

---

## Components to Build in React

1. **Header** (`Header.tsx`)
   - Logo + title
   - Tab navigation (controlled)
   - User avatar menu

2. **TabContainer** (`TabContainer.tsx`)
   - Two tabs: "Clb của tôi" | "Xem các giải đấu"
   - Underline indicator

3. **FinancialCard** (`FinancialCard.tsx`)
   - Reusable card component
   - Supports icon, title, metric, subtitle
   - Variants: balance, payment-status

4. **Calendar** (`Calendar.tsx`)
   - Month view with event indicators
   - Click day → emit event
   - Navigation arrows

5. **LeaveRequestModal** (`LeaveRequestModal.tsx`)
   - Single-day leave form
   - Long-term leave toggle + date pickers
   - Submit/cancel actions

6. **NotificationItem** (`NotificationItem.tsx`)
   - Title + timestamp
   - Icon + left border

7. **NotificationList** (`NotificationList.tsx`)
   - Container for notification items

8. **ClubCard** (`ClubCard.tsx`)
   - Club info + manage button
   - Responsive grid layout

9. **TournamentCard** (`TournamentCard.tsx`)
   - Tournament info + status badge
   - Responsive grid

10. **Button** (`Button.tsx`)
    - Primary, secondary, ghost variants
    - Size: sm, md, lg
    - Loading state

11. **Input** (`Input.tsx`)
    - Text input with label
    - Error state

12. **Badge** (`Badge.tsx`)
    - Status indicators (success, warning, error)

---

## Dark Mode Implementation

- CSS variables bound to Figma color variables
- `prefers-color-scheme: dark` media query trigger
- Manual toggle button in header (future)
- System preference respected by default

---

## Next Steps

1. ✅ Design spec created
2. → Create design in Figma UI (manual)
3. → Build React components
4. → Implement API integration
5. → Testing & refinement
