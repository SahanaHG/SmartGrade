import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Dashboard - the main Swing window of SMARTGRADE.
 *
 * Layout: a dark navigation sidebar on the left and a CardLayout "content" area on the right.
 * Each sidebar button shows one page (Dashboard, Add, View, Search, Update, Analytics, Class Summary).
 *
 * The file is organised in 4 blocks:
 *   1. Theme + small helper methods
 *   2. Custom components (buttons, cards, charts, score ring)
 *   3. The 7 pages (inner classes)
 *   4. Navigation, dialogs and exit handling
 */
public class Dashboard extends JFrame {

    // =====================================================================
    //  1. THEME + HELPERS
    // =====================================================================
    static final Color BG = new Color(0xF1F5F9);
    static final Color SIDEBAR = new Color(0x0F172A);
    static final Color SIDEBAR_HOVER = new Color(0x1E293B);
    static final Color ACCENT = new Color(0x6366F1);
    static final Color TEAL = new Color(0x14B8A6);
    static final Color GOOD = new Color(0x22C55E);
    static final Color BAD = new Color(0xEF4444);
    static final Color WARN = new Color(0xF59E0B);
    static final Color TEXT = new Color(0x0F172A);
    static final Color MUTED = new Color(0x64748B);
    static final Color BORDER = new Color(0xE2E8F0);
    static final Color[] PALETTE = {ACCENT, TEAL, WARN, new Color(0xEC4899), new Color(0x0EA5E9),
            GOOD, new Color(0x8B5CF6)};

    static final Font F_BASE = new Font("SansSerif", Font.PLAIN, 13);
    static final Font F_BOLD = new Font("SansSerif", Font.BOLD, 13);
    static final Font F_INPUT = new Font("SansSerif", Font.PLAIN, 14);
    static final Font F_H2 = new Font("SansSerif", Font.BOLD, 16);
    static final Font F_TITLE = new Font("SansSerif", Font.BOLD, 28);
    static final Font F_BIG = new Font("SansSerif", Font.BOLD, 32);
    static final Font F_MONO = new Font(Font.MONOSPACED, Font.PLAIN, 13);

    static final String[] SEMESTERS = {"1", "2", "3", "4", "5", "6", "7", "8"};
    static final String[] DEFAULT_SUBJECTS = {"Mathematics", "Physics", "Chemistry", "English", "Computer Science"};
    static final String[] STUDENT_COLUMNS = {"ID", "Name", "Department", "Sem", "Subjects", "Total",
            "Average", "Percentage", "Grade", "Status"};

    static Color gradeColor(String grade) {
        switch (grade) {
            case "A+": return new Color(0x16A34A);
            case "A":  return new Color(0x14B8A6);
            case "B":  return new Color(0x3B82F6);
            case "C":  return new Color(0xF59E0B);
            case "D":  return new Color(0xF97316);
            default:   return BAD;
        }
    }

    static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    static JTextField field() {
        JTextField f = new JTextField();
        f.setFont(F_INPUT);
        f.setForeground(TEXT);
        f.setCaretColor(ACCENT);
        f.setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(8, 10, 8, 10)));
        return f;
    }

    static JComboBox<String> combo(String[] items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setFont(F_INPUT);
        c.setBackground(Color.WHITE);
        return c;
    }

    static JPanel formRow(String caption, JComponent input) {
        Flat p = new Flat(new BorderLayout(0, 5));
        p.add(label(caption, F_BOLD, MUTED), BorderLayout.NORTH);
        p.add(input, BorderLayout.CENTER);
        return p;
    }

    /** A row of equally wide components (cards, charts ...). */
    static JPanel row(JComponent... items) {
        Flat p = new Flat(new GridLayout(1, items.length, 16, 0));
        for (JComponent c : items) p.add(c);
        return p;
    }

    /** Fixed-width left component + flexible right component. */
    static JPanel split(JComponent left, JComponent right, int leftWidth) {
        Flat p = new Flat(new BorderLayout(16, 0));
        left.setPreferredSize(new Dimension(leftWidth, left.getPreferredSize().height));
        p.add(left, BorderLayout.WEST);
        p.add(right, BorderLayout.CENTER);
        return p;
    }

    static JPanel buttons(JComponent... items) {
        Flat p = new Flat(null);
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        for (int i = 0; i < items.length; i++) {
            if (i > 0) p.add(Box.createHorizontalStrut(12));
            p.add(items[i]);
        }
        p.add(Box.createHorizontalGlue());
        return p;
    }

    static JPanel mini(String caption, JLabel value) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        p.add(label(caption, F_BASE, MUTED), BorderLayout.NORTH);
        p.add(value, BorderLayout.CENTER);
        return p;
    }

    static String fit(String s, FontMetrics fm, int width) {
        if (fm.stringWidth(s) <= width) return s;
        while (s.length() > 1 && fm.stringWidth(s + "...") > width) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    static DefaultTableModel readOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return getRowCount() > 0 && getValueAt(0, c) != null ? getValueAt(0, c).getClass() : Object.class;
            }
        };
    }

    static void styleTable(JTable t) {
        t.setRowHeight(34);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setFont(F_BASE);
        t.setForeground(TEXT);
        t.setSelectionBackground(new Color(0xE0E7FF));
        t.setSelectionForeground(TEXT);
        t.setFillsViewportHeight(true);
        t.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        CellRenderer r = new CellRenderer();
        t.setDefaultRenderer(Object.class, r);
        t.setDefaultRenderer(Number.class, r);
        t.setDefaultRenderer(Double.class, r);
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(0, 38));
        h.setDefaultRenderer(new HeaderRenderer());
    }

    /** Fixes the width of one table column (keeps small columns like Rank / Grade narrow). */
    static void colWidth(JTable t, int col, int width) {
        t.getColumnModel().getColumn(col).setMinWidth(width);
        t.getColumnModel().getColumn(col).setMaxWidth(width);
        t.getColumnModel().getColumn(col).setPreferredWidth(width);
    }

    /** Standard narrow columns for the 10-column student table. */
    static void studentColumnWidths(JTable t) {
        colWidth(t, 0, 72);
        colWidth(t, 3, 68);
        colWidth(t, 4, 100);
        colWidth(t, 5, 78);
        colWidth(t, 6, 92);
        colWidth(t, 7, 116);
        colWidth(t, 8, 76);
        colWidth(t, 9, 80);
    }

    static JScrollPane tableScroll(JTable t, int visibleRows) {
        t.setPreferredScrollableViewportSize(new Dimension(300, visibleRows * t.getRowHeight()));
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(new LineBorder(BORDER));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    /** Fills a table model using the standard student columns. */
    static void fillStudentTable(DefaultTableModel model, List<Student> list) {
        model.setRowCount(0);
        for (Student s : list) {
            double pct = GradeCalculator.percentage(s);
            model.addRow(new Object[]{s.getId(), s.getName(), s.getDepartment(), s.getSemester(),
                    s.getSubjectCount(), GradeCalculator.total(s), GradeCalculator.average(s), pct,
                    GradeCalculator.grade(pct), GradeCalculator.status(s)});
        }
    }

    static String selectedId(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return table.getModel().getValueAt(table.convertRowIndexToModel(row), 0).toString();
    }

    // =====================================================================
    //  2. CUSTOM COMPONENTS
    // =====================================================================

    /** A transparent panel that never grows taller than it needs (keeps pages tidy). */
    static class Flat extends JPanel {
        Flat(LayoutManager layout) {
            super(layout);
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        @Override public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    /** Rounded coloured button with hover effect. */
    static class RoundButton extends JButton {
        private final Color base;
        private boolean hover;

        RoundButton(String text, Color base) {
            super(text);
            this.base = base;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(Color.WHITE);
            setFont(F_BOLD);
            setBorder(new EmptyBorder(10, 20, 10, 20));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = !isEnabled() ? new Color(0xCBD5E1) : hover ? base.brighter() : base;
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Sidebar navigation button with an accent bar on the active page. */
    static class NavButton extends JButton {
        private boolean active, hover;

        NavButton(String text) {
            super(text);
            setHorizontalAlignment(SwingConstants.LEFT);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setFont(F_BOLD);
            setForeground(new Color(0xCBD5E1));
            setBorder(new EmptyBorder(11, 26, 11, 10));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        void setActive(boolean a) {
            active = a;
            setForeground(a ? Color.WHITE : new Color(0xCBD5E1));
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (active || hover) {
                g2.setColor(active ? new Color(0x312E81) : SIDEBAR_HOVER);
                g2.fillRoundRect(10, 2, getWidth() - 20, getHeight() - 4, 14, 14);
            }
            if (active) {
                g2.setColor(new Color(0xA5B4FC));
                g2.fillRoundRect(14, 12, 4, getHeight() - 24, 4, 4);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** White rounded panel with a soft shadow and an optional title. Add content in the CENTER. */
    static class Card extends JPanel {
        Card(String title) {
            super(new BorderLayout(0, 12));
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setBorder(new EmptyBorder(18, 22, 24, 22));
            if (title != null) add(label(title, F_H2, TEXT), BorderLayout.NORTH);
        }

        @Override public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 1, h = getHeight() - 5;
            g2.setColor(new Color(15, 23, 42, 16));
            g2.fillRoundRect(0, 4, w, h, 20, 20);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w, h, 20, 20);
            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, w, h, 20, 20);
            g2.dispose();
        }
    }

    /** Big-number card for the dashboard (coloured strip on the left). */
    static class StatCard extends Card {
        private final JLabel value = label("0", F_BIG, TEXT);
        private final Color accent;

        StatCard(String caption, Color accent) {
            super(null);
            this.accent = accent;
            add(label(caption, F_BOLD, MUTED), BorderLayout.NORTH);
            add(value, BorderLayout.CENTER);
        }

        void setValue(String text) { value.setText(text); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(accent);
            g2.fillRoundRect(0, 20, 5, getHeight() - 5 - 40, 5, 5);
            g2.dispose();
        }
    }

    /** Card that highlights one student (used for "Top" / "Lowest" on the class page). */
    static class Highlight extends Card {
        private final JLabel name = label("-", F_H2, TEXT);
        private final JLabel detail = label(" ", F_BASE, MUTED);

        Highlight(String title) {
            super(title);
            Flat box = new Flat(new BorderLayout(0, 4));
            box.add(name, BorderLayout.NORTH);
            box.add(detail, BorderLayout.CENTER);
            add(box, BorderLayout.CENTER);
        }

        void set(Student s) {
            if (s == null) { name.setText("-"); detail.setText("No students yet"); return; }
            double pct = GradeCalculator.percentage(s);
            name.setText(s.getName() + "  (" + s.getId() + ")");
            detail.setText("Average " + GradeCalculator.fmt(GradeCalculator.average(s))
                    + "   |   Grade " + GradeCalculator.grade(pct) + "   |   " + GradeCalculator.status(s));
        }
    }

    /** Horizontal bar chart drawn with Java2D. */
    static class BarChart extends JPanel {
        private final LinkedHashMap<String, Double> data = new LinkedHashMap<>();
        private double max = 100;
        private boolean whole;
        private Function<String, Color> colorFn;

        BarChart() { setOpaque(false); }

        void setColorFn(Function<String, Color> f) { colorFn = f; }

        void setData(Map<String, ? extends Number> d, double max, boolean whole) {
            data.clear();
            for (Map.Entry<String, ? extends Number> e : d.entrySet()) data.put(e.getKey(), e.getValue().doubleValue());
            this.max = Math.max(max, 1);
            this.whole = whole;
            revalidate();
            repaint();
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(280, Math.max(1, data.size()) * 34 + 8);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(F_BASE);
            FontMetrics fm = g2.getFontMetrics();
            if (data.isEmpty()) {
                g2.setColor(MUTED);
                g2.drawString("No data yet - add some students first.", 4, 24);
                g2.dispose();
                return;
            }
            int labelW = 130, valueW = 54, trackW = getWidth() - labelW - valueW - 6, y = 4, i = 0;
            if (trackW < 20) { g2.dispose(); return; }
            for (Map.Entry<String, Double> e : data.entrySet()) {
                Color c = colorFn != null ? colorFn.apply(e.getKey()) : PALETTE[i % PALETTE.length];
                g2.setFont(F_BASE);
                g2.setColor(TEXT);
                g2.drawString(fit(e.getKey(), fm, labelW - 10), 4, y + 20);
                g2.setColor(new Color(0xE2E8F0));
                g2.fillRoundRect(labelW, y + 7, trackW, 14, 14, 14);
                int w = (int) Math.round(trackW * Math.min(e.getValue() / max, 1.0));
                if (w > 0) {
                    g2.setColor(c);
                    g2.fillRoundRect(labelW, y + 7, Math.max(w, 14), 14, 14, 14);
                }
                String v = whole ? String.valueOf(e.getValue().intValue()) : GradeCalculator.fmt(e.getValue());
                g2.setFont(F_BOLD);
                g2.setColor(TEXT);
                g2.drawString(v, labelW + trackW + 10, y + 20);
                y += 34;
                i++;
            }
            g2.dispose();
        }
    }

    /** Circular gauge showing the percentage and grade. */
    static class ScoreRing extends JPanel {
        private double pct;
        private String grade = "-";
        private Color color = ACCENT;

        ScoreRing() { setOpaque(false); setPreferredSize(new Dimension(220, 230)); }

        void set(double pct, String grade, Color color) {
            this.pct = pct;
            this.grade = grade;
            this.color = color;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight()) - 40;
            if (size < 40) { g2.dispose(); return; }
            int x = (getWidth() - size) / 2, y = (getHeight() - size) / 2;
            g2.setStroke(new BasicStroke(16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(0xE2E8F0));
            g2.drawArc(x, y, size, size, 0, 360);
            g2.setColor(color);
            g2.drawArc(x, y, size, size, 90, -(int) Math.round(Math.min(pct, 100) * 3.6));
            String big = GradeCalculator.fmt(pct) + "%";
            g2.setFont(new Font("SansSerif", Font.BOLD, 30));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(TEXT);
            g2.drawString(big, (getWidth() - fm.stringWidth(big)) / 2, getHeight() / 2 + 4);
            String small = "Grade " + grade;
            g2.setFont(F_H2);
            fm = g2.getFontMetrics();
            g2.setColor(color);
            g2.drawString(small, (getWidth() - fm.stringWidth(small)) / 2, getHeight() / 2 + 30);
            g2.dispose();
        }
    }

    static class CellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean sel,
                                                                 boolean focus, int row, int col) {
            super.getTableCellRendererComponent(table, value, sel, false, row, col);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            setFont(F_BASE);
            setForeground(TEXT);
            if (value instanceof Double) setText(GradeCalculator.fmt((Double) value));
            setHorizontalAlignment(value instanceof Number ? CENTER : LEFT);
            if (!sel) setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF8FAFC));
            String column = table.getColumnName(col);
            if (value != null && (column.equals("Grade") || column.equals("Status"))) {
                String v = value.toString();
                setForeground(v.equals("PASS") ? new Color(0x16A34A) : v.equals("FAIL") ? BAD : gradeColor(v));
                setFont(F_BOLD);
                setHorizontalAlignment(CENTER);
            }
            return this;
        }
    }

    static class HeaderRenderer extends DefaultTableCellRenderer {
        HeaderRenderer() { setOpaque(true); }

        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean sel,
                                                                 boolean focus, int row, int col) {
            super.getTableCellRendererComponent(table, value, false, false, row, col);
            setBackground(SIDEBAR);
            setForeground(Color.WHITE);
            setFont(F_BOLD);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            setHorizontalAlignment(LEFT);
            return this;
        }
    }

    /** Scroll-body that always matches the width of the window. */
    static class ScrollBody extends JPanel implements Scrollable {
        ScrollBody() { super(new BorderLayout()); setOpaque(false); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 18; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 100; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /** Editable subject / marks table with Add, Remove and Default buttons. */
    static class MarksEditor extends JPanel {
        private final DefaultTableModel model = new DefaultTableModel(new String[]{"Subject", "Marks (0-100)"}, 0);
        private final JTable table = new JTable(model);

        MarksEditor() {
            super(new BorderLayout(0, 12));
            setOpaque(false);
            styleTable(table);
            add(tableScroll(table, 6), BorderLayout.CENTER);

            RoundButton add = new RoundButton("+ Add Subject", ACCENT);
            RoundButton remove = new RoundButton("Remove Selected", new Color(0x64748B));
            RoundButton defaults = new RoundButton("Default Subjects", new Color(0x0EA5E9));
            add.addActionListener(e -> model.addRow(new Object[]{"", ""}));
            remove.addActionListener(e -> {
                stopEditing();
                int r = table.getSelectedRow();
                if (r >= 0) model.removeRow(r);
            });
            defaults.addActionListener(e -> useDefaultSubjects());
            add(buttons(add, remove, defaults), BorderLayout.SOUTH);
            useDefaultSubjects();
        }

        private void stopEditing() {
            if (table.isEditing()) table.getCellEditor().stopCellEditing();
        }

        void useDefaultSubjects() {
            stopEditing();
            model.setRowCount(0);
            for (String s : DEFAULT_SUBJECTS) model.addRow(new Object[]{s, ""});
        }

        void setData(List<String> names, List<Double> marks) {
            stopEditing();
            model.setRowCount(0);
            for (int i = 0; i < names.size(); i++) model.addRow(new Object[]{names.get(i), GradeCalculator.fmt(marks.get(i))});
        }

        void clearAll() { model.setRowCount(0); }

        void setEditable(boolean on) { table.setEnabled(on); }

        /** Reads and validates every row. Throws ValidationException with a clear message on bad input. */
        void collect(ArrayList<String> names, ArrayList<Double> marks) throws ValidationException {
            stopEditing();
            names.clear();
            marks.clear();
            for (int r = 0; r < model.getRowCount(); r++) {
                String subject = String.valueOf(model.getValueAt(r, 0)).trim();
                String raw = String.valueOf(model.getValueAt(r, 1)).trim();
                if (subject.equals("null")) subject = "";
                if (raw.equals("null")) raw = "";
                if (subject.isEmpty() && raw.isEmpty()) continue;           // ignore blank rows
                subject = Validation.requireSubject(subject);
                names.add(subject);
                marks.add(Validation.parseMark(subject, raw));
            }
            Validation.requireNoDuplicates(names);
            if (names.isEmpty()) throw new ValidationException("Add at least one subject with marks.");
        }
    }

    // =====================================================================
    //  3. WINDOW + PAGES
    // =====================================================================
    private final StudentManager manager;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);
    private final Map<String, NavButton> navButtons = new LinkedHashMap<>();
    private final Map<String, Page> pages = new LinkedHashMap<>();
    private UpdatePage updatePage;

    public Dashboard(StudentManager manager) {
        super("SMARTGRADE - Student Academic Performance & Analytics System");
        this.manager = manager;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { confirmExit(); }
        });
        setSize(1260, 800);
        setMinimumSize(new Dimension(1060, 680));
        setLocationRelativeTo(null);

        content.setBackground(BG);
        JPanel root = new JPanel(new BorderLayout());
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);

        updatePage = new UpdatePage();
        register("Dashboard", new HomePage());
        register("Add Student", new AddPage());
        register("View Students", new ViewPage());
        register("Search Student", new SearchPage());
        register("Update Student", updatePage);
        register("Performance Analytics", new AnalyticsPage());
        register("Class Summary", new ClassPage());
        showPage("Dashboard");
    }

    private void register(String name, Page page) {
        pages.put(name, page);
        content.add(page, name);
        NavButton b = navButtons.get(name);
        b.addActionListener(e -> showPage(name));
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(SIDEBAR);
        side.setPreferredSize(new Dimension(250, 0));
        side.setBorder(new EmptyBorder(26, 0, 20, 0));

        JLabel logo = new JLabel("<html><span style='color:#FFFFFF'>SMART</span><span style='color:#818CF8'>GRADE</span></html>");
        logo.setFont(new Font("SansSerif", Font.BOLD, 26));
        logo.setBorder(new EmptyBorder(0, 26, 0, 0));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel tag = label("Track. Analyze. Improve.", F_BASE, new Color(0x94A3B8));
        tag.setBorder(new EmptyBorder(2, 27, 26, 0));
        tag.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(logo);
        side.add(tag);

        for (String name : new String[]{"Dashboard", "Add Student", "View Students", "Search Student",
                "Update Student", "Performance Analytics", "Class Summary"}) {
            NavButton b = new NavButton(name);
            navButtons.put(name, b);
            side.add(b);
            side.add(Box.createVerticalStrut(4));
        }

        side.add(Box.createVerticalGlue());
        RoundButton exit = new RoundButton("Exit", new Color(0xDC2626));
        exit.setAlignmentX(Component.LEFT_ALIGNMENT);
        exit.setMaximumSize(new Dimension(190, 42));
        exit.addActionListener(e -> confirmExit());
        JPanel exitWrap = new Flat(new FlowLayout(FlowLayout.LEFT, 30, 0));
        exitWrap.add(exit);
        side.add(exitWrap);
        JLabel foot = label("CodeAlpha Java Internship", new Font("SansSerif", Font.PLAIN, 11), new Color(0x64748B));
        foot.setBorder(new EmptyBorder(14, 30, 0, 0));
        foot.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(foot);
        return side;
    }

    void showPage(String name) {
        pages.get(name).refresh();
        cardLayout.show(content, name);
        navButtons.forEach((k, b) -> b.setActive(k.equals(name)));
    }

    /** Base class of every page: title, subtitle and a scrollable body. */
    abstract class Page extends JPanel {
        protected final JPanel body = new JPanel();

        Page(String title, String subtitle) {
            super(new BorderLayout());
            setBackground(BG);
            JPanel head = new JPanel();
            head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
            head.setOpaque(false);
            head.setBorder(new EmptyBorder(26, 34, 8, 34));
            JLabel t = label(title, F_TITLE, TEXT);
            JLabel s = label(subtitle, F_BASE, MUTED);
            t.setAlignmentX(Component.LEFT_ALIGNMENT);
            s.setAlignmentX(Component.LEFT_ALIGNMENT);
            head.add(t);
            head.add(Box.createVerticalStrut(4));
            head.add(s);
            add(head, BorderLayout.NORTH);

            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setOpaque(false);
            ScrollBody wrap = new ScrollBody();
            wrap.setBorder(new EmptyBorder(10, 34, 24, 34));
            wrap.add(body, BorderLayout.NORTH);
            JScrollPane sp = new JScrollPane(wrap);
            sp.setBorder(null);
            sp.setOpaque(false);
            sp.getViewport().setOpaque(false);
            sp.getVerticalScrollBar().setUnitIncrement(18);
            add(sp, BorderLayout.CENTER);
        }

        void addRow(JComponent c) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(c);
            body.add(Box.createVerticalStrut(18));
        }

        abstract void refresh();
    }

    // ---------------------------------------------------------------------
    //  Page 1: Dashboard (home)
    // ---------------------------------------------------------------------
    class HomePage extends Page {
        private final StatCard total = new StatCard("Total Students", ACCENT);
        private final StatCard average = new StatCard("Class Average", TEAL);
        private final StatCard passed = new StatCard("Passed", GOOD);
        private final StatCard failed = new StatCard("Failed", BAD);
        private final BarChart gradeChart = new BarChart();
        private final DefaultTableModel topModel = readOnlyModel(new String[]{"Rank", "Student", "Average", "Grade"});

        HomePage() {
            super("Welcome to SMARTGRADE", "Track. Analyze. Improve.  -  your academic performance command centre");
            addRow(row(total, average, passed, failed));

            gradeChart.setColorFn(Dashboard::gradeColor);
            Card gradeCard = new Card("Grade Distribution");
            gradeCard.add(gradeChart, BorderLayout.CENTER);

            JTable topTable = new JTable(topModel);
            styleTable(topTable);
            colWidth(topTable, 0, 72);
            colWidth(topTable, 2, 80);
            colWidth(topTable, 3, 70);
            Card topCard = new Card("Top Performers");
            topCard.add(tableScroll(topTable, 6), BorderLayout.CENTER);
            addRow(row(gradeCard, topCard));

            RoundButton add = new RoundButton("+ Add Student", ACCENT);
            RoundButton analytics = new RoundButton("Class Summary", TEAL);
            RoundButton sample = new RoundButton("Load Sample Data", new Color(0xF59E0B));
            add.addActionListener(e -> showPage("Add Student"));
            analytics.addActionListener(e -> showPage("Class Summary"));
            sample.addActionListener(e -> {
                try {
                    int n = manager.loadSampleData();
                    info("Sample data", n + " demo student(s) added.");
                    showPage("Dashboard");
                } catch (Exception ex) { showError(ex); }
            });
            Card quick = new Card("Quick Actions");
            quick.add(buttons(add, analytics, sample), BorderLayout.CENTER);
            addRow(quick);
        }

        @Override void refresh() {
            ArrayList<Student> all = manager.getAll();
            PerformanceAnalyzer pa = new PerformanceAnalyzer(all);
            total.setValue(String.valueOf(all.size()));
            average.setValue(all.isEmpty() ? "-" : GradeCalculator.fmt(pa.classAverage()));
            passed.setValue(String.valueOf(pa.passedCount()));
            failed.setValue(String.valueOf(pa.failedCount()));
            gradeChart.setData(pa.gradeDistribution(), Math.max(1, all.size()), true);
            topModel.setRowCount(0);
            int rank = 1;
            for (Student s : pa.ranking()) {
                if (rank > 5) break;
                topModel.addRow(new Object[]{rank++, s.getName(), GradeCalculator.average(s),
                        GradeCalculator.grade(GradeCalculator.percentage(s))});
            }
        }
    }

    // ---------------------------------------------------------------------
    //  Page 2: Add Student
    // ---------------------------------------------------------------------
    class AddPage extends Page {
        private final JTextField id = field();
        private final JTextField name = field();
        private final JTextField dept = field();
        private final JComboBox<String> sem = combo(SEMESTERS);
        private final MarksEditor marks = new MarksEditor();

        AddPage() {
            super("Add Student", "Register a new student and enter marks for multiple subjects");
            Card form = new Card("Student Details");
            Flat col = new Flat(null);
            col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
            col.add(formRow("Student ID (unique)", id));
            col.add(Box.createVerticalStrut(12));
            col.add(formRow("Full Name", name));
            col.add(Box.createVerticalStrut(12));
            col.add(formRow("Department", dept));
            col.add(Box.createVerticalStrut(12));
            col.add(formRow("Semester", sem));
            form.add(col, BorderLayout.CENTER);

            Card marksCard = new Card("Subjects & Marks");
            marksCard.add(marks, BorderLayout.CENTER);
            addRow(split(form, marksCard, 360));

            RoundButton save = new RoundButton("Save Student", ACCENT);
            RoundButton clear = new RoundButton("Clear Form", new Color(0x64748B));
            save.addActionListener(e -> save());
            clear.addActionListener(e -> clear());
            addRow(buttons(save, clear));
        }

        private void save() {
            try {
                Student s = buildStudent(id.getText(), name.getText(), dept.getText(),
                        (String) sem.getSelectedItem(), marks);
                manager.addStudent(s);
                info("Student added", s.getName() + " (" + s.getId() + ") was saved successfully.");
                clear();
            } catch (Exception ex) {
                showError(ex);
            }
        }

        private void clear() {
            id.setText("");
            name.setText("");
            dept.setText("");
            sem.setSelectedIndex(0);
            marks.useDefaultSubjects();
        }

        @Override void refresh() { }
    }

    private Student buildStudent(String id, String name, String dept, String sem, MarksEditor editor)
            throws ValidationException {
        String cleanId = Validation.requireId(id);
        String cleanName = Validation.requireName(name);
        String cleanDept = Validation.requireDepartment(dept);
        int semester = Validation.requireSemester(sem);
        ArrayList<String> names = new ArrayList<>();
        ArrayList<Double> marks = new ArrayList<>();
        editor.collect(names, marks);
        return new Student(cleanId, cleanName, cleanDept, semester, names, marks);
    }

    // ---------------------------------------------------------------------
    //  Page 3: View Students
    // ---------------------------------------------------------------------
    class ViewPage extends Page {
        private final DefaultTableModel model = readOnlyModel(STUDENT_COLUMNS);
        private final JTable table = new JTable(model);
        private final JLabel count = label("", F_BASE, MUTED);

        ViewPage() {
            super("View Students", "All registered students - click a column header to sort");
            styleTable(table);
            studentColumnWidths(table);
            table.setAutoCreateRowSorter(true);
            Card card = new Card(null);
            Flat top = new Flat(new BorderLayout());
            top.add(label("Registered Students", F_H2, TEXT), BorderLayout.WEST);
            top.add(count, BorderLayout.EAST);
            card.add(top, BorderLayout.NORTH);
            card.add(tableScroll(table, 10), BorderLayout.CENTER);
            addRow(card);

            RoundButton report = new RoundButton("View Report", ACCENT);
            RoundButton edit = new RoundButton("Edit Student", TEAL);
            RoundButton delete = new RoundButton("Delete Selected", new Color(0xDC2626));
            RoundButton reload = new RoundButton("Refresh", new Color(0x64748B));
            report.addActionListener(e -> {
                Student s = manager.findById(selectedId(table));
                if (s == null) info("Select a student", "Please click a student row first."); else showReport(s);
            });
            edit.addActionListener(e -> {
                String sid = selectedId(table);
                if (sid == null) info("Select a student", "Please click a student row first.");
                else { showPage("Update Student"); updatePage.loadStudent(sid); }
            });
            delete.addActionListener(e -> deleteWithConfirm(selectedId(table), this::refresh));
            reload.addActionListener(e -> refresh());
            addRow(buttons(report, edit, delete, reload));
        }

        @Override void refresh() {
            ArrayList<Student> all = manager.getAll();
            fillStudentTable(model, all);
            count.setText(all.size() + " student(s)");
        }
    }

    // ---------------------------------------------------------------------
    //  Page 4: Search Student
    // ---------------------------------------------------------------------
    class SearchPage extends Page {
        private final JTextField query = field();
        private final DefaultTableModel model = readOnlyModel(STUDENT_COLUMNS);
        private final JTable table = new JTable(model);
        private final JLabel result = label(" ", F_BASE, MUTED);
        private final JTextArea report = new JTextArea(18, 40);

        SearchPage() {
            super("Search Student", "Find students by their unique ID or by name (partial names work too)");
            Card searchCard = new Card("Search");
            Flat line = new Flat(new BorderLayout(12, 0));
            line.add(query, BorderLayout.CENTER);
            RoundButton go = new RoundButton("Search", ACCENT);
            line.add(go, BorderLayout.EAST);
            Flat col = new Flat(new BorderLayout(0, 8));
            col.add(line, BorderLayout.NORTH);
            col.add(result, BorderLayout.SOUTH);
            searchCard.add(col, BorderLayout.CENTER);
            addRow(searchCard);

            styleTable(table);
            studentColumnWidths(table);
            table.setAutoCreateRowSorter(true);
            Card tableCard = new Card("Results");
            tableCard.add(tableScroll(table, 5), BorderLayout.CENTER);
            addRow(tableCard);

            report.setFont(F_MONO);
            report.setEditable(false);
            report.setMargin(new Insets(12, 14, 12, 14));
            report.setText("Select a student from the results to see the performance summary report.");
            JScrollPane rs = new JScrollPane(report);
            rs.setBorder(new LineBorder(BORDER));
            rs.setPreferredSize(new Dimension(300, 330));
            Card reportCard = new Card("Performance Summary Report");
            reportCard.add(rs, BorderLayout.CENTER);
            addRow(reportCard);

            RoundButton delete = new RoundButton("Delete Selected", new Color(0xDC2626));
            RoundButton edit = new RoundButton("Edit Student", TEAL);
            addRow(buttons(edit, delete));

            go.addActionListener(e -> search());
            query.addActionListener(e -> search());
            query.addKeyListener(new KeyAdapter() {
                @Override public void keyReleased(KeyEvent e) { search(); }
            });
            table.getSelectionModel().addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) return;
                Student s = manager.findById(selectedId(table));
                if (s != null) { report.setText(PerformanceAnalyzer.studentReport(s)); report.setCaretPosition(0); }
            });
            delete.addActionListener(e -> deleteWithConfirm(selectedId(table), this::search));
            edit.addActionListener(e -> {
                String sid = selectedId(table);
                if (sid == null) info("Select a student", "Please click a student in the results first.");
                else { showPage("Update Student"); updatePage.loadStudent(sid); }
            });
        }

        private void search() {
            ArrayList<Student> found = manager.search(query.getText());
            fillStudentTable(model, found);
            result.setText(found.isEmpty() ? "No student matches '" + query.getText().trim() + "'."
                    : found.size() + " student(s) found.");
            if (found.isEmpty()) report.setText("No results.");
        }

        @Override void refresh() { search(); }
    }

    // ---------------------------------------------------------------------
    //  Page 5: Update Student
    // ---------------------------------------------------------------------
    class UpdatePage extends Page {
        private final JTextField idField = field();
        private final JTextField name = field();
        private final JTextField dept = field();
        private final JComboBox<String> sem = combo(SEMESTERS);
        private final MarksEditor marks = new MarksEditor();
        private final RoundButton saveDetails = new RoundButton("Save Details", ACCENT);
        private final RoundButton saveMarks = new RoundButton("Save Marks", TEAL);
        private String loadedId;

        UpdatePage() {
            super("Update Student", "Load a student by ID, then change their details or their subject marks");
            Card find = new Card("Find Student");
            Flat line = new Flat(new BorderLayout(12, 0));
            line.add(idField, BorderLayout.CENTER);
            RoundButton load = new RoundButton("Load Student", ACCENT);
            line.add(load, BorderLayout.EAST);
            find.add(line, BorderLayout.CENTER);
            addRow(find);

            Card details = new Card("Student Details");
            Flat col = new Flat(null);
            col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
            col.add(formRow("Full Name", name));
            col.add(Box.createVerticalStrut(12));
            col.add(formRow("Department", dept));
            col.add(Box.createVerticalStrut(12));
            col.add(formRow("Semester", sem));
            col.add(Box.createVerticalStrut(18));
            col.add(buttons(saveDetails));
            details.add(col, BorderLayout.CENTER);

            Card marksCard = new Card("Subject Marks");
            marksCard.add(marks, BorderLayout.CENTER);
            marksCard.add(buttons(saveMarks), BorderLayout.SOUTH);
            addRow(split(details, marksCard, 360));

            load.addActionListener(e -> loadStudent(idField.getText()));
            idField.addActionListener(e -> loadStudent(idField.getText()));
            saveDetails.addActionListener(e -> saveDetails());
            saveMarks.addActionListener(e -> saveMarks());
            setFormEnabled(false);
        }

        void loadStudent(String id) {
            Student s = manager.findById(id);
            if (s == null) {
                loadedId = null;
                setFormEnabled(false);
                JOptionPane.showMessageDialog(Dashboard.this, "No student found with ID '"
                        + (id == null ? "" : id.trim()) + "'.", "Not found", JOptionPane.WARNING_MESSAGE);
                return;
            }
            loadedId = s.getId();
            idField.setText(s.getId());
            name.setText(s.getName());
            dept.setText(s.getDepartment());
            sem.setSelectedItem(String.valueOf(s.getSemester()));
            marks.setData(s.getSubjectNames(), s.getMarks());
            setFormEnabled(true);
        }

        private void setFormEnabled(boolean on) {
            name.setEnabled(on);
            dept.setEnabled(on);
            sem.setEnabled(on);
            saveDetails.setEnabled(on);
            saveMarks.setEnabled(on);
            marks.setEditable(on);
            if (!on) {
                name.setText("");
                dept.setText("");
                marks.clearAll();
            }
        }

        private void saveDetails() {
            try {
                manager.updateDetails(loadedId, Validation.requireName(name.getText()),
                        Validation.requireDepartment(dept.getText()),
                        Validation.requireSemester((String) sem.getSelectedItem()));
                info("Updated", "Details of " + loadedId + " were updated.");
            } catch (Exception ex) { showError(ex); }
        }

        private void saveMarks() {
            try {
                ArrayList<String> names = new ArrayList<>();
                ArrayList<Double> list = new ArrayList<>();
                marks.collect(names, list);
                manager.updateMarks(loadedId, names, list);
                info("Updated", "Marks of " + loadedId + " were updated.");
            } catch (Exception ex) { showError(ex); }
        }

        @Override void refresh() {
            if (loadedId != null && manager.findById(loadedId) == null) {   // student was deleted meanwhile
                loadedId = null;
                idField.setText("");
                setFormEnabled(false);
            }
        }
    }

    // ---------------------------------------------------------------------
    //  Page 6: Performance Analytics (one student)
    // ---------------------------------------------------------------------
    class AnalyticsPage extends Page {
        private final JComboBox<String> picker = combo(new String[0]);
        private ArrayList<Student> list = new ArrayList<>();
        private final ScoreRing ring = new ScoreRing();
        private final JLabel vTotal = label("-", F_H2, TEXT), vAvg = label("-", F_H2, TEXT),
                vHigh = label("-", F_H2, TEXT), vLow = label("-", F_H2, TEXT),
                vStrong = label("-", F_H2, GOOD.darker()), vWeak = label("-", F_H2, BAD),
                vGrade = label("-", F_H2, TEXT), vStatus = label("-", F_H2, TEXT);
        private final BarChart subjectChart = new BarChart();
        private final JTextArea tips = new JTextArea();
        private boolean loading;

        AnalyticsPage() {
            super("Performance Analytics", "One student's marks, strengths, weaknesses and personalised advice");
            Card pick = new Card("Select Student");
            Flat line = new Flat(new BorderLayout(12, 0));
            line.add(picker, BorderLayout.CENTER);
            RoundButton export = new RoundButton("Export Report (.txt)", TEAL);
            RoundButton view = new RoundButton("Full Report", ACCENT);
            line.add(buttons(view, export), BorderLayout.EAST);
            pick.add(line, BorderLayout.CENTER);
            addRow(pick);

            Card ringCard = new Card("Overall Score");
            ringCard.add(ring, BorderLayout.CENTER);
            Card stats = new Card("Key Metrics");
            JPanel grid = new JPanel(new GridLayout(4, 2, 16, 16));
            grid.setOpaque(false);
            grid.add(mini("Total Marks", vTotal));
            grid.add(mini("Average Marks", vAvg));
            grid.add(mini("Highest Score", vHigh));
            grid.add(mini("Lowest Score", vLow));
            grid.add(mini("Strongest Subject", vStrong));
            grid.add(mini("Weakest Subject", vWeak));
            grid.add(mini("Grade", vGrade));
            grid.add(mini("Result", vStatus));
            stats.add(grid, BorderLayout.CENTER);
            addRow(split(ringCard, stats, 300));

            Card chartCard = new Card("Subject-wise Marks");
            chartCard.add(subjectChart, BorderLayout.CENTER);
            tips.setEditable(false);
            tips.setLineWrap(true);
            tips.setWrapStyleWord(true);
            tips.setOpaque(false);
            tips.setFont(F_INPUT);
            tips.setForeground(TEXT);
            Card tipCard = new Card("Personalised Recommendations");
            tipCard.add(tips, BorderLayout.CENTER);
            addRow(row(chartCard, tipCard));

            picker.addActionListener(e -> { if (!loading) show(selected()); });
            view.addActionListener(e -> {
                Student s = selected();
                if (s == null) info("No student", "Add a student first."); else showReport(s);
            });
            export.addActionListener(e -> {
                Student s = selected();
                if (s == null) { info("No student", "Add a student first."); return; }
                try {
                    File f = manager.getFileManager().saveReport(s.getId() + "_report.txt",
                            PerformanceAnalyzer.studentReport(s));
                    info("Report exported", "Saved to:\n" + f.getAbsolutePath());
                } catch (Exception ex) { showError(ex); }
            });
        }

        private Student selected() {
            int i = picker.getSelectedIndex();
            return i >= 0 && i < list.size() ? list.get(i) : null;
        }

        private void show(Student s) {
            if (s == null) {
                ring.set(0, "-", ACCENT);
                for (JLabel l : new JLabel[]{vTotal, vAvg, vHigh, vLow, vStrong, vWeak, vGrade, vStatus}) l.setText("-");
                subjectChart.setData(new LinkedHashMap<String, Double>(), 100, false);
                tips.setText("Add a student to see personalised recommendations.");
                return;
            }
            double pct = GradeCalculator.percentage(s);
            String grade = GradeCalculator.grade(pct);
            ring.set(pct, grade, gradeColor(grade));
            vTotal.setText(GradeCalculator.fmt(GradeCalculator.total(s)) + " / "
                    + (int) (s.getSubjectCount() * GradeCalculator.MAX_MARK));
            vAvg.setText(GradeCalculator.fmt(GradeCalculator.average(s)));
            vHigh.setText(GradeCalculator.fmt(GradeCalculator.highest(s)));
            vLow.setText(GradeCalculator.fmt(GradeCalculator.lowest(s)));
            vStrong.setText(GradeCalculator.strongestSubject(s));
            vWeak.setText(GradeCalculator.weakestSubject(s));
            vGrade.setText(grade);
            vGrade.setForeground(gradeColor(grade));
            vStatus.setText(GradeCalculator.status(s));
            vStatus.setForeground(GradeCalculator.isPass(pct) ? new Color(0x16A34A) : BAD);

            LinkedHashMap<String, Double> data = new LinkedHashMap<>();
            ArrayList<String> names = s.getSubjectNames();
            ArrayList<Double> marks = s.getMarks();
            for (int i = 0; i < names.size(); i++) data.put(names.get(i), marks.get(i));
            subjectChart.setColorFn(n -> gradeColor(GradeCalculator.grade(data.get(n))));
            subjectChart.setData(data, 100, false);

            StringBuilder sb = new StringBuilder();
            int n = 1;
            for (String t : GradeCalculator.recommendations(s)) sb.append(n++).append(". ").append(t).append("\n\n");
            tips.setText(sb.toString().trim());
        }

        @Override void refresh() {
            Student before = selected();
            loading = true;
            list = manager.getAll();
            picker.removeAllItems();
            int keep = -1;
            for (int i = 0; i < list.size(); i++) {
                Student s = list.get(i);
                picker.addItem(s.getId() + " - " + s.getName());
                if (before != null && s.getId().equals(before.getId())) keep = i;
            }
            if (!list.isEmpty()) picker.setSelectedIndex(Math.max(keep, 0));
            loading = false;
            show(selected());
        }
    }

    // ---------------------------------------------------------------------
    //  Page 7: Class Summary
    // ---------------------------------------------------------------------
    class ClassPage extends Page {
        private final StatCard avg = new StatCard("Class Average", TEAL);
        private final StatCard passed = new StatCard("Students Passed", GOOD);
        private final StatCard failed = new StatCard("Students Failed", BAD);
        private final StatCard rate = new StatCard("Pass Rate", ACCENT);
        private final Highlight best = new Highlight("Highest-performing Student");
        private final Highlight worst = new Highlight("Lowest-performing Student");
        private final BarChart gradeChart = new BarChart();
        private final BarChart subjectChart = new BarChart();
        private final DefaultTableModel rankModel = readOnlyModel(
                new String[]{"Rank", "ID", "Name", "Department", "Average", "Grade", "Status"});

        ClassPage() {
            super("Class Summary", "Class-wide statistics, subject averages, grade distribution and ranking");
            addRow(row(avg, passed, failed, rate));
            addRow(row(best, worst));

            gradeChart.setColorFn(Dashboard::gradeColor);
            Card gradeCard = new Card("Grade Distribution");
            gradeCard.add(gradeChart, BorderLayout.CENTER);
            Card subjectCard = new Card("Subject-wise Class Averages");
            subjectCard.add(subjectChart, BorderLayout.CENTER);
            addRow(row(gradeCard, subjectCard));

            JTable rankTable = new JTable(rankModel);
            styleTable(rankTable);
            colWidth(rankTable, 0, 72);
            colWidth(rankTable, 1, 80);
            colWidth(rankTable, 4, 92);
            colWidth(rankTable, 5, 70);
            colWidth(rankTable, 6, 80);
            Card rankCard = new Card("Student Ranking (by average marks)");
            rankCard.add(tableScroll(rankTable, 8), BorderLayout.CENTER);
            addRow(rankCard);

            RoundButton export = new RoundButton("Export Class Report (.txt)", TEAL);
            export.addActionListener(e -> {
                try {
                    File f = manager.getFileManager().saveReport("class_report.txt",
                            new PerformanceAnalyzer(manager.getAll()).classReport());
                    info("Report exported", "Saved to:\n" + f.getAbsolutePath());
                } catch (Exception ex) { showError(ex); }
            });
            addRow(buttons(export));
        }

        @Override void refresh() {
            ArrayList<Student> all = manager.getAll();
            PerformanceAnalyzer pa = new PerformanceAnalyzer(all);
            avg.setValue(all.isEmpty() ? "-" : GradeCalculator.fmt(pa.classAverage()));
            passed.setValue(String.valueOf(pa.passedCount()));
            failed.setValue(String.valueOf(pa.failedCount()));
            rate.setValue(GradeCalculator.fmt(pa.passRate()) + "%");
            best.set(pa.topper());
            worst.set(pa.lowestPerformer());
            gradeChart.setData(pa.gradeDistribution(), Math.max(1, all.size()), true);
            subjectChart.setColorFn(null);
            subjectChart.setData(pa.subjectAverages(), 100, false);
            rankModel.setRowCount(0);
            int rank = 1;
            for (Student s : pa.ranking()) {
                double pct = GradeCalculator.percentage(s);
                rankModel.addRow(new Object[]{rank++, s.getId(), s.getName(), s.getDepartment(),
                        GradeCalculator.average(s), GradeCalculator.grade(pct), GradeCalculator.status(s)});
            }
        }
    }

    // =====================================================================
    //  4. DIALOGS, DELETE, EXIT
    // =====================================================================
    void info(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /** Turns any exception into a friendly message - the application never crashes. */
    void showError(Exception ex) {
        String msg;
        if (ex instanceof ValidationException) msg = ex.getMessage();
        else if (ex instanceof UncheckedIOException) msg = "File error: " + ex.getCause().getMessage();
        else msg = "Something went wrong: " + ex.getMessage();
        JOptionPane.showMessageDialog(this, msg, "Please check your input", JOptionPane.ERROR_MESSAGE);
    }

    void showReport(Student s) {
        JTextArea area = new JTextArea(PerformanceAnalyzer.studentReport(s));
        area.setFont(F_MONO);
        area.setEditable(false);
        area.setMargin(new Insets(10, 12, 10, 12));
        area.setCaretPosition(0);
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(560, 500));
        JOptionPane.showMessageDialog(this, sp, "Performance Report - " + s.getName(), JOptionPane.PLAIN_MESSAGE);
    }

    void deleteWithConfirm(String id, Runnable afterDelete) {
        if (id == null) {
            info("Select a student", "Please select a student first.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete student " + id + " permanently?\nThis also removes the record from the data file.",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            try {
                manager.deleteStudent(id);
                afterDelete.run();
                info("Deleted", "Student " + id + " was deleted.");
            } catch (Exception ex) { showError(ex); }
        }
    }

    void confirmExit() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Exit SMARTGRADE?\nAll records are already saved automatically.",
                "Exit", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            manager.save();
            dispose();
            System.exit(0);
        }
    }
}
