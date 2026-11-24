import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/** Alternative concept: ONE shared Action used by a button, a menu item, Enter and Ctrl+K. */
public class ConverterAction extends JFrame {
    final JTextField amount = new JTextField(10);
    final JComboBox<String> target = new JComboBox<>(new String[]{"USD", "EUR"});
    final JLabel result = new JLabel(" ");

    final Action convertAction = new AbstractAction("Convert") {
        @Override public void actionPerformed(ActionEvent e) { convert(); }
    };

    public ConverterAction() {
        super("Converter (shared Action)");
        setLayout(new FlowLayout());
        convertAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.CTRL_DOWN_MASK));
        JMenuBar bar = new JMenuBar(); JMenu tools = new JMenu("Tools");
        tools.add(new JMenuItem(convertAction));                 // trigger 1: menu item (+ Ctrl+K accelerator)
        bar.add(tools); setJMenuBar(bar);
        add(new JLabel("INR")); add(amount); add(target);
        add(new JButton(convertAction));                         // trigger 2: button (text comes from the Action)
        amount.setAction(convertAction);                         // trigger 3: Enter in the field
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "convert");   // trigger 4: F5 anywhere
        getRootPane().getActionMap().put("convert", convertAction);
        add(result);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
    }

    void convert() {
        try {
            double a = Double.parseDouble(amount.getText().trim());
            String c = (String) target.getSelectedItem();
            result.setText(String.format("%.2f %s", a * (c.equals("USD") ? 0.012 : 0.011), c));
        } catch (NumberFormatException e) { result.setText("Invalid amount"); }
        pack();
    }
}
