import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** The program exactly as reported by users (class renamed so both versions can coexist). */
public class DrawPadBroken extends Frame {
    int count = 0; Label status = new Label("Clicks: 0");
    public DrawPadBroken() {
        add(status, BorderLayout.SOUTH);
        addMouseListener(new MouseAdapter() {
            public void mouseclicked(MouseEvent e) {          // lower-case c: a NEW method, never called
                count++;
                Graphics g = getGraphics(); g.fillOval(e.getX(), e.getY(), 6, 6);
            }
        });
        setSize(400, 300); setVisible(true);
    }

    /** First defect fixed (mouseClicked spelled correctly) but still drawing with getGraphics(). */
    static class HalfFixed extends Frame {
        int count = 0; Label status = new Label("Clicks: 0");
        HalfFixed() {
            super("Half-fixed DrawPad");
            add(status, BorderLayout.SOUTH);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    count++;
                    status.setText("Clicks: " + count);
                    Graphics g = getGraphics(); g.fillOval(e.getX(), e.getY(), 6, 6);   // drawn once, not remembered
                }
            });
            setSize(400, 300);
        }
    }
}
