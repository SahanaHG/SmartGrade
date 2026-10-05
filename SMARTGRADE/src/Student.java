import java.util.ArrayList;

/**
 * Student - holds the information of ONE student (the "model" class).
 * Fields are private (encapsulation) and accessed through getters/setters.
 * Subject names and marks are stored in two ArrayLists that share the same index:
 * subjectNames.get(i) has the mark marks.get(i).
 */
public class Student {
    private String id;
    private String name;
    private String department;
    private int semester;
    private ArrayList<String> subjectNames;
    private ArrayList<Double> marks;

    public Student(String id, String name, String department, int semester,
                   ArrayList<String> subjectNames, ArrayList<Double> marks) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.semester = semester;
        this.subjectNames = new ArrayList<>(subjectNames);
        this.marks = new ArrayList<>(marks);
    }

    // ---------- getters ----------
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public int getSemester() { return semester; }
    public ArrayList<String> getSubjectNames() { return new ArrayList<>(subjectNames); }
    public ArrayList<Double> getMarks() { return new ArrayList<>(marks); }
    public int getSubjectCount() { return subjectNames.size(); }

    // ---------- setters (the ID is never changed once created) ----------
    public void setName(String name) { this.name = name; }
    public void setDepartment(String department) { this.department = department; }
    public void setSemester(int semester) { this.semester = semester; }

    /** Replaces all subjects and marks in one go (used by "Update Marks"). */
    public void setSubjects(ArrayList<String> subjectNames, ArrayList<Double> marks) {
        this.subjectNames = new ArrayList<>(subjectNames);
        this.marks = new ArrayList<>(marks);
    }
}
