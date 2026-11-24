package registration.service;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import registration.dao.RegistrationDao;
import registration.dao.RegistrationDao.Event;
import registration.dao.RegistrationDao.Registration;

/** Service layer: validation, duplicate rule, capacity rule, export. No Swing, no SQL text. */
public class RegistrationService {

    /** Input the user must correct; message is shown as-is. */
    public static class InvalidInputException extends Exception { public InvalidInputException(String m) { super(m); } }
    public static class DuplicateException extends Exception { public DuplicateException(String m) { super(m); } }
    public static class EventFullException extends Exception { public EventFullException(String m) { super(m); } }

    static final Pattern REG = Pattern.compile("\\d{9}");
    static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z .']{1,79}");
    static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    private final RegistrationDao dao;
    public RegistrationService(RegistrationDao dao) { this.dao = dao; }

    public List<Event> events() throws Exception { return dao.events(); }

    /** Fast, pure checks - safe to run on the event-dispatch thread. */
    public void validate(String regNo, String name, String email, String dept, Event event) throws InvalidInputException {
        if (regNo == null || !REG.matcher(regNo.trim()).matches()) throw new InvalidInputException("Register number must be exactly 9 digits.");
        if (name == null || !NAME.matcher(name.trim()).matches()) throw new InvalidInputException("Enter the student's full name (letters only).");
        if (email == null || !EMAIL.matcher(email.trim()).matches()) throw new InvalidInputException("Enter a valid e-mail address, e.g. name@college.edu.");
        if (dept == null || dept.isBlank()) throw new InvalidInputException("Choose a department.");
        if (event == null) throw new InvalidInputException("Choose an event.");
    }

    /** Slow (database) - call from a SwingWorker. Returns the new registration id. */
    public long register(String regNo, String name, String email, String dept, Event event)
            throws InvalidInputException, DuplicateException, EventFullException, Exception {
        validate(regNo, name, email, dept, event);
        String r = regNo.trim();
        if (dao.exists(r, event.id()))                           // friendly early check ...
            throw new DuplicateException(r + " is already registered for " + event.name() + ".");
        if (dao.countFor(event.id()) >= event.capacity())
            throw new EventFullException(event.name() + " is full (" + event.capacity() + " seats).");
        try {
            return dao.insert(r, name.trim(), email.trim(), dept, event.id());
        } catch (RegistrationDao.DuplicateRegistrationException e) {   // ... and the UNIQUE constraint catches the race
            throw new DuplicateException(r + " is already registered for " + event.name() + ".");
        }
    }

    public List<Registration> page(Event filter, int pageSize, int pageNo) throws Exception {
        return dao.page(filter == null ? 0 : filter.id(), pageSize, pageNo * pageSize);
    }

    public int count(Event filter) throws Exception { return dao.count(filter == null ? 0 : filter.id()); }

    /** Exports every matching registration to CSV, streaming page by page (constant memory). */
    public int exportCsv(Event filter, Path file) throws Exception {
        int written = 0, pageNo = 0;
        try (Writer w = Files.newBufferedWriter(file)) {
            w.write("id,reg_no,name,email,department,event,registered_at\n");
            List<Registration> page;
            while (!(page = page(filter, 1000, pageNo++)).isEmpty()) {
                for (Registration r : page) {
                    w.write(r.id() + "," + r.regNo() + "," + csv(r.name()) + "," + csv(r.email()) + "," + r.department() + "," + csv(r.event()) + "," + r.at() + "\n");
                    written++;
                }
            }
        } catch (IOException e) {
            throw new IOException("Could not write " + file + ": " + e.getMessage(), e);
        }
        return written;
    }

    static String csv(String s) { return s.contains(",") || s.contains("\"") ? "\"" + s.replace("\"", "\"\"") + "\"" : s; }
}
