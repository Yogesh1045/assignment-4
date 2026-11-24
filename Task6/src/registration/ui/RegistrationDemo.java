package registration.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import registration.dao.RegistrationDao;
import registration.dao.RegistrationDao.Event;
import registration.dao.RegistrationDao.Registration;
import registration.service.RegistrationService;

/** Task 6 test driver: creates the schema, runs the test cases through the real window, takes screenshots, measures paging. */
public class RegistrationDemo {
    static RegistrationApp app;

    static void waitIdle(RegistrationApp a) throws Exception {
        for (int i = 0; i < 100; i++) { Thread.sleep(50); if (a.register.isEnabled() && !a.listStatus.getText().startsWith("Loading")) break; }
        Thread.sleep(200);
    }

    static void fill(String r, String n, String e, String d, int eventIndex) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            app.regNo.setText(r); app.name.setText(n); app.email.setText(e); app.dept.setSelectedItem(d);
            app.event.setSelectedIndex(eventIndex);
            app.register.doClick();
        });
        waitIdle(app);
    }

    public static void main(String[] args) throws Exception {
        RegistrationDao dao = RegistrationDao.fromSystemProperties();
        dao.runScript(Files.readString(Path.of("sql/schema.sql")));
        System.out.println("Schema created from sql/schema.sql");
        RegistrationService service = new RegistrationService(dao);

        SwingUtilities.invokeAndWait(() -> { app = new RegistrationApp(service); app.setLocation(0, 0); app.setVisible(true); });
        Thread.sleep(1500);
        Shot.take(app, "screenshots/1_register_screen.png");

        Object[][] tests = {
            {"T1 normal registration",         "192311001", "Arun Kumar", "arun@simats.edu", "CSE", 0},
            {"T2 normal (other event)",        "192311001", "Arun Kumar", "arun@simats.edu", "CSE", 2},
            {"T3 normal (other student)",      "192311002", "Divya R",    "divya@simats.edu", "IT", 0},
            {"T4 invalid register no",         "19231",     "Kiran",      "kiran@simats.edu", "ECE", 0},
            {"T5 invalid e-mail",              "192311003", "Kiran",      "kiran.simats.edu", "ECE", 0},
            {"T6 no event chosen",             "192311003", "Kiran",      "kiran@simats.edu", "ECE", -1},
            {"T7 DUPLICATE (same student+event)","192311001","Arun Kumar", "arun@simats.edu", "CSE", 0},
        };
        int shot = 2;
        for (Object[] t : tests) {
            fill((String) t[1], (String) t[2], (String) t[3], (String) t[4], (Integer) t[5]);
            System.out.printf("%-36s -> %s%n", t[0], app.formStatus.getText());
            if (t[0].toString().startsWith("T1") || t[0].toString().startsWith("T5") || t[0].toString().startsWith("T7"))
                Shot.take(app, "screenshots/" + (shot++) + "_" + t[0].toString().substring(0, 2) + ".png");
        }

        // Bypass the friendly pre-check: the UNIQUE constraint must still refuse a duplicate
        try { dao.insert("192311002", "Divya R", "divya@simats.edu", "IT", service.events().get(0).id()); System.out.println("DB constraint: duplicate ACCEPTED (bug)"); }
        catch (RegistrationDao.DuplicateRegistrationException e) { System.out.println("DB constraint check: direct duplicate INSERT rejected by UNIQUE(reg_no, event_id)"); }

        // Registrations screen, filter, export
        SwingUtilities.invokeAndWait(() -> { app.cards.show(app.screens, "list"); app.pageNo = 0; app.loadPage(); });
        waitIdle(app);
        System.out.println("All events      -> " + app.listStatus.getText() + ", table rows = " + app.model.getRowCount());
        Shot.take(app, "screenshots/" + (shot++) + "_all_registrations.png");
        SwingUtilities.invokeAndWait(() -> app.filter.setSelectedIndex(1));
        waitIdle(app);
        System.out.println("Filter event #1 -> " + app.listStatus.getText() + ", table rows = " + app.model.getRowCount());
        Shot.take(app, "screenshots/" + (shot++) + "_filtered.png");
        SwingUtilities.invokeAndWait(() -> app.onExport(Path.of("registrations_export.csv")));
        waitIdle(app);
        System.out.println("Export          -> " + app.listStatus.getText());
        System.out.println("CSV content:\n" + Files.readString(Path.of("registrations_export.csv")).indent(4));
        Files.deleteIfExists(Path.of("registrations_export.csv"));

        paging(dao, service);
        SwingUtilities.invokeAndWait(app::dispose);
        System.exit(0);
    }

    /** 100,000 registrations: load everything into the JTable on the EDT vs one page in a SwingWorker. */
    static void paging(RegistrationDao dao, RegistrationService service) throws Exception {
        List<Event> events = service.events();
        long t0 = System.nanoTime();
        dao.bulkInsert(100_000, events);
        System.out.printf("%nInserted 100,000 registrations in %.1f s (batch of 5,000)%n", (System.nanoTime() - t0) / 1e9);
        Runtime rt = Runtime.getRuntime();

        // A) everything at once, on the EDT (what a naive handler does)
        System.gc(); long m0 = rt.totalMemory() - rt.freeMemory();
        DefaultTableModel all = new DefaultTableModel(new String[]{"ID", "Reg No", "Name", "E-mail", "Dept", "Event", "At"}, 0);
        long[] edtBlocked = new long[1];
        SwingUtilities.invokeAndWait(() -> {
            long a = System.nanoTime();
            try {
                for (Registration r : dao.page(0, 1_000_000, 0)) all.addRow(new Object[]{r.id(), r.regNo(), r.name(), r.email(), r.department(), r.event(), r.at()});
            } catch (Exception e) { throw new RuntimeException(e); }
            edtBlocked[0] = System.nanoTime() - a;
        });
        System.gc(); long m1 = rt.totalMemory() - rt.freeMemory();
        System.out.printf("A) load all 100,000 rows on the EDT : %6.0f ms, EDT blocked the whole time (window frozen), rows held %,d, memory ~%,d KB%n",
                edtBlocked[0] / 1e6, all.getRowCount(), (m1 - m0) / 1024);
        all.setRowCount(0);

        // B) one page of 50 in a SwingWorker, probing EDT latency while it loads
        SwingUtilities.invokeAndWait(() -> { app.filter.setSelectedIndex(0); });
        waitIdle(app);
        System.gc(); long m2 = rt.totalMemory() - rt.freeMemory();
        long b0 = System.nanoTime();
        SwingUtilities.invokeAndWait(() -> { app.pageNo = 1000; app.loadPage(); });      // page 1,001 -> OFFSET 50,000
        long worst = 0;
        while (app.listStatus.getText().startsWith("Loading")) {
            long q = System.nanoTime(); SwingUtilities.invokeAndWait(() -> { }); worst = Math.max(worst, System.nanoTime() - q);
            Thread.sleep(2);
        }
        long b1 = System.nanoTime();
        System.gc(); long m3 = rt.totalMemory() - rt.freeMemory();
        System.out.printf("B) one page (LIMIT 50 OFFSET 50,000) : %6.0f ms, EDT worst response %.1f ms, rows held %d, memory ~%,d KB%n",
                (b1 - b0) / 1e6, worst / 1e6, app.model.getRowCount(), Math.max(0, m3 - m2) / 1024);
        System.out.println("   status: " + app.listStatus.getText());
        Shot.take(app, "screenshots/9_paged_100k.png");
        for (int off : new int[]{0, 10_000, 50_000, 99_950}) {
            long a = System.nanoTime(); dao.page(0, 50, off); long b = System.nanoTime();
            System.out.printf("   LIMIT 50 OFFSET %,6d : %6.1f ms%n", off, (b - a) / 1e6);
        }
    }
}
