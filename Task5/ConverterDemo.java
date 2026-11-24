import java.awt.Component;
import java.awt.Window;
import java.lang.reflect.Field;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Task 5 test driver: valid / invalid / Enter key for the Complete-the-Code version, a v2 dialog, the shared Action. */
public class ConverterDemo {

    static <T> T field(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name); f.setAccessible(true);
        @SuppressWarnings("unchecked") T t = (T) f.get(o); return t;
    }

    static JButton findButton(java.awt.Container c, String text) {
        for (Component k : c.getComponents()) {
            if (k instanceof JButton b && text.equals(b.getText())) return b;
            if (k instanceof java.awt.Container cc) { JButton b = findButton(cc, text); if (b != null) return b; }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        Converter[] holder = new Converter[1];
        SwingUtilities.invokeAndWait(() -> { holder[0] = new Converter(); holder[0].setLocation(0, 0); holder[0].setVisible(true); });
        Converter c = holder[0];
        JTextField amount = field(c, "amount"); JLabel result = field(c, "result"); JComboBox<String> target = field(c, "target");
        JButton btn = findButton(c.getContentPane(), "Convert");

        SwingUtilities.invokeAndWait(() -> { amount.setText("2500"); target.setSelectedItem("USD"); btn.doClick(); });
        System.out.println("T1 valid (button)  : 2500 INR -> '" + result.getText() + "'  expected '30.00 USD'");
        Shot.take(c, "screenshots/1_valid_conversion.png");

        SwingUtilities.invokeAndWait(() -> { amount.setText("12a5"); btn.doClick(); });
        System.out.println("T2 invalid (button): '12a5' -> '" + result.getText() + "'  expected 'Invalid amount'");
        Shot.take(c, "screenshots/2_invalid_amount.png");

        SwingUtilities.invokeAndWait(() -> { amount.setText("1000"); target.setSelectedItem("EUR"); amount.postActionEvent(); });   // Enter
        System.out.println("T3 Enter key       : 1000 INR -> '" + result.getText() + "'  expected '11.00 EUR'");
        Shot.take(c, "screenshots/3_enter_key.png");

        SwingUtilities.invokeAndWait(() -> { amount.setText(""); btn.doClick(); });
        System.out.println("T4 empty field     : '' -> '" + result.getText() + "'  expected 'Invalid amount'");
        SwingUtilities.invokeAndWait(c::dispose);

        // v2 of style D: Enter -> dialog
        JFrame[] d = new JFrame[1];
        SwingUtilities.invokeAndWait(() -> { d[0] = new ConverterD(); d[0].setLocation(0, 0); d[0].setVisible(true); });
        ConverterD cd = (ConverterD) d[0];
        SwingUtilities.invokeAndWait(() -> cd.amount.setText("830"));
        SwingUtilities.invokeLater(cd.amount::postActionEvent);          // dialog is modal: post, don't wait
        Thread.sleep(1200);
        for (Window w : Window.getWindows())
            if (w instanceof JDialog dlg && dlg.isShowing()) { Shot.take(dlg, "screenshots/4_v2_enter_shows_dialog.png"); SwingUtilities.invokeLater(dlg::dispose); }
        Thread.sleep(300);
        SwingUtilities.invokeAndWait(cd::dispose);

        // Shared Action alternative
        ConverterAction[] a = new ConverterAction[1];
        SwingUtilities.invokeAndWait(() -> { a[0] = new ConverterAction(); a[0].setLocation(0, 0); a[0].setVisible(true); });
        SwingUtilities.invokeAndWait(() -> { a[0].amount.setText("500"); a[0].convertAction.actionPerformed(null); });
        System.out.println("Shared Action      : 500 INR -> '" + a[0].result.getText() + "' (one Action object, 4 triggers)");
        Shot.take(a[0], "screenshots/5_shared_action.png");
        System.exit(0);
    }
}
