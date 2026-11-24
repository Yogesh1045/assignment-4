# Task 1: GUI Design Review (diagnose and repair a poor event-handling design)

**Files:** `original/FeeWindow.java` (the given code), `FeeService.java` + `FeeWindow.java` (redesign), `FeeDemo.java` (tests, screenshots, measurements)
**Run:** `javac FeeService.java FeeWindow.java FeeDemo.java Shot.java && java FeeDemo` (or `java FeeWindow` to use it by hand)
**Evidence:** `output/FeeDemo.txt`, `output/original_compile_errors.txt`, `screenshots/`

## 1.1 Diagnosis
| # | Deficiency | What the user experiences / what goes wrong |
|---|---|---|
| 1 | **All logic inside the listener** (parsing, SQL, bank wait, printing): no separation between interface and logic | Nothing can be tested without clicking the window; changing the database or bank means editing GUI code |
| 2 | **`e.getActionCommand() == "Pay"`** compares *references*, not text | Works only by accident (string interning). If the label changes or is translated ("Pay Now", "செலுத்து"), Pay silently behaves like Clear |
| 3 | **Every non-Pay command clears the form**, including **Receipt** | Clicking Receipt erases what the clerk typed and shows no receipt |
| 4 | **No input validation**: `Double.parseDouble("")` or `"45k"` | An uncaught `NumberFormatException` on the event thread; nothing visible happens, or a stack trace appears in the console. Negative or 3-decimal amounts are accepted; `double` is used for money |
| 5 | **Database work and `Thread.sleep(3000)` on the event-dispatch thread** | The window **freezes for at least 3 s**: no repaint, buttons unresponsive, "Not responding". Measured: an event queued 50 ms after Pay waited **2,950 ms** |
| 6 | **No feedback**: `System.out.println("Paid")` | The clerk sees nothing on screen, does not know if it worked, and clicks Pay again → **double payment** |
| 7 | **SQL built by string concatenation** | **SQL injection**: a register number like `x'); DROP TABLE fees;--` runs as SQL. Also breaks on names with `'` |
| 8 | **Connection and Statement never closed**; no window-closing handler | A leaked connection per payment until the DB refuses connections; the AWT `Frame` cannot be closed with the X button |
| 9 | **Checked exceptions not handled**: `SQLException`, `InterruptedException` | **Does not compile** (4 errors, see `output/original_compile_errors.txt`) |
| 10 | Buttons and fields are never added to the frame, and no layout is set | Even if it compiled, an empty window would appear |

## 1.2 Redesign
```
FeeWindow (JFrame, UI only)                 FeeService (logic, no Swing)
 - regNo, amount : JTextField                - validate(regNo, amountText) : BigDecimal   throws ValidationException
 - pay, clear, receipt : JButton             - pay(regNo, amountText) : Payment            (slow: bank + DB)
 - message : JLabel                          - receipt(regNo) : Optional<Payment>
 + onPay()  -> validate on EDT,              uses ▼                     uses ▼
              pay() in SwingWorker           «interface» FeeRepository   «interface» PaymentGateway
 + onClear()                                  save(Payment)               charge(regNo, amount) : bankRef
 + onReceipt() -> dialog                      lastPayment(regNo)          (prod: HTTP; test: SlowBank)
                                              (prod: JDBC PreparedStatement in try-with-resources;
                                               test: InMemoryRepo)
```
Event handling (from `FeeWindow.java`):
```java
pay.addActionListener(e -> onPay());          // one handler per button, no string comparison
clear.addActionListener(e -> onClear());
receipt.addActionListener(e -> onReceipt());  // shows the last receipt in a dialog

void onPay() {
    try { service.validate(regNo.getText(), amount.getText()); }       // instant, on the EDT
    catch (FeeService.ValidationException ex) { show(ex.getMessage(), false); return; }
    setBusy(true); show("Contacting bank... please wait.", true);      // disables Pay: no double payment
    new SwingWorker<FeeService.Payment, Void>() {
        protected FeeService.Payment doInBackground() throws Exception { return service.pay(r, a); }  // worker thread
        protected void done() { setBusy(false); /* get() -> success message or "Payment failed: ..." */ }
    }.execute();
}
```
**Results:** 10/10 `FeeService` unit tests pass **with no window at all** (including SQL-injection text rejected by validation). In the GUI: invalid amount → red message; during payment the buttons are disabled and the EDT answers in **≈ 2 ms** (it was frozen 2,950 ms before); success shows the receipt number; Receipt opens a dialog (screenshots 1–4).

## 1.3 Design defence (≤ 100 words)
All decisions (validation, payment, receipts) now live in `FeeService`, which has no GUI code, so it is tested with plain method calls and fake bank/repository objects: 10 tests, run in milliseconds, with no clicking. The window only reads fields and shows results, so layout changes cannot break business rules. Storage and bank sit behind interfaces, so switching MySQL to another database, or the bank gateway, changes one class. Each button has its own small handler, so adding a button touches nothing else. Slow work runs in a SwingWorker, so the window never freezes.

## Alternative concept: one listener with an if–else chain vs one listener per button
```java
// one shared listener (original style, corrected to use equals)
public void actionPerformed(ActionEvent e) {
    String c = e.getActionCommand();
    if (c.equals("Pay")) onPay(); else if (c.equals("Clear")) onClear(); else if (c.equals("Receipt")) onReceipt(); ...
}
// per button / Swing Action
pay.addActionListener(e -> onPay());          // or: Action payAction = new AbstractAction("Pay") {...}; new JButton(payAction)
```
**Measured dispatch time for the last of k buttons** (2,000,000 events):
| k buttons | one listener + if–else (ns) | listener per button (ns) |
|---|---|---|
| 3 | 13.6 | 4.1 |
| 10 | 39.3 | 4.1 |
| 30 | 78.6 | ≈ 0 (inlined by the JIT) |
| 100 | 320.0 | ≈ 0 |

- **Time:** the chain does up to **k string comparisons, O(k)**. A per-button listener is called directly by its button, **O(1)**. Both are nanoseconds, far below what a user notices; the real difference is correctness (no `==` bug, no wrong-branch fall-through) and maintainability.
- **Space:** one shared listener is **O(1)** (the frame itself). Per-button listeners cost **k small objects, O(k)**: a non-capturing lambda ≈ 16 B; a capturing lambda or inner class ≈ 16–24 B plus a reference to the frame. For k = 100 that is about 2 KB, which is negligible. A Swing `Action` is larger (≈ 100 B, it holds the name, icon, enabled state and accelerator), but one Action can be shared by a button, a menu item and a shortcut.
