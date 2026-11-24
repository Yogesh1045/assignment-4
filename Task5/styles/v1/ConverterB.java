import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/** Task 5 - style B: named inner class (v1: Convert button, result in label). */
public class ConverterB extends JFrame {
    final JTextField amount = new JTextField(10);
    final JComboBox<String> from = new JComboBox<>(new String[]{"INR", "USD", "EUR"});
    final JComboBox<String> to = new JComboBox<>(new String[]{"USD", "EUR", "INR"});
    final JLabel result = new JLabel(" ");
    final JButton convert = new JButton("Convert");

    /** Units of INR per one unit of the currency. */
    static double inr(String c) { return c.equals("INR") ? 1 : c.equals("USD") ? 83.0 : 90.5; }

    public ConverterB() {
        super("Converter B");
        setLayout(new FlowLayout());
        add(amount); add(from); add(new JLabel("to")); add(to); add(convert); add(result);
        convert.addActionListener(new ConvertHandler());

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dispose(); }
        });
        pack();
    }

    String compute() {
        try {
            double a = Double.parseDouble(amount.getText().trim());
            double v = a * inr((String) from.getSelectedItem()) / inr((String) to.getSelectedItem());
            return String.format("%.2f %s", v, to.getSelectedItem());
        } catch (NumberFormatException e) {
            return "Invalid amount";
        }
    }
    class ConvertHandler implements ActionListener {
        @Override public void actionPerformed(ActionEvent e) { result.setText(compute()); }
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> new ConverterB().setVisible(true));
    }
}
