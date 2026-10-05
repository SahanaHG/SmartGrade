import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * FileManager - saves and loads students using plain CSV text (Java File I/O).
 *
 * File format (data/students.csv), one student per line:
 *   id,name,department,semester,subject1:mark1;subject2:mark2;...
 * Example:
 *   S101,Ananya Rao,Computer Science,3,DAA:92.0;DBMS:95.0;UNIX Shell Programming:88.0
 */
public class FileManager {
    private static final String HEADER = "id,name,department,semester,subjects";
    private final File dataFile;

    public FileManager(String path) {
        this.dataFile = new File(path);
    }

    /** Writes ALL students to the file (also used after update / delete). */
    public void save(List<Student> students) throws IOException {
        File parent = dataFile.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataFile, StandardCharsets.UTF_8))) {
            writer.write(HEADER);
            writer.newLine();
            for (Student s : students) {
                writer.write(toCsv(s));
                writer.newLine();
            }
        }
    }

    /** Reads the file at start-up. A missing file simply means "no students yet". */
    public ArrayList<Student> load() {
        ArrayList<Student> students = new ArrayList<>();
        if (!dataFile.exists()) {
            return students;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(dataFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("id,")) continue;   // skip header / empty lines
                try {
                    students.add(fromCsv(line));
                } catch (RuntimeException e) {
                    System.err.println("Skipping damaged line: " + line);  // never crash on a bad line
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read " + dataFile + ": " + e.getMessage());
        }
        return students;
    }

    /** Saves a text report into the reports/ folder and returns the created file. */
    public File saveReport(String fileName, String text) throws IOException {
        File folder = new File(dataFile.getAbsoluteFile().getParentFile().getParentFile(), "reports");
        folder.mkdirs();
        File out = new File(folder, fileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(out, StandardCharsets.UTF_8))) {
            writer.write(text);
        }
        return out;
    }

    private String toCsv(Student s) {
        StringBuilder subjects = new StringBuilder();
        ArrayList<String> names = s.getSubjectNames();
        ArrayList<Double> marks = s.getMarks();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) subjects.append(';');
            subjects.append(names.get(i)).append(':').append(marks.get(i));
        }
        return s.getId() + "," + s.getName() + "," + s.getDepartment() + "," + s.getSemester() + "," + subjects;
    }

    private Student fromCsv(String line) {
        String[] parts = line.split(",", 5);
        ArrayList<String> names = new ArrayList<>();
        ArrayList<Double> marks = new ArrayList<>();
        if (parts.length == 5 && !parts[4].isBlank()) {
            for (String pair : parts[4].split(";")) {
                String[] nm = pair.split(":");
                names.add(nm[0].trim());
                marks.add(Double.parseDouble(nm[1].trim()));
            }
        }
        return new Student(parts[0].trim(), parts[1].trim(), parts[2].trim(),
                Integer.parseInt(parts[3].trim()), names, marks);
    }
}
