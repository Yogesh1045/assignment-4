import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Task 4 test driver: sends real MOUSE_PRESSED/RELEASED/CLICKED events to each window,
 * forces a repaint (what restoring a minimised window does), and takes screenshots.
 * Also times the BufferedImage alternative.
 */
public class DrawPadDemo {
    static final int[][] CLICKS = {{60, 60}, {120, 90}, {200, 140}, {280, 70}, {330, 180}};

    static void click(Frame f, int x, int y) throws Exception {
        EventQueue.invokeAndWait(() -> {
            long t = System.currentTimeMillis();
            f.dispatchEvent(new MouseEvent(f, MouseEvent.MOUSE_PRESSED, t, 0, x, y, 1, false, MouseEvent.BUTTON1));
            f.dispatchEvent(new MouseEvent(f, MouseEvent.MOUSE_RELEASED, t, 0, x, y, 1, false, MouseEvent.BUTTON1));
            f.dispatchEvent(new MouseEvent(f, MouseEvent.MOUSE_CLICKED, t, 0, x, y, 1, false, MouseEvent.BUTTON1));
        });
        Thread.sleep(80);
    }

    /** Simulates minimise + restore: the window's pixels are discarded and AWT repaints from paint(). */
    static void minimiseAndRestore(Frame f) throws Exception {
        f.setVisible(false); Thread.sleep(300);
        f.setVisible(true);  Thread.sleep(500);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("== Original (reported) program: mouseclicked never called ==");
        DrawPadBroken broken = new DrawPadBroken();
        broken.setLocation(0, 0); Thread.sleep(500);
        for (int[] c : CLICKS) click(broken, c[0], c[1]);
        System.out.println("   after 5 clicks: count = " + broken.count + ", label = '" + broken.status.getText() + "'");
        Shot.take(broken, "screenshots/1_original_after_5_clicks.png");
        broken.dispose();

        System.out.println("\n== Half-fixed (mouseClicked spelled right, still getGraphics) ==");
        DrawPadBroken.HalfFixed half = new DrawPadBroken.HalfFixed();
        half.setLocation(0, 0); half.setVisible(true); Thread.sleep(500);
        for (int[] c : CLICKS) click(half, c[0], c[1]);
        System.out.println("   after 5 clicks: label = '" + half.status.getText() + "'");
        Shot.take(half, "screenshots/2_halffixed_before_minimise.png");
        minimiseAndRestore(half);
        System.out.println("   after minimise + restore: label = '" + half.status.getText() + "' but dots are gone (see screenshot)");
        Shot.take(half, "screenshots/3_halffixed_after_restore.png");
        half.dispose();

        System.out.println("\n== Repaired program (Complete the Code) ==");
        DrawPad pad = new DrawPad();
        pad.setLocation(0, 0); pad.setVisible(true); Thread.sleep(500);
        for (int[] c : CLICKS) click(pad, c[0], c[1]);
        Shot.take(pad, "screenshots/4_fixed_before_minimise.png");
        minimiseAndRestore(pad);
        Shot.take(pad, "screenshots/5_fixed_after_restore.png");
        pad.dispose();

        alternativeTiming();
        System.exit(0);
    }

    /** n stored points redrawn every paint vs one off-screen image copied per paint. */
    static void alternativeTiming() {
        System.out.println("\n== Alternative: redraw n points vs copy one 400x300 BufferedImage (avg of 200 repaints) ==");
        System.out.printf("%10s %20s %22s %16s %18s%n", "n clicks", "redraw points (us)", "copy BufferedImage (us)", "points memory", "image memory");
        BufferedImage screen = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        for (int n : new int[]{100, 1_000, 10_000, 100_000}) {
            Random r = new Random(n);
            List<Point> pts = new ArrayList<>(n);
            BufferedImage canvas = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
            Graphics2D cg = canvas.createGraphics();
            cg.setColor(Color.WHITE); cg.fillRect(0, 0, 400, 300); cg.setColor(Color.BLACK);
            for (int i = 0; i < n; i++) { Point p = new Point(r.nextInt(400), r.nextInt(300)); pts.add(p); cg.fillOval(p.x - 3, p.y - 3, 6, 6); }
            Graphics g = screen.getGraphics();
            for (int w = 0; w < 20; w++) { for (Point p : pts) g.fillOval(p.x - 3, p.y - 3, 6, 6); g.drawImage(canvas, 0, 0, null); }
            long t0 = System.nanoTime();
            for (int k = 0; k < 200; k++) for (Point p : pts) g.fillOval(p.x - 3, p.y - 3, 6, 6);
            long t1 = System.nanoTime();
            for (int k = 0; k < 200; k++) g.drawImage(canvas, 0, 0, null);
            long t2 = System.nanoTime();
            System.out.printf("%10d %20.1f %22.1f %13d KB %15d KB%n", n, (t1 - t0) / 200e3, (t2 - t1) / 200e3,
                    (long) n * 32 / 1024, 400L * 300 * 4 / 1024);
        }
    }
}
