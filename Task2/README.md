# Task 2: From Requirement to Event-Driven Design (Hospital Outpatient Appointment Desk)

**Files:** `AppointmentDesk.java` (Swing window + booking service + two doctor directories), `AppointmentDemo.java` (tests, event trace, timing)
**Run:** `javac *.java && java -cp .:../Task6/lib/mariadb-java-client-2.7.6.jar AppointmentDesk` (on Windows use `;` instead of `:`). Without a database it falls back to built-in sample doctors.
**Evidence:** `output/AppointmentDemo.txt`, `screenshots/`

## 2.1 Event model
| Event source | Event object | Listener interface (method) | Action taken |
|---|---|---|---|
| `JFrame` opens | `WindowEvent` | `WindowListener.windowOpened` (via `WindowAdapter`) | log; focus the patient ID field |
| `JComboBox` department | `ItemEvent` (DESELECTED then SELECTED) | `ItemListener.itemStateChanged` | on SELECTED only: reload the doctor combo for that department; enable it; show the count |
| `JComboBox` doctor / slot | `ActionEvent` | (none needed) | the value is read when Book is pressed |
| `JTextField` patient ID, Enter key | `ActionEvent` | `ActionListener.actionPerformed` | same as clicking Book |
| `JButton` Book | `ActionEvent` | `ActionListener.actionPerformed` | validate → book → confirmation or red error; reset the form |
| `JButton` Clear | `ActionEvent` | `ActionListener.actionPerformed` | empty the form |
| `JFrame` close box | `WindowEvent` | `WindowListener.windowClosing` | if a booking is in progress, `JOptionPane.showConfirmDialog`; close only on YES |
| `JFrame` after `dispose()` | `WindowEvent` | `windowClosed` | log; release resources |

## 2.2 Component hierarchy
```
JFrame "Outpatient Appointment Desk"      (contentPane: JPanel, BorderLayout)
├── NORTH : JLabel  "Outpatient Appointments"
├── CENTER: JPanel form  (GridLayout 4 × 2)
│     ├── JLabel "Patient ID:"   ── JTextField patientId
│     ├── JLabel "Department:"   ── JComboBox<String> department
│     ├── JLabel "Doctor:"       ── JComboBox<Doctor> doctor      (model replaced on every department change)
│     └── JLabel "Time slot:"    ── JComboBox<String> slot
└── SOUTH : JPanel (BorderLayout)
      ├── CENTER: JPanel (FlowLayout)  ── JButton "Book", JButton "Clear"
      └── SOUTH : JLabel status        (green = confirmation, red = error)
JOptionPane (modal dialog) — "A booking is in progress. Close and discard it?"
```

## 2.3 Handler pseudocode (SIMATS standard)
```
ALGORITHM RefreshDoctors                      // on department ItemEvent with state = SELECTED
INPUT  : dept ← selected department; doctorsByDept : Map<String, List<Doctor>> (loaded once)
OUTPUT : doctor combo shows only that department's doctors
BEGIN
    IF dept = NULL THEN
        doctor.clear(); doctor.disable(); RETURN
    END IF
    list ← doctorsByDept.getOrDefault(dept, empty list)
    doctor.setModel(list)
    IF list is empty THEN
        doctor.disable(); status ← "No doctors available in " + dept   (red)
    ELSE
        doctor.enable();  status ← size(list) + " doctor(s) in " + dept  (green)
    END IF
END

ALGORITHM BookAppointment                     // on Book ActionEvent (or Enter in patient ID)
INPUT  : pid ← patientId.text; doc ← doctor.selected; slot ← slot.selected; taken : Set<String>
OUTPUT : confirmation or error message
BEGIN
    IF pid does not match "P" followed by 5 digits THEN  status ← "Patient ID must look like P12345."; RETURN
    IF doc = NULL  THEN status ← "Choose a doctor.";    RETURN
    IF slot = NULL THEN status ← "Choose a time slot."; RETURN
    key ← doc.id + "@" + slot
    IF key ∈ taken THEN  status ← doc.name + " is already booked at " + slot; RETURN
    taken ← taken ∪ {key}
    status ← "Booked " + pid + " with " + doc.name + " at " + slot     (green)
    reset patientId, department, doctor
END
```

## Test results (screenshots 1–7)
| Test | Result |
|---|---|
| Orthopaedics chosen | doctor list = [Dr. Chitra, Dr. Dinesh, Dr. Ezhil] |
| Cardiology chosen | doctor list = [Dr. Anitha, Dr. Bala]: the list follows the department |
| T1 valid booking | Booked P10001 with Dr. Anitha (Cardiology) at 09:30. |
| T2 patient ID "P1" | Patient ID must look like P12345. |
| T3 same doctor + slot | Dr. Anitha is already booked at 09:30. |
| T4 other doctor, same slot | Booked P10003 with Dr. Gokul (ENT) at 09:30. |
| close with a booking in progress | confirmation dialog shown; answered **No**, window stays open; after Clear it closes without asking |

## 2.4 Event trace (from `output/AppointmentDemo.txt`)
```
constructor: components created
windowActivated
windowOpened
itemStateChanged(department=Orthopaedics)
itemStateChanged(department=Cardiology)
actionPerformed(Book)  ... (one per test)
windowClosing                       ← close box clicked with a booking in progress
windowDeactivated                   ← the modal confirm dialog takes focus
confirm dialog answered NO          ← window kept
windowClosing                       ← second close, form now empty
windowClosed                        ← after dispose()
```
Order: **constructor → setVisible(true) → windowActivated → windowOpened → (user events) → windowClosing → [dispose()] → windowClosed.** `windowDeactivated`/`windowActivated` fire whenever focus moves to or from another window (here the dialog). `windowIconified`/`windowDeiconified` fire on minimise and restore.
**Compared with an applet's life cycle:** an applet's methods are called by the *browser*: `init()` (once, like our constructor), `start()` (each time the page becomes visible, like `windowActivated`/`windowDeiconified`), `paint()` (whenever it must be redrawn, as in any component), `stop()` (when the page is left, like `windowDeactivated`/`windowIconified`) and `destroy()` (once, like `windowClosed`). A frame application *owns* its window and decides when to close; an applet can only react. (Applets are deprecated since Java 9 and removed from browsers.)

## Alternative concept: query the database on every department change
```java
List<Doctor> doctorsIn(String dept) {              // DbDirectory
    try (PreparedStatement ps = c.prepareStatement("SELECT id, name, department FROM doctors WHERE department = ? ORDER BY name")) {
        ps.setString(1, dept); ... read ResultSet ...
    }
}
```
**Measured** (MariaDB on the same machine, median of 5 × 500 changes):
| d departments | m doctors | Map lookup | DB query per change | Map memory |
|---|---|---|---|---|
| 10 | 50 | **0.27 µs** | 53.8 µs | ≈ few KB |
| 50 | 1,000 | 0.34 µs | 95.9 µs | ≈ 178 KB |
| 200 | 10,000 | 0.29 µs | 197.4 µs | ≈ 1.8 MB |

- **Time per change:** Map **O(1)** hash lookup, plus O(m/d) to fill the combo. DB: a network round-trip plus an index lookup, **O(log m + m/d)**, and about **200–700× slower** even locally; over a real network add 1–50 ms. It must also run off the EDT, or the combo freezes.
- **Memory:** Map **O(d + m)**, all doctors held (≈ 180 B per doctor including the String objects). DB approach: **O(m/d)**, only the current department's list.
- **Loading:** the Map is loaded once at start-up (one query, O(m)). The trade-off is **staleness**: a doctor added during the day is not seen until reload. **Choice:** Map, because the doctor list changes rarely and is small; reload it on a menu action or a timer.
