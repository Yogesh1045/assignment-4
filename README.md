# Programming in Java: Assignment 4 (CO4)
## Event-Driven GUI Design: AWT, Swing, Listeners, Layouts and GUI–JDBC

| | |
|---|---|
| **Name** | Yogesh |
| **Register Number** | 192371045 |
| **Course** | CSA09 – Programming in Java |
| **Java version** | OpenJDK 21 (Java 17 or newer needed) |
| **Database (Tasks 2, 6)** | MySQL 8 or MariaDB 10; driver in `Task6/lib/` |

One folder per task, each with Java source, a `screenshots/` folder (every screenshot is linked to a test in that task's README), an `output/` folder (console logs, `.txt` + `.png`), and a `README.md` with the diagnoses, tables, diagrams, pseudocode and comparisons.

> **How the screenshots were made:** each `*Demo.java` drives the real window (button clicks, typed text, mouse events, window-closing events) and captures it with `java.awt.Robot`. They were recorded on a Linux virtual display with no window manager, which is why windows appear without a title bar.

## How to run
```bash
./run_all.sh                 # compiles and runs every task's demo, regenerating outputs and screenshots
cd Task5 && javac Converter.java && java Converter    # any single program by hand
```
Tasks 2 and 6 need the database: run `Task6/sql/schema.sql`, then pass `-Ddb.url=jdbc:mysql://localhost:3306/campus -Ddb.user=... -Ddb.password=...` (defaults: MariaDB URL, `student`/`student123`). Task 2 also runs without a database, using built-in sample doctors.

## Results
| Task | Topic | Marks | Result |
|---|---|---|---|
| [Task1](Task1) | Diagnose and repair a fee window | 6 | 10 deficiencies diagnosed (the original does not even compile: 4 checked-exception errors). Redesign: `FeeService` + thin `FeeWindow` + SwingWorker; 10/10 service tests pass without a GUI; EDT frozen 2,950 ms → responsive in 2 ms. |
| [Task2](Task2) | Appointment desk: requirement → design | 8 | Event table, component tree, pseudocode, full window-event trace recorded. Doctor list follows the department; double-booking rejected; confirm-on-close works. Map lookup 0.3 µs vs DB query 54–197 µs. |
| [Task3](Task3) | Library search layout | 9 | Nested BorderLayout/FlowLayout. At 3 sizes only the results area grew (216 → 576 px high); the others stayed the same, as measured. Absolute layout clips controls (screenshots). |
| [Task4](Task4) | DrawPad forensics | 7 | **Complete the Code:** dots survive minimise/restore (screenshots before and after). `mouseclicked` compiles silently without `@Override` and is a compile error with it. BufferedImage repaint is constant (≈ 15 µs) vs O(n) (118 ms at 100k dots). |
| [Task5](Task5) | Four listener styles | 7 | **Complete the Code:** `30.00 USD` / `Invalid amount` / Enter works. Change effort measured: 3, 6, 8, 4 lines; the inner-class `this` trap was found by the compiler. Shared `Action` alternative implemented. |
| [Task6](Task6) | Event registration (Swing + JDBC) | 13 | Layered UI/service/DAO; JMenuBar + CardLayout + JTable + paging with SwingWorker. Normal, invalid and duplicate registrations demonstrated; the UNIQUE constraint backs the duplicate rule. 100k rows: load-all freezes the window for 608 ms and uses 41 MB; paged uses 0.2 MB with a responsive window. |
