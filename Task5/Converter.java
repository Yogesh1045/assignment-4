import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class Converter extends JFrame {
    private final JTextField amount = new JTextField(10);
    private final JComboBox<String> target =
            new JComboBox<>(new String[]{"USD", "EUR"});
    private final JLabel result = new JLabel(" ");

    private static double rate(String currency) {
        return currency.equals("USD") ? 0.012 : 0.011;
    }

    public Converter() {
        super("Currency Converter");
        setLayout(new FlowLayout());
        JButton convertButton = new JButton("Convert");
        add(new JLabel("INR")); add(amount); add(target);
        add(convertButton); add(result);
        // TODO 1 (completed): Register ONE lambda listener for both the Convert button and the
        // Enter key in the amount field
        ActionListener onConvert = e -> convert();     // one listener object ...
        convertButton.addActionListener(onConvert);     // ... for the button
        amount.addActionListener(onConvert);            // ... and Enter in the text field
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
    }

    private void convert() {
        // TODO 2 (completed): Parse the amount and show the converted value with 2 decimals, or
        // "Invalid amount" if parsing fails
        try {
            double inr = Double.parseDouble(amount.getText().trim());
            String cur = (String) target.getSelectedItem();
            result.setText(String.format("%.2f %s", inr * rate(cur), cur));
        } catch (NumberFormatException ex) {
            result.setText("Invalid amount");
        }
        pack();                                         // let the frame fit the new label text
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Converter().setVisible(true));
    }
}
