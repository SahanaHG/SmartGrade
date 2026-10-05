import java.util.ArrayList;
import java.util.HashSet;

/**
 * Validation - every input check of the application lives here.
 * Each method either returns the cleaned value or throws a ValidationException
 * with a meaningful message, so the GUI never crashes on bad input.
 */
public class Validation {

    /** ID: 3-12 letters/digits, no spaces (example: S101). Returned in UPPER CASE. */
    public static String requireId(String raw) throws ValidationException {
        String id = raw == null ? "" : raw.trim();
        if (id.isEmpty()) {
            throw new ValidationException("Student ID cannot be empty.");
        }
        if (!id.matches("[A-Za-z0-9]{3,12}")) {
            throw new ValidationException("Invalid Student ID '" + id
                    + "'.\nUse 3 to 12 letters or digits only, without spaces (example: S101).");
        }
        return id.toUpperCase();
    }

    /** Name: letters and spaces (also . ' -) are allowed, e.g. "Ananya Rao". */
    public static String requireName(String raw) throws ValidationException {
        String name = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) {
            throw new ValidationException("Student name cannot be empty.");
        }
        if (!name.matches("\\p{L}[\\p{L} .'-]{1,49}")) {
            throw new ValidationException("Invalid name '" + name
                    + "'.\nUse letters and spaces only (2 to 50 characters).");
        }
        return name;
    }

    public static String requireDepartment(String raw) throws ValidationException {
        String dept = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        if (dept.isEmpty()) {
            throw new ValidationException("Department cannot be empty.");
        }
        if (!dept.matches("\\p{L}[\\p{L} &.-]{1,49}")) {
            throw new ValidationException("Invalid department '" + dept + "'. Use letters and spaces only.");
        }
        return dept;
    }

    /** Semester must be a whole number from 1 to 8. */
    public static int requireSemester(String raw) throws ValidationException {
        try {
            int sem = Integer.parseInt(raw == null ? "" : raw.trim());
            if (sem < 1 || sem > 8) {
                throw new ValidationException("Semester must be between 1 and 8.");
            }
            return sem;
        } catch (NumberFormatException e) {
            throw new ValidationException("Semester must be a number between 1 and 8.");
        }
    }

    public static String requireSubject(String raw) throws ValidationException {
        String subject = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        if (subject.isEmpty()) {
            throw new ValidationException("Subject name cannot be empty.");
        }
        if (!subject.matches("[A-Za-z0-9][A-Za-z0-9 &+.-]{0,39}")) {
            throw new ValidationException("Invalid subject name '" + subject
                    + "'.\nUse letters, digits and spaces only (max 40 characters).");
        }
        return subject;
    }

    /**
     * Converts the marks typed by the user into a number.
     * Handles: empty text, letters instead of numbers, negative marks, marks above 100.
     */
    public static double parseMark(String subject, String raw) throws ValidationException {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            throw new ValidationException("Marks for '" + subject + "' cannot be empty.");
        }
        if (!text.matches("-?\\d+(\\.\\d+)?")) {
            throw new ValidationException("Marks for '" + subject + "' must be a number, but you entered '"
                    + text + "'.");
        }
        double mark = Double.parseDouble(text);
        if (mark < 0 || mark > 100) {
            throw new ValidationException("Marks for '" + subject + "' must be between 0 and 100 (you entered "
                    + text + ").");
        }
        return mark;
    }

    /** The same subject cannot appear twice for one student. */
    public static void requireNoDuplicates(ArrayList<String> subjects) throws ValidationException {
        HashSet<String> seen = new HashSet<>();
        for (String s : subjects) {
            if (!seen.add(s.toLowerCase())) {
                throw new ValidationException("Subject '" + s + "' is entered more than once.");
            }
        }
    }
}
