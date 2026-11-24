import java.awt.Window;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

/** Task 2 test driver: event trace from open to close, booking tests with screenshots, Map vs per-change query timing. */
public class AppointmentDemo {
    static final List<String> trace = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        AppointmentDesk.seed(0, 0);
        AppointmentDesk.DoctorDirectory dir = new AppointmentDesk.MapDirectory();
        AppointmentDesk[] h = new AppointmentDesk[1];
        long t0 = System.nanoTime();
        SwingUtilities.invokeAndWait(() -> {
            try { h[0] = new AppointmentDesk(dir, m -> trace.add(String.format("%6.0f ms  %s", (System.nanoTime() - t0) / 1e6, m))); }
            catch (Exception e) { throw new RuntimeException(e); }
            h[0].setLocation(0, 0); h[0].setVisible(true);
        });
        AppointmentDesk w = h[0];
        Thread.sleep(600);
        Shot.take(w, "screenshots/1_opened.png");

        SwingUtilities.invokeAndWait(() -> w.department.setSelectedItem("Orthopaedics"));
        System.out.println("Doctors after choosing Orthopaedics: " + itemsOf(w));
        SwingUtilities.invokeAndWait(() -> w.department.setSelectedItem("Cardiology"));
        System.out.println("Doctors after choosing Cardiology  : " + itemsOf(w));
        Shot.take(w, "screenshots/2_doctor_list_follows_department.png");

        String[][] tests = {   // patient, department, doctorIndex, slot
            {"P10001", "Cardiology", "0", "09:30"},
            {"P1",     "Cardiology", "0", "10:00"},
            {"P10002", "Cardiology", "0", "09:30"},
            {"P10003", "ENT",        "0", "09:30"},
        };
        String[] names = {"T1 valid booking", "T2 bad patient ID", "T3 same doctor + slot again", "T4 another doctor, same slot"};
        for (int i = 0; i < tests.length; i++) {
            String[] t = tests[i];
            SwingUtilities.invokeAndWait(() -> {
                w.patientId.setText(t[0]); w.department.setSelectedItem(t[1]);
                w.doctor.setSelectedIndex(Integer.parseInt(t[2])); w.slot.setSelectedItem(t[3]); w.book.doClick();
            });
            System.out.printf("%-30s -> %s%n", names[i], w.status.getText());
            Shot.take(w, "screenshots/" + (i + 3) + "_" + names[i].substring(0, 2) + ".png");
        }

        // Close while a booking is in progress -> confirm dialog -> answer NO, then close for real
        SwingUtilities.invokeAndWait(() -> { w.patientId.setText("P10009"); w.department.setSelectedItem("ENT"); });
        SwingUtilities.invokeLater(() -> w.dispatchEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING)));
        Thread.sleep(1200);
        for (Window x : Window.getWindows())
            if (x instanceof JDialog d && d.isShowing()) {
                Shot.take(d, "screenshots/7_confirm_before_close.png");
                SwingUtilities.invokeAndWait(() -> clickButton(d, "No"));
            }
        Thread.sleep(300);
        System.out.println("After answering NO, window still showing: " + w.isShowing());
        SwingUtilities.invokeAndWait(() -> { w.onClear(); });
        SwingUtilities.invokeAndWait(() -> w.dispatchEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING)));
        Thread.sleep(500);

        System.out.println("\nEvent trace from open to close:");
        for (String s : trace) System.out.println("  " + s);

        timing();
        System.exit(0);
    }

    static String itemsOf(AppointmentDesk w) {
        List<String> l = new ArrayList<>();
        for (int i = 0; i < w.doctor.getItemCount(); i++) l.add(w.doctor.getItemAt(i).name());
        return l.toString();
    }

    static void clickButton(java.awt.Container c, String text) {
        for (java.awt.Component k : c.getComponents()) {
            if (k instanceof javax.swing.JButton b && b.getText().equals(text)) { b.doClick(); return; }
            if (k instanceof java.awt.Container cc) clickButton(cc, text);
        }
    }

    /** Time per department change: HashMap lookup vs SELECT ... WHERE department = ?, for d departments x (m/d) doctors. */
    static void timing() throws Exception {
        System.out.println("\nTime per department change (median of 5 x 500 changes):");
        System.out.printf("%6s %8s %16s %18s %18s%n", "d", "m", "Map lookup (us)", "DB query (us)", "Map memory (KB)");
        for (int[] dm : new int[][]{{10, 50}, {50, 1_000}, {200, 10_000}}) {
            int d = dm[0], m = dm[1];
            AppointmentDesk.seed(d, m / d);
            Runtime rt = Runtime.getRuntime(); System.gc(); long before = rt.totalMemory() - rt.freeMemory();
            AppointmentDesk.MapDirectory map = new AppointmentDesk.MapDirectory();
            System.gc(); long after = rt.totalMemory() - rt.freeMemory();
            AppointmentDesk.DbDirectory db = new AppointmentDesk.DbDirectory();
            double[] tm = new double[5], tq = new double[5];
            for (int r = -1; r < 5; r++) {
                long a = System.nanoTime(); int n = 0;
                for (int i = 0; i < 500; i++) n += map.doctorsIn("Dept" + (i % d)).size();
                long b = System.nanoTime();
                for (int i = 0; i < 500; i++) n += db.doctorsIn("Dept" + (i % d)).size();
                long c = System.nanoTime();
                if (r >= 0) { tm[r] = (b - a) / 500e3; tq[r] = (c - b) / 500e3; }
                if (n < 0) System.out.println();
            }
            Arrays.sort(tm); Arrays.sort(tq);
            System.out.printf("%6d %8d %16.2f %18.1f %18d%n", d, m, tm[2], tq[2], Math.max(0, after - before) / 1024);
            db.c.close();
        }
        AppointmentDesk.seed(0, 0);
    }
}
