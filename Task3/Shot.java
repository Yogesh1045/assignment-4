import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Test helper: captures a window exactly as it appears on screen (Robot screen capture). */
public final class Shot {
    private Shot() { }
    public static void take(Component c, String file) {
        try {
            SwingUtilities.invokeAndWait(() -> { });          // let pending events/paints finish
            Thread.sleep(400);
            Rectangle r = new Rectangle(c.getLocationOnScreen(), c.getSize());
            BufferedImage img = new Robot().createScreenCapture(r);
            new File(file).getParentFile().mkdirs();
            ImageIO.write(img, "png", new File(file));
            System.out.println("[screenshot] " + file + " (" + r.width + "x" + r.height + ")");
        } catch (Exception e) {
            throw new RuntimeException("screenshot failed: " + file, e);
        }
    }
}
