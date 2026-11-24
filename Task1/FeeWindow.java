import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/**
 * Task 1.2 - the window only collects input and shows results. One small listener per button.
 * Slow work (bank + database) runs in a SwingWorker, never on the event-dispatch thread.
 */
public class FeeWindow extends JFrame {
    final JTextField regNo = new JTextField(12);
    final JTextField amount = new JTextField(12);
    final JButton pay = new JButton("Pay"), clear = new JButton("Clear"), receipt = new JButton("Receipt");
    final JLabel message = new JLabel("Enter register number and amount.");
    private final FeeService service;

    public FeeWindow(FeeService service) {
        super("Fee Payment");
        this.service = service;
        JPanel form = new JPanel(new GridLayout(2, 2, 6, 6));
        form.add(new JLabel("Register number:")); form.add(regNo);
        form.add(new JLabel("Amount (Rs):"));     form.add(amount);
        JPanel buttons = new JPanel();
        buttons.add(pay); buttons.add(clear); buttons.add(receipt);
        message.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        root.add(form, BorderLayout.NORTH); root.add(buttons, BorderLayout.CENTER); root.add(message, BorderLayout.SOUTH);
        setContentPane(root);

        pay.addActionListener(e -> onPay());               // one handler per button: no string comparison
        clear.addActionListener(e -> onClear());
        receipt.addActionListener(e -> onReceipt());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (!pay.isEnabled()) { show("A payment is in progress - please wait.", false); return; }
                dispose();                                  // repository/connection pool closed by the application on exit
            }
        });
        pack();
    }

    void onPay() {
        try {
            service.validate(regNo.getText(), amount.getText());          // fast, on the EDT
        } catch (FeeService.ValidationException ex) {
            show(ex.getMessage(), false);
            return;
        }
        setBusy(true);
        show("Contacting bank... please wait.", true);
        String r = regNo.getText(), a = amount.getText();
        new SwingWorker<FeeService.Payment, Void>() {
            @Override protected FeeService.Payment doInBackground() throws Exception {
                return service.pay(r, a);                                 // slow, on a worker thread
            }
            @Override protected void done() {                             // back on the EDT
                setBusy(false);
                try {
                    FeeService.Payment p = get();
                    show("Paid Rs " + p.amount() + " for " + p.regNo() + ". Receipt " + p.receiptNo(), true);
                } catch (ExecutionException ex) {
                    show("Payment failed: " + ex.getCause().getMessage() + ". You were not charged twice; try again.", false);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    void onClear() { regNo.setText(""); amount.setText(""); show("Cleared.", true); regNo.requestFocusInWindow(); }

    void onReceipt() {
        try {
            Optional<FeeService.Payment> p = service.receipt(regNo.getText());
            if (p.isEmpty()) { show("No payment found for " + regNo.getText().trim() + ".", false); return; }
            FeeService.Payment x = p.get();
            JOptionPane.showMessageDialog(this, "Receipt " + x.receiptNo() + "\nRegister no: " + x.regNo()
                    + "\nAmount: Rs " + x.amount() + "\nPaid at: " + x.paidAt(), "Fee Receipt", JOptionPane.INFORMATION_MESSAGE);
        } catch (FeeService.ValidationException ex) {
            show(ex.getMessage(), false);
        } catch (Exception ex) {
            show("Could not load receipt: " + ex.getMessage(), false);
        }
    }

    void setBusy(boolean b) { pay.setEnabled(!b); clear.setEnabled(!b); receipt.setEnabled(!b); }

    void show(String text, boolean ok) { message.setText(text); message.setForeground(ok ? new Color(0, 110, 0) : Color.RED); pack(); }

    public static void main(String[] args) {
        FeeService s = new FeeService(new FeeService.InMemoryRepo(), new FeeService.SlowBank(3000));
        javax.swing.SwingUtilities.invokeLater(() -> new FeeWindow(s).setVisible(true));
    }
}
