package registration.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Data-access layer: the ONLY class that knows SQL. Every statement is a PreparedStatement in try-with-resources. */
public class RegistrationDao {

    public record Event(int id, String name, String venue, String date, int capacity) {
        @Override public String toString() { return name; }
    }
    public record Registration(long id, String regNo, String name, String email, String department, String event, Timestamp at) { }

    /** Thrown when the UNIQUE(reg_no, event_id) constraint rejects a row. */
    public static class DuplicateRegistrationException extends Exception {
        public DuplicateRegistrationException(String m, Throwable c) { super(m, c); }
    }

    private final String url, user, password;

    public RegistrationDao(String url, String user, String password) { this.url = url; this.user = user; this.password = password; }

    public static RegistrationDao fromSystemProperties() {
        return new RegistrationDao(System.getProperty("db.url", "jdbc:mariadb://localhost:3306/campus"),
                System.getProperty("db.user", "student"), System.getProperty("db.password", "student123"));
    }

    private Connection open() throws SQLException { return DriverManager.getConnection(url, user, password); }

    /** Runs sql/schema.sql (statements separated by ';'), skipping comments. */
    public void runScript(String script) throws SQLException {
        try (Connection c = open(); Statement s = c.createStatement()) {
            for (String stmt : script.replaceAll("(?m)^--.*$", "").split(";")) {
                String t = stmt.trim();
                if (!t.isEmpty() && !t.toUpperCase().startsWith("CREATE DATABASE") && !t.toUpperCase().startsWith("USE ")) s.execute(t);
            }
        }
    }

    public List<Event> events() throws SQLException {
        List<Event> out = new ArrayList<>();
        try (Connection c = open(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, name, venue, event_date, capacity FROM events ORDER BY event_date, name")) {
            while (rs.next()) out.add(new Event(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5)));
        }
        return out;
    }

    public boolean exists(String regNo, int eventId) throws SQLException {
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement("SELECT 1 FROM registrations WHERE reg_no = ? AND event_id = ?")) {
            ps.setString(1, regNo); ps.setInt(2, eventId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public int countFor(int eventId) throws SQLException {
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM registrations WHERE event_id = ?")) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    public long insert(String regNo, String name, String email, String dept, int eventId) throws SQLException, DuplicateRegistrationException {
        String sql = "INSERT INTO registrations (reg_no, student_name, email, department, event_id) VALUES (?,?,?,?,?)";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, regNo); ps.setString(2, name); ps.setString(3, email); ps.setString(4, dept); ps.setInt(5, eventId);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); return k.getLong(1); }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DuplicateRegistrationException("Already registered for this event", e);
        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) throw new DuplicateRegistrationException("Already registered for this event", e);
            throw e;
        }
    }

    /** One page, newest first. eventId <= 0 means all events. */
    public List<Registration> page(int eventId, int limit, int offset) throws SQLException {
        String sql = "SELECT r.id, r.reg_no, r.student_name, r.email, r.department, e.name, r.registered_at "
                   + "FROM registrations r JOIN events e ON e.id = r.event_id "
                   + (eventId > 0 ? "WHERE r.event_id = ? " : "") + "ORDER BY r.id DESC LIMIT ? OFFSET ?";
        List<Registration> out = new ArrayList<>(limit);
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            if (eventId > 0) ps.setInt(i++, eventId);
            ps.setInt(i++, limit); ps.setInt(i, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new Registration(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getTimestamp(7)));
            }
        }
        return out;
    }

    public int count(int eventId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM registrations" + (eventId > 0 ? " WHERE event_id = ?" : "");
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (eventId > 0) ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    /** Bulk loader used only by the performance experiment. */
    public void bulkInsert(int n, List<Event> events) throws SQLException {
        try (Connection c = open()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO registrations (reg_no, student_name, email, department, event_id) VALUES (?,?,?,?,?)")) {
                for (int i = 0; i < n; i++) {
                    ps.setString(1, String.format("%09d", 300_000_000 + i)); ps.setString(2, "Student " + i);
                    ps.setString(3, "s" + i + "@simats.edu"); ps.setString(4, i % 2 == 0 ? "CSE" : "IT");
                    ps.setInt(5, events.get(i % events.size()).id()); ps.addBatch();
                    if (i % 5000 == 4999) ps.executeBatch();
                }
                ps.executeBatch();
            }
            c.commit();
        }
    }
}
