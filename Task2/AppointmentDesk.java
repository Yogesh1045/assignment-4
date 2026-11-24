import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Task 2 - Hospital outpatient appointment desk.
 * JFrame (BorderLayout)
 *  ├─ NORTH : JLabel title
 *  ├─ CENTER: JPanel form (GridLayout 4x2)
 *  │     JLabel+JTextField patientId | JLabel+JComboBox department | JLabel+JComboBox doctor | JLabel+JComboBox slot
 *  └─ SOUTH : JPanel (BorderLayout)  CENTER: JPanel buttons [Book][Clear]   SOUTH: JLabel status
 */
public class AppointmentDesk extends JFrame {

    record Doctor(String id, String name, String department) { @Override public String toString() { return name; } }

    /** Where the doctor list comes from (Map loaded once, or a DB query per change). */
    interface DoctorDirectory { List<Doctor> doctorsIn(String dept) throws Exception; List<String> departments() throws Exception; }

    static final String[] SLOTS = {"09:00", "09:30", "10:00", "10:30", "11:00", "11:30", "14:00", "14:30", "15:00"};

    // ---------- the model / service (no GUI code) ----------
    static final class BookingService {
        final Set<String> taken = new HashSet<>();                    // doctorId + slot
        String book(String patientId, Doctor d, String slot) {
            if (patientId == null || !patientId.trim().matches("P\\d{5}")) throw new IllegalArgumentException("Patient ID must look like P12345.");
            if (d == null) throw new IllegalArgumentException("Choose a doctor.");
            if (slot == null) throw new IllegalArgumentException("Choose a time slot.");
            if (!taken.add(d.id() + "@" + slot)) throw new IllegalStateException(d.name() + " is already booked at " + slot + ".");
            return "Booked " + patientId.trim() + " with " + d.name() + " (" + d.department() + ") at " + slot + ".";
        }
    }

    final JTextField patientId = new JTextField(10);
    final JComboBox<String> department = new JComboBox<>();
    final JComboBox<Doctor> doctor = new JComboBox<>();
    final JComboBox<String> slot = new JComboBox<>(SLOTS);
    final JButton book = new JButton("Book"), clear = new JButton("Clear");
    final JLabel status = new JLabel("Select a department.");
    final DoctorDirectory directory;
    final BookingService service = new BookingService();
    final Consumer<String> trace;
    boolean askBeforeClose = true;                                    // the demo switches this to answer the dialog itself

    AppointmentDesk(DoctorDirectory dir, Consumer<String> trace) throws Exception {
        super("Outpatient Appointment Desk");
        this.directory = dir; this.trace = trace;
        trace.accept("constructor: components created");
        JLabel title = new JLabel("  Outpatient Appointments");
        title.setFont(title.getFont().deriveFont(16f));
        JPanel form = new JPanel(new GridLayout(4, 2, 6, 6));
        form.add(new JLabel("Patient ID:")); form.add(patientId);
        form.add(new JLabel("Department:")); form.add(department);
        form.add(new JLabel("Doctor:"));     form.add(doctor);
        form.add(new JLabel("Time slot:"));  form.add(slot);
        JPanel buttons = new JPanel(); buttons.add(book); buttons.add(clear);
        JPanel south = new JPanel(new BorderLayout()); south.add(buttons, BorderLayout.CENTER); south.add(status, BorderLayout.SOUTH);
        JPanel root = new JPanel(new BorderLayout(6, 6)); root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        root.add(title, BorderLayout.NORTH); root.add(form, BorderLayout.CENTER); root.add(south, BorderLayout.SOUTH);
        setContentPane(root);

        for (String d : dir.departments()) department.addItem(d);
        department.setSelectedIndex(-1);
        doctor.setEnabled(false);

        department.addItemListener(e -> {                             // ItemEvent fires twice: DESELECTED then SELECTED
            if (e.getStateChange() == ItemEvent.SELECTED) { trace.accept("itemStateChanged(department=" + e.getItem() + ")"); refreshDoctors(); }
        });
        book.addActionListener(e -> { trace.accept("actionPerformed(Book)"); onBook(); });
        clear.addActionListener(e -> { trace.accept("actionPerformed(Clear)"); onClear(); });
        patientId.addActionListener(e -> book.doClick());            // Enter in the ID field = Book

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e)      { trace.accept("windowOpened"); }
            @Override public void windowActivated(WindowEvent e)   { trace.accept("windowActivated"); }
            @Override public void windowDeactivated(WindowEvent e) { trace.accept("windowDeactivated"); }
            @Override public void windowIconified(WindowEvent e)   { trace.accept("windowIconified"); }
            @Override public void windowDeiconified(WindowEvent e) { trace.accept("windowDeiconified"); }
            @Override public void windowClosed(WindowEvent e)      { trace.accept("windowClosed"); }
            @Override public void windowClosing(WindowEvent e) {
                trace.accept("windowClosing");
                if (bookingInProgress() && askBeforeClose) {
                    int ans = JOptionPane.showConfirmDialog(AppointmentDesk.this,
                            "A booking is in progress. Close and discard it?", "Confirm close", JOptionPane.YES_NO_OPTION);
                    trace.accept("confirm dialog answered " + (ans == JOptionPane.YES_OPTION ? "YES" : "NO"));
                    if (ans != JOptionPane.YES_OPTION) return;
                }
                dispose();
            }
        });
        pack();
    }

    /** In progress = something entered but not yet booked. */
    boolean bookingInProgress() { return !patientId.getText().isBlank() || department.getSelectedIndex() >= 0; }

    void refreshDoctors() {
        String dept = (String) department.getSelectedItem();
        try {
            List<Doctor> list = directory.doctorsIn(dept);
            doctor.setModel(new DefaultComboBoxModel<>(list.toArray(new Doctor[0])));
            doctor.setEnabled(!list.isEmpty());
            say(list.isEmpty() ? "No doctors available in " + dept + "." : list.size() + " doctor(s) in " + dept + ".", !list.isEmpty());
        } catch (Exception ex) {
            doctor.setModel(new DefaultComboBoxModel<>());
            doctor.setEnabled(false);
            say("Could not load doctors: " + ex.getMessage(), false);
        }
    }

    void onBook() {
        try {
            String msg = service.book(patientId.getText(), (Doctor) doctor.getSelectedItem(), (String) slot.getSelectedItem());
            say(msg, true);
            patientId.setText(""); department.setSelectedIndex(-1); doctor.setModel(new DefaultComboBoxModel<>()); doctor.setEnabled(false);
        } catch (RuntimeException ex) {
            say(ex.getMessage(), false);
        }
    }

    void onClear() { patientId.setText(""); department.setSelectedIndex(-1); doctor.setModel(new DefaultComboBoxModel<>()); doctor.setEnabled(false); say("Cleared.", true); }

    void say(String m, boolean ok) { status.setText(m); status.setForeground(ok ? new Color(0, 110, 0) : Color.RED); pack(); }

    // ---------- data sources ----------
    static final String URL = System.getProperty("db.url", "jdbc:mariadb://localhost:3306/campus");
    static final String USER = System.getProperty("db.user", "student"), PWD = System.getProperty("db.password", "student123");

    static final Object[][] SAMPLE = {
        {"D01", "Dr. Anitha", "Cardiology"}, {"D02", "Dr. Bala", "Cardiology"},
        {"D03", "Dr. Chitra", "Orthopaedics"}, {"D04", "Dr. Dinesh", "Orthopaedics"}, {"D05", "Dr. Ezhil", "Orthopaedics"},
        {"D06", "Dr. Farah", "Paediatrics"}, {"D07", "Dr. Gokul", "ENT"}, {"D08", "Dr. Hema", "Dermatology"},
    };

    /** Creates and fills the doctors table (d departments, m doctors). */
    static void seed(int departments, int doctorsPerDept) throws Exception {
        try (Connection c = DriverManager.getConnection(URL, USER, PWD); Statement s = c.createStatement()) {
            s.execute("DROP TABLE IF EXISTS doctors");
            s.execute("CREATE TABLE doctors (id VARCHAR(10) PRIMARY KEY, name VARCHAR(60) NOT NULL, department VARCHAR(40) NOT NULL, INDEX(department))");
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO doctors VALUES (?,?,?)")) {
                if (departments == 0) for (Object[] r : SAMPLE) { ps.setString(1, (String) r[0]); ps.setString(2, (String) r[1]); ps.setString(3, (String) r[2]); ps.addBatch(); }
                else for (int d = 0; d < departments; d++) for (int k = 0; k < doctorsPerDept; k++) {
                    ps.setString(1, "X" + d + "_" + k); ps.setString(2, "Dr. " + d + "-" + k); ps.setString(3, "Dept" + d); ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    /** Loaded ONCE into a Map<String, List<Doctor>>; every department change is a HashMap lookup. */
    static final class MapDirectory implements DoctorDirectory {
        final Map<String, List<Doctor>> byDept = new LinkedHashMap<>();
        MapDirectory() throws Exception {
            try (Connection c = DriverManager.getConnection(URL, USER, PWD);
                 Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT id, name, department FROM doctors ORDER BY department, name")) {
                while (rs.next()) byDept.computeIfAbsent(rs.getString(3), k -> new ArrayList<>()).add(new Doctor(rs.getString(1), rs.getString(2), rs.getString(3)));
            }
        }
        MapDirectory(Object[][] rows) { for (Object[] r : rows) byDept.computeIfAbsent((String) r[2], k -> new ArrayList<>()).add(new Doctor((String) r[0], (String) r[1], (String) r[2])); }
        public List<Doctor> doctorsIn(String dept) { return byDept.getOrDefault(dept, List.of()); }
        public List<String> departments() { return new ArrayList<>(byDept.keySet()); }
    }

    /** Alternative: a SELECT on every department change. */
    static final class DbDirectory implements DoctorDirectory {
        final Connection c;
        DbDirectory() throws Exception { c = DriverManager.getConnection(URL, USER, PWD); }
        public List<Doctor> doctorsIn(String dept) throws Exception {
            List<Doctor> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT id, name, department FROM doctors WHERE department = ? ORDER BY name")) {
                ps.setString(1, dept);
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(new Doctor(rs.getString(1), rs.getString(2), rs.getString(3))); }
            }
            return out;
        }
        public List<String> departments() throws Exception {
            List<String> out = new ArrayList<>();
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT DISTINCT department FROM doctors ORDER BY department")) { while (rs.next()) out.add(rs.getString(1)); }
            return out;
        }
    }

    public static void main(String[] args) throws Exception {
        DoctorDirectory dir;
        try { seed(0, 0); dir = new MapDirectory(); }
        catch (Exception e) { System.out.println("Database not available (" + e.getMessage() + ") - using built-in sample doctors."); dir = new MapDirectory(SAMPLE); }
        DoctorDirectory d = dir;
        SwingUtilities.invokeLater(() -> {
            try { new AppointmentDesk(d, m -> System.out.println("[event] " + m)).setVisible(true); }
            catch (Exception e) { e.printStackTrace(); }
        });
    }
}
