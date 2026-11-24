# Task 4: Event-Handling Forensics (Drawing Pad)

**Files:** `broken/DrawPadBroken.java` (the reported program, plus a half-fixed version), `DrawPad.java` (Complete the Code), `misspelled/DrawPad.java` (the @Override experiment), `DrawPadDemo.java` (sends real mouse events, simulates minimise/restore, takes screenshots, times the alternative)
**Run:** `javac DrawPad.java && java DrawPad` · demo: `javac *.java broken/DrawPadBroken.java && java DrawPadDemo`
**Evidence:** `output/DrawPadDemo.txt`, `output/override_compile_check.txt`, `screenshots/`

## 4.1 Locate the defect
`public void mouseclicked(MouseEvent e)` is spelled with a lower-case **c**. `MouseAdapter` defines `mouseClicked`. Java is case-sensitive, so this declares a **new, unrelated method** instead of overriding the adapter's empty one. AWT calls `mouseClicked`, which does nothing; `mouseclicked` is never called. So `count++` never runs. **The label never changes** for a second reason as well: even if the method ran, it only increments `count` and never calls `status.setText(...)`.
Evidence: after 5 real clicks, `count = 0, label = 'Clicks: 0'` (screenshot 1).

## 4.2 A failing interaction (after fixing the spelling)
**Interaction:** click 5 times (5 dots appear, label "Clicks: 5") → **minimise the window** → restore it. **Result:** the label still says 5, but **all dots are gone** (screenshots 2 → 3).
**Why:** `getGraphics()` draws straight onto the window's current pixels, and nothing remembers what was drawn. When a window is hidden, minimised, covered or resized, the OS discards those pixels and AWT asks the component to redraw itself by calling `paint()` (via `update()` and `repaint()`). The Frame's `paint()` knows nothing about the dots, so they are lost. (On the virtual display used for the screenshots, minimise and restore are reproduced with `setVisible(false)` / `setVisible(true)`, which triggers the same full repaint.)

## 4.3 Repair (Complete the Code)
```java
addMouseListener(new MouseAdapter() {
    @Override public void mouseClicked(MouseEvent e) {        // TODO 1
        points.add(e.getPoint());                             // store the model
        status.setText("Clicks: " + points.size());          // update the label
        repaint();                                            // request paint() asynchronously
    }
});
@Override public void paint(Graphics g) {                     // TODO 2
    for (Point p : points) g.fillOval(p.x - 3, p.y - 3, 6, 6); // 6 × 6 circle CENTRED on the click
}
```
The state (points) is separate from the drawing, and `paint()` redraws the whole state every time it is called. Screenshots 4 and 5: **5 dots before and after minimise/restore**, label "Clicks: 5" (`fillOval(x, y, …)` would put the click at the top-left corner, so the offset of 3 centres it).

## 4.4 Prevent recurrence: `@Override`
Recorded by compiling `misspelled/DrawPad.java` both ways:
```
--- mouseclicked WITHOUT @Override:
compiles with NO error or warning (the bug is silent)
--- mouseclicked WITH @Override:
misspelled/DrawPad.java (with @Override):19: error: method does not override or implement a method from a supertype
            @Override public void mouseclicked(MouseEvent e) {
            ^
1 error
```
`@Override` states the intent "this replaces a supertype method". The compiler checks that a method with exactly that name and parameter list exists in a supertype, so a misspelling or a wrong parameter type becomes a **compile-time error** instead of a silent runtime bug. Rule: always put `@Override` on listener and adapter methods.

## Alternative concept: off-screen `BufferedImage`
```java
private final BufferedImage canvas = new BufferedImage(400, 300, TYPE_INT_ARGB);
public void mouseClicked(MouseEvent e) {
    Graphics2D g = canvas.createGraphics(); g.fillOval(e.getX() - 3, e.getY() - 3, 6, 6); g.dispose();   // draw ONCE
    count++; status.setText("Clicks: " + count); repaint();
}
public void paint(Graphics g) { g.drawImage(canvas, 0, 0, null); }                                     // copy pixels
```
**Measured** (average of 200 repaints):
| n clicks | redraw stored points | copy one 400×300 image | points memory | image memory |
|---|---|---|---|---|
| 100 | 265 µs | 27 µs | 3 KB | 468 KB |
| 1,000 | 1,440 µs | 18 µs | 31 KB | 468 KB |
| 10,000 | 12,746 µs | 18 µs | 312 KB | 468 KB |
| 100,000 | **118,207 µs (≈ 0.12 s: visible lag)** | **15 µs** | 3.1 MB | 468 KB |

- **Time per repaint:** stored points **O(n)**; image copy **O(W·H)**, constant in n. The crossover is at about 100 clicks. Beyond that the image wins by up to about 8,000×.
- **Space:** points **O(n)** (≈ 32 B per `Point`); image **O(W·H)**, 4 bytes per pixel = 468 KB at 400×300, independent of n. Points use less memory until about 15,000 clicks.
- **Trade-offs:** the image is fixed-size, so resizing the window means creating a larger image and copying. Individual dots can no longer be undone, moved or saved as data, because they become pixels. Many paint programs keep **both**: the point list for undo and saving, and a cached image for fast repaint.
