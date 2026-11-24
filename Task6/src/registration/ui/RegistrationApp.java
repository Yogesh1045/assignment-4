package registration.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import registration.dao.RegistrationDao;
import registration.dao.RegistrationDao.Event;
import registration.dao.RegistrationDao.Registration;
import registration.service.RegistrationService;

/**
 * UI layer: JFrame with JMenuBar and a CardLayout holding two screens (Register, Registrations).
 * Every database call runs in a SwingWorker; the EDT only reads fields and updates components.
 */
public class RegistrationApp extends JFrame {
    static final int PAGE = 50;

    final RegistrationService service;
    final CardLayout cards = new CardLayout();
    final JPanel screens = new JPanel(cards);

    // Register screen
    final JTextField regNo = new JTextField(12), name = new JTextField(18), email = new JTextField(18);
    final JComboBox<String> dept = new JComboBox<>(new String[]{"CSE", "IT", "ECE", "EEE", "MECH", "CIVIL"});
    final JComboBox<Event> event = new JComboBox<>();
    final JButton register = new JButton("Register");
    final JLabel formStatus = new JLabel(" ");

    // Registrations screen
    final JComboBox<Object> filter = new JComboBox<>();
    final DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Reg No", "Name", "E-mail", "Dept", "Event", "Registered at"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    final JTable table = new JTable(model);
    final JButton prev = new JButton("< Prev"), next = new JButton("Next >");
    final JLabel listStatus = new JLabel(" ");
    int pageNo = 0, total = 0;

    public RegistrationApp(RegistrationService service) {
        super("Campus Event Registration");
        this.service = service;
        setJMenuBar(menu());
        screens.add(registerScreen(), "register");
        screens.add(listScreen(), "list");
        setContentPane(screens);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(820, 460);
        loadEvents();
    }

    // ---------------------------------------------------------------- menu
    JMenuBar menu() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem export = new JMenuItem("Export CSV...");
        export.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK));
        export.addActionListener(e -> onExport(null));
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> dispose());
        file.add(export); file.addSeparator(); file.add(exit);
        JMenu view = new JMenu("View");
        JMenuItem reg = new JMenuItem("Register Student"), list = new JMenuItem("All Registrations");
        reg.addActionListener(e -> cards.show(screens, "register"));
        list.addActionListener(e -> { cards.show(screens, "list"); pageNo = 0; loadPage(); });
        view.add(reg); view.add(list);
        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> JOptionPane.showMessageDialog(this, "Campus Event Registration v1.0\nCultural Committee", "About", JOptionPane.INFORMATION_MESSAGE));
        help.add(about);
        bar.add(file); bar.add(view); bar.add(help);
        return bar;
    }

    // ---------------------------------------------------------------- screens
    JPanel registerScreen() {
        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        form.add(new JLabel("Register number:")); form.add(regNo);
        form.add(new JLabel("Student name:"));    form.add(name);
        form.add(new JLabel("E-mail:"));          form.add(email);
        form.add(new JLabel("Department:"));      form.add(dept);
        form.add(new JLabel("Event:"));           form.add(event);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(register);
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JLabel title = new JLabel("Register a student for an event");
        title.setFont(title.getFont().deriveFont(16f));
        p.add(title, BorderLayout.NORTH);
        JPanel centre = new JPanel(new BorderLayout()); centre.add(form, BorderLayout.NORTH); centre.add(buttons, BorderLayout.CENTER);
        p.add(centre, BorderLayout.CENTER);
        p.add(formStatus, BorderLayout.SOUTH);
        register.addActionListener(e -> onRegister());
        return p;
    }

    JPanel listScreen() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Filter by event:")); top.add(filter);
        JButton export = new JButton("Export CSV");
        top.add(export);
        JPanel bottom = new JPanel(new BorderLayout());
        JPanel nav = new JPanel(); nav.add(prev); nav.add(next);
        bottom.add(nav, BorderLayout.EAST); bottom.add(listStatus, BorderLayout.WEST);
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(top, BorderLayout.NORTH); p.add(new JScrollPane(table), BorderLayout.CENTER); p.add(bottom, BorderLayout.SOUTH);
        filter.addActionListener(e -> { pageNo = 0; loadPage(); });
        prev.addActionListener(e -> { if (pageNo > 0) { pageNo--; loadPage(); } });
        next.addActionListener(e -> { if ((pageNo + 1) * PAGE < total) { pageNo++; loadPage(); } });
        export.addActionListener(e -> onExport(null));
        return p;
    }

    // ---------------------------------------------------------------- handlers
    void loadEvents() {
        new SwingWorker<List<Event>, Void>() {
            @Override protected List<Event> doInBackground() throws Exception { return service.events(); }
            @Override protected void done() {
                try {
                    List<Event> ev = get();
                    event.removeAllItems(); filter.removeAllItems();
                    filter.addItem("All events");
                    for (Event e : ev) { event.addItem(e); filter.addItem(e); }
                    event.setSelectedIndex(-1);
                } catch (Exception ex) { say(formStatus, "Cannot reach the database: " + rootMessage(ex), false); }
            }
        }.execute();
    }

    void onRegister() {
        Event ev = (Event) event.getSelectedItem();
        String r = regNo.getText(), n = name.getText(), m = email.getText(), d = (String) dept.getSelectedItem();
        try {
            service.validate(r, n, m, d, ev);                                   // instant feedback, no DB
        } catch (RegistrationService.InvalidInputException ex) {
            say(formStatus, ex.getMessage(), false);
            return;
        }
        register.setEnabled(false);
        say(formStatus, "Saving...", true);
        new SwingWorker<Long, Void>() {
            @Override protected Long doInBackground() throws Exception { return service.register(r, n, m, d, ev); }
            @Override protected void done() {
                register.setEnabled(true);
                try {
                    long id = get();
                    say(formStatus, "Registered " + r.trim() + " for " + ev.name() + " (registration #" + id + ").", true);
                    regNo.setText(""); name.setText(""); email.setText(""); event.setSelectedIndex(-1);
                } catch (ExecutionException ex) {
                    Throwable c = ex.getCause();
                    if (c instanceof RegistrationService.DuplicateException || c instanceof RegistrationService.EventFullException
                            || c instanceof RegistrationService.InvalidInputException) say(formStatus, c.getMessage(), false);
                    else say(formStatus, "Database error - registration not saved: " + rootMessage(c), false);
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            }
        }.execute();
    }

    Event currentFilter() { return filter.getSelectedItem() instanceof Event e ? e : null; }

    /** Loads ONE page (LIMIT/OFFSET) in the background; the table never holds more than PAGE rows. */
    void loadPage() {
        Event f = currentFilter();
        int p = pageNo;
        prev.setEnabled(false); next.setEnabled(false);
        say(listStatus, "Loading...", true);
        new SwingWorker<List<Registration>, Void>() {
            int count;
            @Override protected List<Registration> doInBackground() throws Exception { count = service.count(f); return service.page(f, PAGE, p); }
            @Override protected void done() {
                try {
                    List<Registration> rows = get();
                    total = count;
                    model.setRowCount(0);
                    for (Registration r : rows) model.addRow(new Object[]{r.id(), r.regNo(), r.name(), r.email(), r.department(), r.event(), r.at()});
                    int from = total == 0 ? 0 : p * PAGE + 1, to = p * PAGE + rows.size();
                    say(listStatus, "Showing " + from + "-" + to + " of " + total + (f == null ? "" : " for " + f.name()), true);
                    prev.setEnabled(p > 0); next.setEnabled(to < total);
                } catch (Exception ex) { say(listStatus, "Could not load registrations: " + rootMessage(ex), false); }
            }
        }.execute();
    }

    /** file == null -> ask with a JFileChooser. */
    void onExport(Path file) {
        Path target = file;
        if (target == null) {
            JFileChooser ch = new JFileChooser();
            ch.setSelectedFile(new java.io.File("registrations.csv"));
            if (ch.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            target = ch.getSelectedFile().toPath();
        }
        Path out = target;
        Event f = currentFilter();
        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() throws Exception { return service.exportCsv(f, out); }
            @Override protected void done() {
                try { say(listStatus, "Exported " + get() + " registrations to " + out.getFileName(), true); }
                catch (Exception ex) { say(listStatus, "Export failed: " + rootMessage(ex), false); }
            }
        }.execute();
    }

    static String rootMessage(Throwable t) { while (t.getCause() != null) t = t.getCause(); return t.getMessage(); }

    void say(JLabel l, String m, boolean ok) { l.setText(m); l.setForeground(ok ? new Color(0, 110, 0) : Color.RED); }

    public static void main(String[] args) {
        RegistrationService s = new RegistrationService(RegistrationDao.fromSystemProperties());
        SwingUtilities.invokeLater(() -> new RegistrationApp(s).setVisible(true));
    }
}
