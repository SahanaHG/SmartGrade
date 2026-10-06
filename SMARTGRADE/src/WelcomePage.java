import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * WelcomePage - Minimalist, elegant splash/front page for Student Grade Tracker.
 * Matches the clean dark presentation: title, subtitle, developer credits,
 * and seamless entry to the main Dashboard.
 */
public class WelcomePage extends JFrame {

    private static final Color DARK_BG = new Color(0x0F172A);
    private static final Color TEXT_WHITE = new Color(0xFFFFFF);
    private static final Color TEXT_MUTED = new Color(0x94A3B8);
    private static final Color ACCENT_BLUE = new Color(0x38BDF8);
    private static final Color BTN_BLUE = new Color(0x2563EB);

    private final StudentManager manager;

    public WelcomePage(StudentManager manager) {
        super("Student Grade Tracker");
        this.manager = manager;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(920, 580);
        setMinimumSize(new Dimension(720, 480));
        setResizable(true);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(DARK_BG);
        root.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        // 1. Main Title
        JLabel title = new JLabel("Student Grade Tracker");
        title.setFont(new Font("SansSerif", Font.BOLD, 42));
        title.setForeground(TEXT_WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 2. Subtitle
        JLabel subtitle = new JLabel("Academic Management System");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 20));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 3. Developer Credit
        JLabel dev = new JLabel("Developed by Sahana HG");
        dev.setFont(new Font("SansSerif", Font.PLAIN, 15));
        dev.setForeground(new Color(0xE2E8F0));
        dev.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 4. Internship Tag
        JLabel intern = new JLabel("CodeAlpha Java Programming Internship");
        intern.setFont(new Font("SansSerif", Font.PLAIN, 14));
        intern.setForeground(ACCENT_BLUE);
        intern.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 5. Enter Dashboard Button
        JButton enterBtn = new EnterButton("Enter Dashboard →", BTN_BLUE);
        enterBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        enterBtn.addActionListener(e -> enterDashboard());

        JLabel hint = new JLabel("(Click button or anywhere to continue)");
        hint.setFont(new Font("SansSerif", Font.PLAIN, 12));
        hint.setForeground(new Color(0x64748B));
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerPanel.add(title);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(subtitle);
        centerPanel.add(Box.createVerticalStrut(32));
        centerPanel.add(dev);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(intern);
        centerPanel.add(Box.createVerticalStrut(32));
        centerPanel.add(enterBtn);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(hint);

        root.add(centerPanel);

        // Click anywhere to enter dashboard
        MouseAdapter clickHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                enterDashboard();
            }
        };
        root.addMouseListener(clickHandler);
        centerPanel.addMouseListener(clickHandler);

        // Press Enter or Space to enter dashboard
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
                    enterDashboard();
                }
            }
        });
        setFocusable(true);

        setContentPane(root);
    }

    private void enterDashboard() {
        new Dashboard(manager).setVisible(true);
        dispose();
    }

    /** Rounded custom button */
    static class EnterButton extends JButton {
        private final Color base;
        private boolean hover;

        EnterButton(String text, Color base) {
            super(text);
            this.base = base;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("SansSerif", Font.BOLD, 15));
            setBorder(new EmptyBorder(12, 28, 12, 28));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(hover ? base.brighter() : base);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static void main(String[] args) {
        Main.main(args);
    }
}
