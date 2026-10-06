import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main - the application entry point.
 * It loads the saved data and opens the Dashboard window.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
                // the default look and feel is fine too
            }
            StudentManager manager = new StudentManager(new FileManager("data/students.csv"));
            new WelcomePage(manager).setVisible(true);
        });
    }
}
