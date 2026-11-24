import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Task 3 - Campus library search screen (AWT).
 *
 * Frame (BorderLayout)
 *  ├─ NORTH : top (BorderLayout)
 *  │    ├─ NORTH : title Label
 *  │    └─ SOUTH : searchRow (FlowLayout.LEFT)  [TextField] [Choice] [Search]
 *  ├─ CENTER: results (java.awt.List)                <- the only region that grows
 *  └─ SOUTH : bottom (BorderLayout)
 *       ├─ CENTER: buttonRow (FlowLayout.CENTER)   [Prev] [Next] [Reserve]
 *       └─ SOUTH : status Label
 */
public class LibrarySearch extends Frame {
    final TextField query = new TextField(25);
    final Choice category = new Choice();
    final Button search = new Button("Search");
    final List results = new List();
    final Button prev = new Button("Prev"), next = new Button("Next"), reserve = new Button("Reserve");
    final Label status = new Label("Ready");

    static final String[][] BOOKS = {
        {"Java: The Complete Reference", "Programming"}, {"Head First Java", "Programming"},
        {"Effective Java", "Programming"}, {"Clean Code", "Programming"},
        {"Database System Concepts", "Databases"}, {"Learning SQL", "Databases"},
        {"Operating System Concepts", "Systems"}, {"Computer Networks", "Systems"},
        {"Engineering Chemistry", "Science"}, {"Higher Engineering Mathematics", "Science"},
    };

    public LibrarySearch() {
        super("Campus Library - Search");
        setLayout(new BorderLayout());

        Label title = new Label("Campus Library Catalogue", Label.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setBackground(new Color(30, 70, 130)); title.setForeground(Color.WHITE);
        for (String c : new String[]{"All", "Programming", "Databases", "Systems", "Science"}) category.add(c);
        Panel searchRow = new Panel(new FlowLayout(FlowLayout.LEFT));
        searchRow.add(new Label("Search:")); searchRow.add(query); searchRow.add(category); searchRow.add(search);
        Panel top = new Panel(new BorderLayout());
        top.add(title, BorderLayout.NORTH);
        top.add(searchRow, BorderLayout.SOUTH);

        Panel buttonRow = new Panel(new FlowLayout(FlowLayout.CENTER));
        buttonRow.add(prev); buttonRow.add(next); buttonRow.add(reserve);
        status.setBackground(new Color(230, 230, 230));
        Panel bottom = new Panel(new BorderLayout());
        bottom.add(buttonRow, BorderLayout.CENTER);
        bottom.add(status, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(results, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        search.addActionListener(e -> doSearch());
        query.addActionListener(e -> doSearch());
        reserve.addActionListener(e -> status.setText(results.getSelectedItem() == null
                ? "Select a book to reserve" : "Reserved: " + results.getSelectedItem()));
        prev.addActionListener(e -> move(-1));
        next.addActionListener(e -> move(+1));
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { dispose(); } });
        setSize(640, 420);
    }

    void doSearch() {
        results.removeAll();
        String q = query.getText().trim().toLowerCase(), cat = category.getSelectedItem();
        for (String[] b : BOOKS)
            if ((cat.equals("All") || b[1].equals(cat)) && b[0].toLowerCase().contains(q)) results.add(b[0] + "   [" + b[1] + "]");
        status.setText(results.getItemCount() + " result(s) for \"" + query.getText() + "\" in " + cat);
    }

    void move(int d) {
        if (results.getItemCount() == 0) return;
        int i = Math.max(0, Math.min(results.getItemCount() - 1, results.getSelectedIndex() + d));
        results.select(i);
        status.setText("Selected " + (i + 1) + " of " + results.getItemCount());
    }

    public static void main(String[] args) { new LibrarySearch().setVisible(true); }
}
