# Smart Air – Android App for Pediatric Asthma Support

Smart Air is a kid-friendly Android app that helps children (ages ~6–16) and their parents track asthma control, use inhalers correctly, and share a concise, parent-controlled summary with healthcare providers.

The app was developed as the course project for **CSCB07 – Software Design (Fall 2025)** and implements the requirements from the official project handout.

---

## High-Level Overview

**Main goals:**

- Help children understand and manage their asthma day-to-day.
- Give parents a clear, at-a-glance view of control, medications, and problem days.
- Allow providers to see a **read-only, parent-approved** summary report (PDF and in-app).

**Core feature buckets (R1–R6):**

- **R1 – Accounts, Roles & Onboarding:** Parent, Child, and Provider roles, secure login, and role-specific home screens.
- **R2 – Parent/Child Linking & Selective Sharing:** Parent manages multiple children and fine-grained “Share with Provider” toggles.
- **R3 – Medicines, Technique & Motivation:** Separate rescue vs controller logging, inhaler technique helper, inventory + badges.
- **R4 – Safety & Control (PEF, Zones & Triage):** PEF entry, Green/Yellow/Red zone logic, one-tap triage with escalation.
- **R5 – Symptoms, Triggers & History:** Daily check-ins, triggers, history browser and export.
- **R6 – Parent Home, Notifications & Provider Report:** Dashboard tiles, push alerts, and shareable provider report.

---

## Users & Roles

### Child

- Logs **rescue** and **controller** medications.
- Completes a simple **daily check-in** (night waking, activity limits, cough/wheeze).
- Uses **Inhaler Technique Training** (step-by-step tutorial with embedded media).
- Can see their **current zone** (Green/Yellow/Red) based on PEF and Personal Best.
- Earns **badges** for consistent controller use and good technique.

### Parent

- Creates an account and **manages one or more children** (name, DOB/age, notes).
- Sees a **Parent Home dashboard** with:
  - Today’s zone.
  - Last rescue time.
  - Weekly rescue count.
  - Short trend snippet (7/30 days).
- Manages **medication inventory**, schedules, and adherence.
- Receives **push alerts** for:
  - Red-zone days.
  - Rapid rescue repeats (≥ 3 uses in 3 hours).
  - “Worse after dose”.
  - Triage start/escalation.
  - Low/expired inventory.
- Controls **what, if anything, is shared** with the Provider per child.
- Can **export reports** (PDF, optionally CSV) for a 3–6 month window.

### Provider (read-only)

- Logs in with a **Provider account** (email/password).
- Can be invited by a Parent using a **one-time code/link** (valid for 7 days, revocable).
- Sees a **read-only Provider Home** for the children shared with them:
  - Rescue frequency and controller adherence.
  - Symptom burden & triggers.
  - Zone distribution over time.
  - Notable triage incidents.
  - Charts (time-series + categorical).

---

## Key Features by Domain

### Accounts, Roles & Onboarding (R1)

- **Email/password login** for Parent and Provider.
- Child can:
  - Either have their **own login**, or
  - Use a **child profile under a Parent account** (no email needed).
- **Role routing**:
  - After login, users are redirected to the correct home: Parent, Child, or Provider.
- **Onboarding flows** (per role):
  - Explain rescue vs controller medicines.
  - Explain app purpose.
  - Explain **who can see what** and how sharing works.
- **Security:**
  - Sign out for all roles.
  - Protected screens require authentication.
  - Input validation for registration (non-empty/valid fields, email format, etc.).

### Parent/Child Linking & Selective Sharing (R2)

- Parent can **add/manage multiple children**.
- For each child, a **“Share with Provider”** screen exposes toggles for:
  - Rescue logs
  - Controller adherence summary
  - Symptoms
  - Triggers
  - Peak-flow (PEF)
  - Triage incidents
  - Summary charts/history visuals
- Toggles are **real-time and reversible**:
  - Changes immediately affect what Providers see.
- Items that are shared show a small **“Shared with Provider” tag** in the UI.
- Parents can generate and manage **one-time invite codes/links** for Providers.

### Medication Logging & Inventory (R3)

**Modules:** `medlog` package (e.g., `RecordMedUsageActivity`, `MedicationInventoryActivity`, `ControllerLogActivity`, `MedicationLogRepository`, etc.).

- **Rescue vs Controller logs** are stored separately.
- Each medication entry includes:
  - Timestamp.
  - Dose count (puffs/measures).
  - Optional pre/post “Better/Same/Worse” check (with a short breath rating).
- **Inventory management:**
  - Parent can track purchase date, quantity left (via manual flags), expiry date.
  - **Alerts** for:
    - Low canister (≤ 20% remaining).
    - Expired medications.
- **Controller adherence**:
  - Parent configures a planned schedule.
  - Adherence is calculated as percentage of planned doses completed.

### Inhaler Technique & Motivation (R3)

**Modules:** `technique` and `badges`.

- **Technique Training:**
  - Step-by-step guidance: seal lips, slow deep breath, hold, spacing between puffs, spacer/mask tips, etc.
  - Includes at least one **embedded video/animation**.
- **Badges & streaks:**
  - Parent configures badge thresholds in `ParentBadgeSettingsActivity`.
  - Child sees earned badges in `ChildBadgesActivity`.
  - Examples:
    - First perfect controller week.
    - 10 high-quality technique sessions.
    - “Low rescue month” (≤ 4 rescue days in 30 days).
  - Shared logic/streaks module tracks consecutive days and high-quality sessions.

### PEF, Zones & One-Tap Triage (R4)

**Modules:** `pef`, `pb`, `zone`, `triage`.

- **PEF & Personal Best (PB):**
  - `PEFActivity` lets users enter PEF measurements (pre/post medication optional).
  - PB is set by the Parent in `PBActivity`.
  - Zone thresholds:
    - **Green:** ≥ 80% of PB.
    - **Yellow:** 50–79% of PB.
    - **Red:** < 50% of PB.
  - Zone changes are saved to history and shown on homescreens.

- **Triage Flow:**
  - Entry point is a **“Having trouble breathing?”** button.
  - Screen check:
    - Critical flags (e.g., can’t speak full sentences, chest retractions, blue/gray lips).
    - Recent rescue usage.
    - Optional current PEF.
  - **Decision card:**
    - “Call Emergency Now” for critical combinations.
    - “Start Home Steps” for controllable symptoms (aligned with the current zone).
  - A timer (default ~10 min) triggers **re-check & escalation** if not improved.
  - Triage incidents are logged and can show up in Provider reports if sharing is enabled.
  - Always displays a safety note: *“This is guidance, not a diagnosis. If in doubt, call emergency.”*

### Daily Check-ins, Triggers & History (R5)

**Modules:** `checkin`, `history`.

- **Daily check-in:**
  - Night wakening due to asthma.
  - Activity limitation.
  - Cough/wheeze severity.
  - Entry author: “Child-entered” vs “Parent-entered”.
- **Triggers:**
  - Multiple tags per entry:
    - Exercise, cold air, dust/pets, smoke, illness, strong odours/perfumes/cleaners, etc.
- **History browser (`HistoryActivity`):**
  - 3–6 months of data.
  - Filter by symptom, trigger, and date range.
  - Export selected range as **PDF** and optionally **CSV** for provider use (`pdf` package).

### Parent Dashboard, Alerts & Provider Report (R6)

**Modules:** `parent_home_ui`, `provider_home_ui`, `notification`, `sharing`, `pdf`.

- **Parent Home dashboard** tiles:
  - Today’s zone.
  - Last rescue time.
  - Weekly rescue count.
  - 7-day (or 30-day) trend snippet using a shared chart component (MPAndroidChart).
- **Notifications & Alerts:**
  - Implemented using **Firebase Cloud Messaging** + Android notifications:
    - Red-zone day.
    - Rapid rescue repeats ≥ 3 in 3 hours (using `RapidRescueCountHelper`).
    - “Worse after dose”.
    - Triage escalation.
    - Low/expired inventory.
  - Target latency: < 10 seconds from event to delivered push, network permitting.
- **Provider Report:**
  - Parent can select a 3–6 month window and export a PDF that includes:
    - Rescue frequency & controller adherence.
    - Symptom burden (counts of problem days).
    - Zone distribution over time.
    - Notable triage incidents.
    - At least one **time-series chart** and one **categorical chart** (bars/pie).
  - Parent fully controls whether this report is shared with a Provider.

---

## System Architecture & Packages

The Android app module lives in `SmartAirSetup/app`.

**Tech stack:**

- **Language:** Java (app code), Kotlin DSL for Gradle.
- **Minimum SDK:** 24 (Android 7.0).
- **Libraries:**
  - AndroidX AppCompat, RecyclerView, ConstraintLayout.
  - **Firebase:** Auth, Firestore, Cloud Messaging.
  - Lifecycle (ViewModel, LiveData).
  - MPAndroidChart for charts.
  - JUnit 4, AndroidX Test, Espresso, Mockito for tests.

**Major packages:**

- `login` – Login/Signup and **MVP-based** login presenter + interfaces.
- `parent_home_ui`, `child_home_ui`, `provider_home_ui` – Role-specific home screens and navigation.
- `navigation` – Shared navigation helpers between screens.
- `onboarding` – First-run flows for each role.
- `medlog` – Medication inventory, controller/rescue logs, pre/post checks, adherence.
- `technique` – Inhaler technique training screens and video.
- `badges` – Parent badge settings and child badge display.
- `pef`, `pb`, `zone` – PEF entry, personal best configuration, and zone UI.
- `checkin` – Daily symptom/trigger check-in.
- `history` – History screen with filters and list rendering.
- `triage` – Triage wizard flows, emergency decisions, and logging.
- `sharing` – Share flags repo, “Share with Provider” UI, provider invite flows.
- `pdf` – Report generation (PDF export).
- `notification` – Alert repository, notification helpers, and receivers.

---

## Data & Backend

- **Authentication:** Firebase Auth (email/password) for Parents and Providers.
- **Data storage:** Firebase Firestore for:
  - Users and roles.
  - Children and parent–child links.
  - Medication logs & inventory.
  - PEF & zone history.
  - Daily check-ins, triggers.
  - Triage incidents.
  - Share-flags and provider invites.
  - Badge/streak data.
- **Notifications:** Firebase Cloud Messaging (FCM) for real-time alerts.
- **Export:** PDF (and optional CSV) built client-side based on Firestore data.

> Note: Some Firestore queries use compound filters and ordering; Firestore may prompt you to create **composite indexes** on first run. Use the auto-generated console links to create these indexes if needed.

---

## App Navigation (How to Use)

**Parent flow:**

1. Register as a **Parent** and log in.
2. Complete onboarding (rescue vs controller, privacy, sharing).
3. Add a child profile (name, age, notes).
4. Configure controller schedule and medication inventory.
5. Use Parent Home to:
   - See today’s zone and recent rescue use.
   - Tap into history, triage incidents, and reports.
6. Optionally invite a **Provider** and toggle which data types are shared.

**Child flow:**

1. Either:
   - Log in with their own child account, or
   - Use the child profile inside the Parent app.
2. Use **Take Medication** to log doses with pre/post checks.
3. Use **Daily Check-in** to report symptoms and triggers.
4. Use **Inhaler Technique** for training and to earn technique badges.

**Provider flow:**

1. Register as a **Provider** and log in.
2. Enter the invite code/link from the Parent.
3. View the read-only dashboard and report for that child, based on the Parent’s share toggles.

---

## Sample Credentials (from Demo Video)

These are placeholders that should be filled in with actual sample accounts used in your recording/demo:

```text
Parent:
  Email: TODO
  Password: TODO

Child:
  Username/ID: TODO
  Password/PIN: TODO

Provider:
  Email: TODO
  Password: TODO
````

> Replace the `TODO` values above with real demo credentials before submission.

---

## Testing (JUnit + Mockito)

* The login flow is refactored to **MVP**:

  * `LoginPresenter` contains business logic.
  * `LoginView` is an interface implemented by `LoginActivity`.
  * `LoginModel` wraps Firebase/Auth operations.
* **Unit tests:**

  * Located under `app/src/test/java/com/example/smartairsetup/` (e.g., `LoginPresenterTest`).
  * Use **JUnit 4** and **Mockito** to:

    * Mock `LoginView` and `LoginModel`.
    * Verify that:

      * Validation errors show the correct error messages.
      * Sign-in button is enabled/disabled correctly.
      * Reset password is only available for valid parent/provider emails.
      * Model methods are called with the correct arguments.

**How to run tests:**

1. In Android Studio, right-click the `test` package or `LoginPresenterTest`.
2. Select **Run ‘Tests in …’**.
3. To see coverage, use **Run with Coverage** on the same package/file.

---

## Building & Running the App

### Prerequisites

* **Android Studio** (Hedgehog/Koala or later).
* **Java 17 / JDK compatible with AGP 8.x**.
* Android device or emulator with **API 24+**.
* A **Firebase project** with:

  * Email/Password auth enabled.
  * Firestore.
  * Cloud Messaging (FCM).

### Setup Steps

1. **Clone / extract the repo**

   ```bash
   git clone https://github.com/ZakariyaArale/Smart-Air-Group67
   cd Smart-Air-Group67/SmartAirSetup
   ```

2. **Open in Android Studio**

   * Open the `SmartAirSetup` folder as an Android project.
   * Let Gradle sync and download dependencies.

3. **Connect to Firebase**

   * In the Firebase console, create a new project.
   * Add an Android app with:

     * Package name: `com.example.smartairsetup`
     * (Optional) SHA-1 for Google-sign-in or stricter security rules.
   * Download the `google-services.json` file and place it in:

     * `SmartAirSetup/app/google-services.json`
   * Ensure `com.google.gms.google-services` is applied (already in `build.gradle.kts`).

4. **Configure Firestore & FCM**

   * Turn on **Firestore** in test mode (or configure rules).
   * Turn on **Cloud Messaging**.
   * If Firestore errors mention an index, follow the link to create the needed composite index.

5. **Run the app**

   * Select a device/emulator.
   * Click **Run ▶** in Android Studio.
   * Register accounts and explore the parent, child, and provider flows as described above.

---

## Development Process (Scrum & Git)

* Followed the **Scrum** guidelines from the course:

  * 2–3 sprints, each with a clearly defined sprint backlog.
  * User stories authored from the Requirements document and tracked in Jira.
  * Stand-up meetings documented (what was done, what’s next, blockers).
* **Version Control:**

  * Git repository with branches per feature.
  * Pull requests use the template in `docs/PULL_REQUEST_TEMPLATE.md` to enforce:

    * Clear change description.
    * Change type (Feature/Chore/Fix/Testing/Docs).
    * Self-review checklist (descriptive naming, no dead code, error handling, etc.).

---

## Known Limitations / Future Work (Nice-to-Have)

Some potential future improvements:

* True offline-first support and conflict resolution when reconnecting.
* More robust error handling and user feedback for network issues.
* Additional languages and accessibility enhancements (e.g., larger fonts, color-blind friendly charts).
* More fine-grained provider access control (per-provider vs global toggles).
* Expanded analytics on trends beyond 6 months.

---

## Contributors

* Team name: **Smart Air – Group 67**
* Course: **CSCB07 – Software Design (Fall 2025)**
* Members:

  * Alireza Pourreza
  * Aydin Salar
  * Zakariya Arale
  * Ben Heerema
  * Rohat Dilberoglu
