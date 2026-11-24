import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Frame;

/** Task 3 test driver: three window sizes for both designs, region sizes measured, layout time measured. */
public class LibraryDemo {

    static String sizes(LibrarySearch f) {
        Component searchRow = f.query.getParent(), buttons = f.reserve.getParent();
        return String.format("results %4dx%-4d | search row height %2d, Search button %dx%d | button row height %2d, Reserve %dx%d",
                f.results.getWidth(), f.results.getHeight(), searchRow.getHeight(), f.search.getWidth(), f.search.getHeight(),
                buttons.getHeight(), f.reserve.getWidth(), f.reserve.getHeight());
    }

    public static void main(String[] args) throws Exception {
        LibrarySearch f = new LibrarySearch();
        f.setLocation(0, 0); f.setVisible(true);
        EventQueue.invokeAndWait(() -> { f.query.setText("java"); f.category.select("Programming"); f.doSearch(); f.results.select(1); });
        int[][] sz = {{520, 340}, {760, 480}, {1100, 700}};
        String[] name = {"small", "medium", "large"};
        for (int i = 0; i < 3; i++) {
            final int w = sz[i][0], h = sz[i][1];
            EventQueue.invokeAndWait(() -> { f.setSize(w, h); f.validate(); });
            Thread.sleep(500);
            System.out.printf("%-6s %4dx%-4d : %s%n", name[i], w, h, sizes(f));
            Shot.take(f, "screenshots/" + (i + 1) + "_nested_" + name[i] + "_" + w + "x" + h + ".png");
        }
        f.dispose();

        LibraryAbsolute a = new LibraryAbsolute();
        a.setLocation(0, 0); a.setVisible(true);
        for (int i = 0; i < 3; i += 2) {
            final int w = sz[i][0], h = sz[i][1];
            EventQueue.invokeAndWait(() -> { a.setSize(w, h); a.validate(); });
            Thread.sleep(500);
            Shot.take(a, "screenshots/" + (i + 4) + "_absolute_" + name[i] + "_" + w + "x" + h + ".png");
        }
        a.dispose();

        timing();
        System.exit(0);
    }

    /** Layout computation time: invalidate + validate the whole tree, 2,000 times. */
    static void timing() throws Exception {
        System.out.println("\nLayout cost (invalidate + validate whole frame, avg of 2,000):");
        for (Frame f : new Frame[]{new LibrarySearch(), new LibraryAbsolute()}) {
            f.setSize(800, 500); f.addNotify();
            EventQueue.invokeAndWait(() -> {
                for (int i = 0; i < 300; i++) { f.invalidate(); f.validate(); }
                long t0 = System.nanoTime();
                for (int i = 0; i < 2000; i++) { f.setSize(800 + i % 50, 500); f.invalidate(); f.validate(); }
                long t = System.nanoTime() - t0;
                System.out.printf("   %-16s %8.1f us per layout pass, %2d containers, %2d components in total%n",
                        f.getClass().getSimpleName(), t / 2000e3, containers(f), components(f));
            });
            f.dispose();
        }
    }

    static int containers(Container c) { int n = 1; for (Component k : c.getComponents()) if (k instanceof java.awt.Panel p) n += containers(p); return n; }
    static int components(Container c) { int n = 0; for (Component k : c.getComponents()) { n++; if (k instanceof java.awt.Panel p) n += components(p); } return n; }
}
