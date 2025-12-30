# SmartAir - Code Documentation

Smart Air helps parents, children, and healthcare providers track and manage asthma-related data. 
Core features include symptom logging, medication tracking, PEF measurement, triage/emergency guidance, and sharing summaries with providers.

---

### Getting Started (Below are Instructions to Run Locally)

### Requirements
- Android Studio (recommended: latest stable)
- Android SDK installed
- A Firebase project

### Setup Steps
1. Clone the repo and open it in Android Studio.
2. In Firebase Console:
   - Create a project
   - Enable **Authentication -> Email/Password**
   - Create a **Firestore Database**
3. Download 'google-services.json' from Firebase:
   - Firebase Console -> Project settings -> Your apps -> Android -> download
   - Place it in the app directory, i.e.
     - 'app/google-services.json'
4. Sync Gradle and run the app:
   - Android Studio -> Sync Gradle
   - Select an emulator/device and run
  
---

### Sample Credentials (from video)
  - Parent Email:  vidParent@Example.com    Password: 123456
  - Child Username:TomUsername              Password: 123456
  - Child Username:SallyUsername            Password: 123456
  - Provider Email:vidProvider@Example.com  Password: 123456
---

### System Structure, Key Modules, Interactions, and Design Decisions

### **Badges**
- Parents can **set badges**, and children can **earn badges**.
- Access:
  - **Parent Home:** 'Add Badges'
  - **Child Home:** 'My Badges'

---

### **Daily Check-In**
- Parents can log their children's overall health condition for the day, and children log how they are feeling with each medication dose. 
- Access:
  - **Parent Home:** 'Daily Check-in'
  - **Child Home:** 'Take Medication'

---

### **PEF / Personal Best / Zones**
- **Parent sets Personal Best (PB)** using the 'Set Personal Best' button.
- **Parent or child enters PEF** using the 'Enter PEF' button, note: child users access this in zones section.
- Only the **Parent** may set PB.  
  This is an intentional design choice to ensure accuracy and safety since PB affects zone calculations. Restricting this prevents accidental changes by the child and keeps their experience simpler.
- The app **overwrites a PEF value only if the new entry for the same day is higher** (based on Piazza guidance).
- Zone access:
  - **Parent:** 'Today's Zone'
  - **Child:** 'Check Zone' on the home UI

---

## **Triage Flow**
- Accessible from the 'Have Trouble Breathing' button in the bottom navigation (both Parent and Child).
- Sends an alert to parent immediately when started.  
  
### **Flow Logic**
1. **Red Flags Check**
   - If any red flag is present -> show emergency guidance and send another alert to parent -> record triage data -> record medication -> return to main UI.
   - If no red flags -> proceed to incident logging.

2. **Incident Logging**
   - Record symptoms and context.
   - Record medication used.

3. **Decision Card**
   - Displays guidance based on severity.
   - Includes a 10-minute timer.
   - After 10 minutes -> follow-up prompt:
     - If improved -> return to decision card summary.
     - If worse or new symptoms -> return to red flags.

4. **New Symptoms / Feeling Worse**
   - Always redirects to red flags.

### **PDF Reporting Rule**
- PDF will export a full report of all sharable info approved by parent, this is saved in the Files app for ease in sharing the pdf to provider
- For "noticeable" triage entries in the PDF report:
  - Uses only the latest triage entry of the day.
  - Include it only if the decision card zone is not Green (Triage escalated).

---

### **User Interface Overview**

### **Parent & Child UI**
- **Home:** All core functionality.
- **Family:**
  - Parent-> list of children.
  - Child -> list of children but they can't change accounts.
- **Have Trouble Breathing:** Triage access.
- **Settings:** Logout access.

### **Provider UI**
- Read-only interface.
- Sees only child data the Parent has approved.
- Parent controls access via:  
  'Parent Home -> Child Overview -> Provider Permissions'
  and controls with toggles. 

### **Parent-Specific Features**
- **Child Overview:** Quick health summary.
- **View History:** Full medical history.
- **Alerts for:**
  - Red-zone days  
  - Rapid rescue repeats (>=3 in 3 hours)  
  - "Worse after dose" 
  - Triage escalation  
  - Inventory low/expired  
- **Child Summary:** Quick summary of child's basic health info.

---

### Provider View (Read-only Access)

Providers can only view data that the parent enables per child.

Provider home loads children where:
- 'children.sharedProviderUids' contains the provider UID

Provider homescreen shows buttons only if the corresponding sharing flag is true:
- Rescue Logs -> 'shareRescueLogs'
- Controller Summary -> 'shareControllerSummary'
- Symptoms -> 'shareSymptoms'
- Triggers -> 'shareTriggers'
- PEF Logs -> 'sharePEF'
- Triage Incidents -> 'shareTriageIncidents'
- Summary Charts -> 'shareSummaryCharts'

---

### Firestore Data Model (Key Paths)

### Users + Children
- 'users/{uid}'  
  Fields: 'email', 'role' (parent/child/provider), etc.

- 'users/{parentUid}/children/{childId}'  
  Fields:
  - 'name', 'dob' (or age fields if used)
  - Provider sharing flags:
    - 'shareRescueLogs'
    - 'shareControllerSummary'
    - 'shareSymptoms'
    - 'shareTriggers'
    - 'sharePEF'
    - 'shareTriageIncidents'
    - 'shareSummaryCharts'
  - Provider access list:
    - 'sharedProviderUids' (array of provider UIDs)

### Medication
- 'users/{parentUid}/children/{childId}/medications/{med_UUID}'
  Example fields:
  - 'med_UUID' (ID of medication in '.../medications')
  - 'puffsLeft' (int)
  - 'timestamp' (stored as Long millis)
  - 'isRescue' (boolean)
  - 'active' (boolean)
  - 'name' (String)
  - 'notes' (String)
  - 'puffNearEmptyThreshold' (unused int) - was going to be for future customization
  - 'purchaseDay' (int)
  - 'purchaseMonth' (int)
  - 'purchaseYear' (int)
  - 'expiryDay' (int)
  - 'expiryMonth' (int)
  - 'expiryYear' (int)
  - 'reminderDays' (unused int[]) - was going to be for future customization

### Med Logs
- 'users/{parentUid}/children/{childId}/medication/{med_UUID}'
  Example Fields:
   - 'childId' (ID of child in '.../medications')
   - 'doseCount' (int)
   - 'feelingChange' (String) - should be "N/A" if in triage and "Better"/"Same"/"Worse" if ddne in med logging
   - 'isRescue' (boolean)
   - 'medId' (ID of medication in '.../medications')
   - 'postFeeling' (int) Rating is 1 - 5 based
   - 'preFeeling' (int)
   - 'timestamp' (long)


### Daily Check-ins (Symptoms)
- 'users/{parentUid}/dailyCheckins/{docId}'
  Example Fields:
  - 'childId', 'childName', 'date'
  - 'nightWaking', 'activityLimits', 'coughWheeze'
  - 'triggers' (array)
  - 'authorLabel' (parent/child)

### PEF Logs
- 'users/{parentUid}/children/{childId}/PEF/logs/daily/{dateDoc}'
  Example Fields:
  - 'date'
  - 'timestamp' (Long millis)
  - 'pb', 'dailyPEF', 'prePEF', 'postPEF'
  - 'zone'

---

### Firestore Indexes Required

Some queries require composite indexes in Firestore.

### Rescue Logs Query (Provider + Parent summary)
Displays Collection: 'medLogs' from firebase. 
Filters to only display med logs where 'isRescue' == true.
Sorts display by decending timestamp.
---

### **Medication Logging**
- **Parent:** Adds medication inventory under 'Medication Inventory'. Can view medication logs under 'Medication Logs'.
- **Child:** Logs medication via 'Take Medication'.

---

### **Inhaler Technique**
- Children can access tutorials on proper inhaler techniques via the 'Inhaler Techniques' button. It will give a step by step process and decide how well technique of child is based on timer between taking doses. It will record a technique log. They can also access a video on inhaler technique by clicking the "watch a technique video!" button. 

---

### **Onboarding**
- Each user type receives onboarding on first use.
- If a child accesses their account by logging in independently, onboarding is triggered for them as well.

###  **Notifications/Alerts**
- Notifications are sent when, 
    - triage starts or escalates,
    - the child inputs a PEF in the red zone, 
    - rescue medications are used by child more than 3 times in a 3 hour window,
    - medications are going to expire soon (at 20, 10, 2, and 1 days),
    - the doses of a medication are below 40 (20% of the average 200 puff inhaler). 


---

## Notes / Known Limitations
- Some Firestore queries require composite indexes (see above).
- Triage incidents are derived from PEF daily logs (based on zone / thresholds used in the app).
- If data is missing on provider screens, confirm:
  1) Parent enabled the sharing toggle
  2) Provider UID is in 'sharedProviderUids'
  3) Firestore rules allow the provider read access
- Notifications can send instantly but require parent to be logged in and have app open or at least in the background. 



## Contributors

* Team name: **Smart Air – Group 67**
* Course: **CSCB07 – Software Design (Fall 2025)**
* Members:

- Zakariya Arale (Scrum Master)
- Alireza Pourreza
- Aydin Salar
- Ben Heerema
- Rohat Dilberoglu
