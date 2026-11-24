import java.awt.Button;
import java.awt.Choice;
import java.awt.Frame;
import java.awt.Label;
import java.awt.List;
import java.awt.TextField;

/** Alternative concept: the same screen with setLayout(null) and setBounds - nothing moves on resize. */
public class LibraryAbsolute extends Frame {
    public LibraryAbsolute() {
        super("Library (absolute positioning)");
        setLayout(null);
        Label title = new Label("Campus Library Catalogue", Label.CENTER); title.setBounds(0, 0, 640, 30);
        Label l = new Label("Search:"); l.setBounds(10, 40, 60, 25);
        TextField q = new TextField(); q.setBounds(75, 40, 240, 25);
        Choice c = new Choice(); c.add("All"); c.add("Programming"); c.setBounds(325, 40, 130, 25);
        Button s = new Button("Search"); s.setBounds(465, 40, 80, 25);
        List r = new List(); r.setBounds(10, 75, 620, 260);
        for (String[] b : LibrarySearch.BOOKS) r.add(b[0]);
        Button p = new Button("Prev"), n = new Button("Next"), res = new Button("Reserve");
        p.setBounds(200, 345, 70, 25); n.setBounds(280, 345, 70, 25); res.setBounds(360, 345, 80, 25);
        Label st = new Label("Ready"); st.setBounds(0, 380, 640, 25);
        for (java.awt.Component k : new java.awt.Component[]{title, l, q, c, s, r, p, n, res, st}) add(k);
        setSize(640, 420);
    }
}
