# Task 3: From Sketch to Layout (Campus Library Search Screen)

**Files:** `LibrarySearch.java` (AWT, nested BorderLayout + FlowLayout), `LibraryAbsolute.java` (alternative: `setLayout(null)`), `LibraryDemo.java` (resizing test + layout timing)
**Run:** `javac *.java && java LibrarySearch` · demo: `java LibraryDemo`
**Evidence:** `output/LibraryDemo.txt`, `screenshots/`

## 3.1 Layout managers chosen
| Container | Layout | Why (resizing requirement) |
|---|---|---|
| Frame | **BorderLayout** | NORTH and SOUTH get their *preferred height* only; **CENTER gets all the remaining space**. That is exactly "results area grows, the rest keeps its size". |
| `top` panel (title + search row) | BorderLayout | stacks the title above the search row, both at preferred height |
| `searchRow` | **FlowLayout(LEFT)** | keeps the TextField, Choice and Search button at their **preferred sizes** whatever the width; extra width becomes empty space on the right |
| results | `java.awt.List` in the frame's CENTER | stretched in both directions by BorderLayout |
| `bottom` panel (buttons + status) | BorderLayout | buttons row above the status line |
| `buttonRow` | **FlowLayout(CENTER)** | Prev/Next/Reserve keep their size and stay centred |
| status | `Label` in `bottom`'s SOUTH | full width, fixed height |
| *(not used)* GridLayout | | it makes every cell the **same size**, so the buttons would stretch with the window, which violates the requirement |
| *(not used)* CardLayout | | for switching whole screens (e.g. search vs details), not for arranging one screen |

## 3.2 Nested containers
```
Frame  [BorderLayout]
├── NORTH  : Panel top [BorderLayout]
│              ├── NORTH: Label "Campus Library Catalogue"
│              └── SOUTH: Panel searchRow [FlowLayout.LEFT]
│                           Label "Search:" · TextField(25) · Choice(category) · Button "Search"
├── CENTER : List results                        ← grows in width AND height
└── SOUTH  : Panel bottom [BorderLayout]
               ├── CENTER: Panel buttonRow [FlowLayout.CENTER]  Button Prev · Button Next · Button Reserve
               └── SOUTH : Label status
```

## 3.3 Implementation
`LibrarySearch.java`. Search (button or Enter) filters 10 sample books by text and category. Prev/Next move the selection; Reserve reports the selected book; the status line shows the result count.

## 3.4 Resizing test (screenshots 1–3)
| Window | results area | search row height | Search button | button row height | Reserve button |
|---|---|---|---|---|---|
| 520 × 340 | 520 × 216 | 41 | 55 × 23 | 33 | 61 × 23 |
| 760 × 480 | 760 × 356 | 41 | 55 × 23 | 33 | 61 × 23 |
| 1100 × 700 | **1100 × 576** | **41** | **55 × 23** | **33** | **61 × 23** |

The sizes were measured by the program, not estimated. Between the smallest and largest window **only the results area changed** (its height grew 216 → 576, width 520 → 1100). The search row, the buttons and the status line kept exactly the same size. **Every region behaved as designed.**

## Alternative concept: GridBagLayout or absolute positioning
```java
// absolute (LibraryAbsolute.java)
setLayout(null);
results.setBounds(10, 75, 620, 260);      // every component: hard-coded x, y, width, height
search.setBounds(465, 40, 80, 25);
// GridBagLayout
GridBagConstraints g = new GridBagConstraints();
g.gridx = 0; g.gridy = 2; g.gridwidth = 4; g.weightx = 1; g.weighty = 1; g.fill = BOTH;  add(results, g);   // only results gets weight
g.weighty = 0; g.fill = NONE; ...                                                       // others: preferred size
```
**Resizing behaviour:** absolute positioning **does not adapt at all**. At 520 × 340 (screenshot 4) the Search button is **cut off** and the Prev/Next/Reserve row and status line are **outside the window**. At 1100 × 700 (screenshot 6) the list stays 620 × 260 with a large empty area. It also breaks with a different font, DPI or look-and-feel. GridBagLayout *can* reproduce the nested design in **one** container (weights decide who grows), but each component needs about 5–8 constraint values, and it is much harder to read and change.

**Measured layout cost** (invalidate + validate the whole frame, average of 2,000 passes):
| Design | time per layout pass | containers | components |
|---|---|---|---|
| Nested BorderLayout + FlowLayout | ≈ 195 µs | 5 | 14 |
| Absolute positioning | ≈ 33 µs | 1 | 10 |

- **Time:** a layout pass visits every component once, **O(c)** for c components. Each nested container also asks its children for preferred sizes (cached after the first pass). Absolute positioning does no computation; it only sets the stored bounds. GridBagLayout is O(c) per pass, but with the largest constant (row and column weight arrays). Even the slowest, 0.2 ms, is about 1% of a 60 Hz frame, so it is irrelevant for a screen that is resized occasionally.
- **Space:** nested design: **4 extra Panel objects** (each ≈ 0.5–1 KB including its peer). GridBag: 1 container plus **one GridBagConstraints per component (O(c))**. Absolute: 1 container, no layout objects. **Choice:** the nested design. It costs a few panels and microseconds, and it is the only one that is correct at every size and easy to change.
