# Smart Air – Code Documentation

Smart Air helps parents, children, and healthcare providers track and manage asthma-related data. Core features include symptom logging, medication tracking, PEF measurement, triage support, and sharing summaries with providers.

---

## Key Modules, Interactions, and Design Decisions

### **Badges**
- Parents can **set badges**, and children can **earn badges**.
- Access:
  - **Parent Home:** `Add Badges`
  - **Child Home:** `My Badges`

---

### **Daily Check-In**
- Both parents and children can log their overall health condition for the day.
- Access:
  - **Parent Home:** `Daily Check-in`
  - **Child Home:** `Take Medication`

---

### **PEF / Personal Best / Zones**
- **Parent sets Personal Best (PB)** using the `Set Personal Best` button.
- **Parent enters PEF** using the `Enter PEF` button.
- Only the **Parent** may set PB.  
  This is an intentional design choice to ensure accuracy and safety since PB affects zone calculations. Restricting this prevents accidental changes by the child and keeps their experience simpler.
- The app **overwrites a PEF value only if the new entry for the same day is higher** (based on Piazza guidance).
- Zone access:
  - **Parent:** `Today's Zone`
  - **Child:** `Check Zone` on the home UI

---

## **Triage Flow**
- Accessible from the `Have Trouble Breathing` button in the bottom navigation (both Parent and Child).
  
### **Flow Logic**
1. **Red Flags Check**
   - If any red flag is present → show emergency guidance → record triage data → record medication → return to main UI.
   - If no red flags → proceed to incident logging.

2. **Incident Logging**
   - Record symptoms and context.
   - Record medication used.

3. **Decision Card**
   - Displays guidance based on severity.
   - Includes a **10-minute timer**.
   - After 10 minutes → follow-up prompt:
     - If improved → return to decision card summary.
     - If worse or new symptoms → return to red flags.

4. **New Symptoms / Feeling Worse**
   - Always redirects to red flags.

### **PDF Reporting Rule**
- PDF will export a full report of all sharable info apporved by parent, this will be saved in the Files app for ease in sharing the pdf to provider
- For “noticeable” triage entries in the PDF report:
  - Use **the latest triage entry of the day**.
  - Include it only if the decision card zone is **not Green**.

---

## **User Interface Overview**

### **Parent & Child UI**
- **Home:** All core functionality.
- **Family:**
  - Parent → list of children.
  - Child → their own profile only.
- **Have Trouble Breathing:** Triage access.
- **Settings:** Logout.

### **Provider UI**
- Read-only interface.
- Sees only child data the Parent has approved.
- Parent controls access via:  
  `Parent Home → Child Overview → Provider Permissions`.

### **Parent-Specific Features**
- **Child Overview:** Quick health summary.
- **View History:** Full medical history.
- **Alerts for:**
  - Red-zone days  
  - Rapid rescue repeats (≥3 in 3 hours)  
  - “Worse after dose”  
  - Triage escalation  
  - Inventory low/expired  
- **Child Summary:** Quick summary of child’s basic health info.

---

## **Medication Logging**
- **Parent:** Adds medication inventory under `Medication Inventory`. Can view medication logs under `Medication Logs`.
- **Child:** Logs medication via `Take Medication`.

---

## **Inhaler Technique**
- Children can access tutorials on proper inhaler techniques via the `Inhaler Techniques` button.

---

## **Onboarding**
- Each user type receives onboarding on first use.
- If a child accesses their account through the Parent app or logs in independently, onboarding is triggered for them as well.
