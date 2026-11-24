import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/** Task 4.4: TODO 1 misspelled as mouseclicked. Compiled twice by run_all.sh: with and without @Override. */
public class DrawPad extends Frame {
    private final List<Point> points = new ArrayList<>();
    private final Label status = new Label("Clicks: 0");
    public DrawPad() {
        super("Draw Pad");
        add(status, BorderLayout.SOUTH);
        addMouseListener(new MouseAdapter() {
            /*OVERRIDE*/ public void mouseclicked(MouseEvent e) {
                points.add(e.getPoint());
                status.setText("Clicks: " + points.size());
                repaint();
            }
        });
        setSize(400, 300);
    }
    @Override public void paint(Graphics g) { for (Point p : points) g.fillOval(p.x - 3, p.y - 3, 6, 6); }
}
