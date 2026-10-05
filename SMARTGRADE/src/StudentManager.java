import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * StudentManager - the "brain" that keeps the ArrayList of students.
 * Add / update / delete / search all happen here, and every change is
 * written to the CSV file immediately, so no data is ever lost.
 */
public class StudentManager {
    private final ArrayList<Student> students;
    private final FileManager fileManager;

    public StudentManager(FileManager fileManager) {
        this.fileManager = fileManager;
        this.students = fileManager.load();      // load previously saved records
    }

    public FileManager getFileManager() { return fileManager; }

    /** Adds a student; refuses duplicate IDs. */
    public void addStudent(Student student) throws ValidationException {
        if (findById(student.getId()) != null) {
            throw new ValidationException("Duplicate ID: a student with ID " + student.getId()
                    + " already exists.\nEvery student needs a unique ID.");
        }
        students.add(student);
        persist();
    }

    /** Finds one student by ID (ignores upper/lower case). Returns null if not found. */
    public Student findById(String id) {
        if (id == null) return null;
        for (Student s : students) {
            if (s.getId().equalsIgnoreCase(id.trim())) return s;
        }
        return null;
    }

    /** Search by ID or by (part of) the name. An empty query returns everyone. */
    public ArrayList<Student> search(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        ArrayList<Student> result = new ArrayList<>();
        for (Student s : students) {
            if (q.isEmpty() || s.getId().toLowerCase().contains(q) || s.getName().toLowerCase().contains(q)) {
                result.add(s);
            }
        }
        return result;
    }

    public void updateDetails(String id, String name, String department, int semester) throws ValidationException {
        Student s = require(id);
        s.setName(name);
        s.setDepartment(department);
        s.setSemester(semester);
        persist();
    }

    public void updateMarks(String id, ArrayList<String> subjects, ArrayList<Double> marks) throws ValidationException {
        Student s = require(id);
        s.setSubjects(subjects, marks);
        persist();
    }

    /** Removes the student from memory AND from the file. */
    public boolean deleteStudent(String id) {
        Student s = findById(id);
        if (s == null) return false;
        students.remove(s);
        persist();
        return true;
    }

    /** Returns a copy of the list so outside code cannot damage it. */
    public ArrayList<Student> getAll() { return new ArrayList<>(students); }

    public int size() { return students.size(); }

    public void save() { persist(); }

    /** Adds 8 demo students (skips IDs that already exist). Returns how many were added. */
    public int loadSampleData() {
        String[] subjects = {"DAA", "UNIX Shell Programming", "DBMS", "Mathematics", "English"};
        Object[][] demo = {
                {"S101", "Ananya Rao", 92, 88, 95, 90, 85},
                {"S102", "Rohan Kumar", 78, 82, 74, 80, 76},
                {"S103", "Meera Nair", 85, 91, 88, 79, 93},
                {"S104", "Arjun Reddy", 65, 58, 70, 62, 68},
                {"S105", "Priya Sharma", 88, 76, 92, 84, 90},
                {"S106", "Karthik Gowda", 42, 38, 55, 47, 40},
                {"S107", "Divya Shetty", 72, 68, 75, 70, 81},
                {"S108", "Imran Khan", 55, 61, 52, 58, 64}
        };
        int added = 0;
        for (Object[] row : demo) {
            if (findById((String) row[0]) != null) continue;
            ArrayList<Double> marks = new ArrayList<>();
            for (int i = 2; i < row.length; i++) marks.add(((Integer) row[i]).doubleValue());
            students.add(new Student((String) row[0], (String) row[1], "Computer Science", 3,
                    new ArrayList<>(Arrays.asList(subjects)), marks));
            added++;
        }
        if (added > 0) persist();
        return added;
    }

    private Student require(String id) throws ValidationException {
        Student s = findById(id);
        if (s == null) throw new ValidationException("No student found with ID " + id + ".");
        return s;
    }

    private void persist() {
        try {
            fileManager.save(students);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
