import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

/**
 * Task 1 evidence: (1) FeeService unit tests without any window, (2) GUI interactions with screenshots,
 * (3) responsiveness of the event-dispatch thread during payment: sleep on the EDT vs SwingWorker,
 * (4) dispatch cost: one listener with an if-else chain vs one listener per button.
 */
public class FeeDemo {
    static int pass, fail;
    static void check(String name, boolean ok) { if (ok) pass++; else fail++; System.out.println("   " + (ok ? "PASS" : "FAIL") + "  " + name); }

    static String msg(FeeService s, String r, String a) {
        try { s.validate(r, a); return "OK"; } catch (FeeService.ValidationException e) { return e.getMessage(); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("1) FeeService unit tests (no GUI, fake bank 0 ms):");
        FeeService.InMemoryRepo repo = new FeeService.InMemoryRepo();
        FeeService s = new FeeService(repo, new FeeService.SlowBank(0));
        check("empty register no rejected", msg(s, "", "100").startsWith("Enter the register"));
        check("8-digit register no rejected", msg(s, "19231100", "100").contains("9 digits"));
        check("letters in amount rejected", msg(s, "192311001", "45k").contains("must be a number"));
        check("negative amount rejected", msg(s, "192311001", "-5").contains("greater than zero"));
        check("3 decimal places rejected", msg(s, "192311001", "10.555").contains("2 decimal"));
        check("over limit rejected", msg(s, "192311001", "250000").contains("maximum"));
        check("SQL-injection text is just an invalid register no", msg(s, "1'); DROP TABLE fees;--", "1").contains("9 digits"));
        FeeService.Payment p = s.pay("192311001", "45000.50");
        check("valid payment saved with exact amount", repo.rows.size() == 1 && p.amount().equals(new BigDecimal("45000.50")));
        check("receipt returns the last payment", s.receipt("192311001").orElseThrow().receiptNo().equals(p.receiptNo()));
        check("receipt for unknown student is empty", s.receipt("192311999").isEmpty());
        System.out.println("   " + pass + "/" + (pass + fail) + " passed");

        System.out.println("\n2) GUI interactions (bank takes 3 s):");
        FeeService gui = new FeeService(new FeeService.InMemoryRepo(), new FeeService.SlowBank(3000));
        FeeWindow[] w = new FeeWindow[1];
        SwingUtilities.invokeAndWait(() -> { w[0] = new FeeWindow(gui); w[0].setLocation(0, 0); w[0].setVisible(true); });
        FeeWindow f = w[0];
        SwingUtilities.invokeAndWait(() -> { f.regNo.setText("192311001"); f.amount.setText("45k"); f.pay.doClick(); });
        System.out.println("   invalid amount -> '" + f.message.getText() + "'");
        Shot.take(f, "screenshots/1_invalid_amount.png");
        SwingUtilities.invokeAndWait(() -> { f.amount.setText("45000"); f.pay.doClick(); });
        Thread.sleep(300);
        System.out.println("   during payment -> '" + f.message.getText() + "', Pay enabled=" + f.pay.isEnabled());
        Shot.take(f, "screenshots/2_payment_in_progress.png");
        long start = System.nanoTime(), worst = 0;
        for (int i = 0; i < 25; i++) {                       // probe EDT latency while the bank call runs
            long t0 = System.nanoTime(); SwingUtilities.invokeAndWait(() -> { }); worst = Math.max(worst, System.nanoTime() - t0);
            Thread.sleep(100);
        }
        System.out.printf("   EDT worst response time during the 3 s payment (SwingWorker): %.1f ms%n", worst / 1e6);
        while (!f.pay.isEnabled()) Thread.sleep(100);
        System.out.println("   after payment  -> '" + f.message.getText() + "'");
        Shot.take(f, "screenshots/3_payment_done.png");
        SwingUtilities.invokeLater(f.receipt::doClick);
        Thread.sleep(1000);
        for (Window x : Window.getWindows()) if (x instanceof JDialog d && d.isShowing()) { Shot.take(d, "screenshots/4_receipt_dialog.png"); SwingUtilities.invokeLater(d::dispose); }
        Thread.sleep(300);

        System.out.println("\n3) Same 3 s wait done ON the EDT (original design):");
        long t0 = System.nanoTime();
        SwingUtilities.invokeLater(() -> { try { Thread.sleep(3000); } catch (InterruptedException e) { } });
        Thread.sleep(50);
        long q0 = System.nanoTime(); SwingUtilities.invokeAndWait(() -> { }); long blocked = System.nanoTime() - q0;
        System.out.printf("   a repaint/click queued 50 ms after Pay waited %.0f ms (window frozen, 'Not responding')%n", blocked / 1e6);
        SwingUtilities.invokeAndWait(f::dispose);

        dispatchCost();
        System.exit(0);
    }

    /** k buttons: one listener comparing action commands (k string compares worst case) vs a listener per button. */
    static void dispatchCost() {
        System.out.println("\n4) Event dispatch cost, last button of k (avg of 2,000,000 events, ns):");
        System.out.printf("   %4s %24s %20s%n", "k", "one listener + if-else", "listener per button");
        for (int k : new int[]{3, 10, 30, 100}) {
            String[] cmds = new String[k];
            for (int i = 0; i < k; i++) cmds[i] = new String("Button" + i);    // distinct String objects, as from getActionCommand()
            int[] hits = new int[k];
            ActionListener chain = e -> {
                String c = e.getActionCommand();
                for (int i = 0; i < k; i++) if (c.equals(cmds[i])) { hits[i]++; return; }   // the if-else chain
            };
            Map<Object, ActionListener> perButton = new HashMap<>();
            Object[] sources = new Object[k];
            for (int i = 0; i < k; i++) { final int id = i; sources[i] = new Object(); perButton.put(sources[i], e -> hits[id]++); }
            ActionEvent last = new ActionEvent(sources[k - 1], ActionEvent.ACTION_PERFORMED, new String("Button" + (k - 1)));
            ActionListener direct = perButton.get(sources[k - 1]);            // the button holds its own listener reference
            for (int i = 0; i < 200_000; i++) { chain.actionPerformed(last); direct.actionPerformed(last); }
            long a0 = System.nanoTime(); for (int i = 0; i < 2_000_000; i++) chain.actionPerformed(last); long a1 = System.nanoTime();
            for (int i = 0; i < 2_000_000; i++) direct.actionPerformed(last); long a2 = System.nanoTime();
            System.out.printf("   %4d %24.1f %20.1f%n", k, (a1 - a0) / 2e6, (a2 - a1) / 2e6);
        }
    }
}
