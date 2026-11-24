import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Task 1.2 - all fee logic, no GUI code. The window only calls these methods.
 * Storage and the bank are interfaces, so tests can use fakes and production uses JDBC / a real gateway.
 */
public class FeeService {

    /** Thrown for input the user must correct; the message is shown to the user as-is. */
    public static class ValidationException extends Exception {
        public ValidationException(String msg) { super(msg); }
    }

    public record Payment(String receiptNo, String regNo, BigDecimal amount, LocalDateTime paidAt) { }

    /** Data-access boundary (production: JDBC with PreparedStatement, in try-with-resources). */
    public interface FeeRepository {
        void save(Payment p) throws Exception;
        Optional<Payment> lastPayment(String regNo) throws Exception;
    }

    /** Bank boundary (production: HTTP call to the payment gateway, may take seconds). */
    public interface PaymentGateway {
        String charge(String regNo, BigDecimal amount) throws Exception;    // returns bank reference
    }

    static final Pattern REG_NO = Pattern.compile("\\d{9}");
    static final BigDecimal MAX = new BigDecimal("200000");

    private final FeeRepository repo;
    private final PaymentGateway bank;

    public FeeService(FeeRepository repo, PaymentGateway bank) { this.repo = repo; this.bank = bank; }

    /** Pure validation: no I/O, so it is trivially unit-testable. */
    public BigDecimal validate(String regNo, String amountText) throws ValidationException {
        if (regNo == null || regNo.isBlank()) throw new ValidationException("Enter the register number.");
        if (!REG_NO.matcher(regNo.trim()).matches()) throw new ValidationException("Register number must be 9 digits.");
        if (amountText == null || amountText.isBlank()) throw new ValidationException("Enter the amount.");
        BigDecimal a;
        try { a = new BigDecimal(amountText.trim()); }
        catch (NumberFormatException e) { throw new ValidationException("Amount must be a number, e.g. 45000 or 45000.50."); }
        if (a.signum() <= 0) throw new ValidationException("Amount must be greater than zero.");
        if (a.scale() > 2) throw new ValidationException("Amount can have at most 2 decimal places.");
        if (a.compareTo(MAX) > 0) throw new ValidationException("Amount exceeds the maximum of Rs 2,00,000.");
        return a;
    }

    /** Slow: talks to the bank and the database. The GUI must call this OFF the event-dispatch thread. */
    public Payment pay(String regNo, String amountText) throws Exception {
        BigDecimal amount = validate(regNo, amountText);
        String bankRef = bank.charge(regNo.trim(), amount);
        Payment p = new Payment("RCPT-" + bankRef, regNo.trim(), amount, LocalDateTime.now().withNano(0));
        repo.save(p);
        return p;
    }

    public Optional<Payment> receipt(String regNo) throws Exception {
        if (regNo == null || !REG_NO.matcher(regNo.trim()).matches()) throw new ValidationException("Enter a valid 9-digit register number.");
        return repo.lastPayment(regNo.trim());
    }

    /** In-memory repository used by the demo and the tests. */
    public static class InMemoryRepo implements FeeRepository {
        final List<Payment> rows = new ArrayList<>();
        @Override public synchronized void save(Payment p) { rows.add(p); }
        @Override public synchronized Optional<Payment> lastPayment(String regNo) {
            for (int i = rows.size() - 1; i >= 0; i--) if (rows.get(i).regNo().equals(regNo)) return Optional.of(rows.get(i));
            return Optional.empty();
        }
    }

    /** Fake bank: waits like a real gateway, returns a reference number. */
    public static class SlowBank implements PaymentGateway {
        final long delayMs; int counter = 5000;
        public SlowBank(long delayMs) { this.delayMs = delayMs; }
        @Override public synchronized String charge(String regNo, BigDecimal amount) throws InterruptedException {
            Thread.sleep(delayMs);
            return String.valueOf(++counter);
        }
    }
}
