# Task 5: Same Screen, Different Event-Handling Styles (Currency Converter)

**Files:**
- `Converter.java`: Complete the Code (lambda style, one listener for the button **and** Enter)
- `styles/v1/ConverterA..D.java`: the four styles, original requirement (button → label)
- `styles/v2/ConverterA..D.java`: the same four after the requirement change (button **or Enter** → **dialog**)
- `ConverterAction.java`: alternative (one shared `Action` for a button, a menu item, Enter, Ctrl+K and F5)
- `ConverterDemo.java`: test driver with screenshots

**Run:** `javac Converter.java && java Converter` · `cd styles/v2 && javac *.java && java ConverterB` · demo: see `run_all.sh`
**Evidence:** `output/ConverterDemo.txt`, `output/change_effort.txt`, `screenshots/`

## Complete the Code
```java
ActionListener onConvert = e -> convert();     // TODO 1: ONE lambda listener object ...
convertButton.addActionListener(onConvert);     // ... for the button
amount.addActionListener(onConvert);            // ... and Enter in the text field (JTextField fires ActionEvent on Enter)

private void convert() {                        // TODO 2
    try {
        double inr = Double.parseDouble(amount.getText().trim());
        String cur = (String) target.getSelectedItem();
        result.setText(String.format("%.2f %s", inr * rate(cur), cur));
    } catch (NumberFormatException ex) {
        result.setText("Invalid amount");
    }
    pack();
}
```
| Test | Output | Screenshot |
|---|---|---|
| 2500 INR → USD, button | `30.00 USD` | `1_valid_conversion.png` |
| `12a5`, button | `Invalid amount` | `2_invalid_amount.png` |
| 1000 INR → EUR, **Enter key** | `11.00 EUR` | `3_enter_key.png` |
| empty field | `Invalid amount` | (console) |

## 5.1 Four implementations (`styles/v1`)
| Style | How Convert is wired |
|---|---|
| (a) frame implements `ActionListener` | `class ConverterA extends JFrame implements ActionListener`; `convert.addActionListener(this)`; `actionPerformed` in the frame |
| (b) named inner class | `class ConvertHandler implements ActionListener { … }`; `convert.addActionListener(new ConvertHandler())` |
| (c) anonymous inner class | `convert.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { … } })` |
| (d) lambda | `convert.addActionListener(e -> result.setText(compute()))` |

All four close with `addWindowListener(new WindowAdapter() { windowClosing → dispose() })`, and all share the same `compute()` method, so only the wiring differs.

## 5.2 Requirement change: Enter also converts, and the result goes in a dialog
Measured with `diff` (`output/change_effort.txt`):
| Style | lines changed | What had to change |
|---|---|---|
| (a) frame as listener | **3** | add `amount.addActionListener(this)`; change the body to `JOptionPane.showMessageDialog(this, …)` |
| (b) named inner class | 6 | create the handler once in a variable, register it twice; in the dialog call, **`this` is the handler, not the frame**, so it had to become `ConverterB.this` |
| (c) anonymous class | **8** | the anonymous object had to be **pulled out into a variable** to register it twice; the same `this` → `ConverterC.this` problem |
| (d) lambda | 4 | store the lambda in a variable and register it twice; in a lambda, `this` is already the frame |

The first compile of v2 actually **failed** for (b) and (c) with `incompatible types: ConverterB.ConvertHandler cannot be converted to Component`. That is the inner-class `this` problem found in practice. Screenshot `4_v2_enter_shows_dialog.png` shows the v2 dialog opened by Enter.

## 5.3 Comparison
| | (a) frame implements | (b) named inner | (c) anonymous | (d) lambda |
|---|---|---|---|---|
| Readability | poor once there are several buttons: one `actionPerformed` with an if-chain on the source | good: a name says what it does | verbose; logic buried in the constructor | **best**: one line, intent visible at the registration |
| Access to frame fields | direct | direct (inner class), but `this` ≠ frame | direct, but `this` ≠ frame | direct, and **`this` = frame** |
| Reuse of the handler | the whole frame is "the listener", which leaks a public `actionPerformed` | **best**: can be instantiated many times, or moved to its own file | not reusable unless stored in a variable | reusable if stored in a variable |
| Ease of change | easy for 1 button, degrades into if-chains | good | worst (8 lines here) | good (4 lines here) |

**For a large application:** use **lambdas or method references that call a named method** (`convert.addActionListener(e -> onConvert())`) for simple wiring, and **shared `Action` objects** for commands that have several triggers (button + menu + shortcut). They are short and readable, the logic lives in a named, testable method (or in a service, as in Task 1), and there is no `this` confusion. Avoid "frame implements ActionListener", which becomes one giant method as buttons are added.

## Alternative concept: method reference, or one shared `Action`
```java
convertButton.addActionListener(this::convert);   // method reference: convert(ActionEvent) or a wrapper lambda

Action convertAction = new AbstractAction("Convert") { public void actionPerformed(ActionEvent e) { convert(); } };
tools.add(new JMenuItem(convertAction));           // menu item (accelerator Ctrl+K stored IN the Action)
add(new JButton(convertAction));                   // button: its text comes from the Action
amount.setAction(convertAction);                   // Enter
getRootPane().getActionMap().put("convert", convertAction);   // F5 key binding
```
`ConverterAction.java` runs this way: 500 INR → `6.00 USD` (screenshot 5).
| Approach | Listener objects for t triggers | Adding a new trigger |
|---|---|---|
| (a) frame | 0 extra (the frame), but one big if-chain | edit `actionPerformed` + register |
| (b) named inner | 1 shared (if stored) or t | 1 line to register |
| (c) anonymous | t (one per registration) unless stored | copy the whole class body, or refactor into a variable |
| (d) lambda / method reference | 1 shared (if stored) or t | 1 line |
| **shared Action** | **1**, for every trigger | 1 line; the name, enabled state and shortcut are kept in sync automatically (`convertAction.setEnabled(false)` disables the button *and* the menu item) |

Space: each listener object is small (≈ 16–32 B), so O(t) vs O(1) is negligible. The real gain of a shared Action is **consistency**: one place for the label, the shortcut and the enabled state.
