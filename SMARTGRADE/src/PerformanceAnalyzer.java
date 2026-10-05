import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PerformanceAnalyzer - class-level analytics plus the printable student report.
 * Create it with the current list of students: new PerformanceAnalyzer(manager.getAll()).
 */
public class PerformanceAnalyzer {
    private final ArrayList<Student> students;

    public PerformanceAnalyzer(List<Student> students) {
        this.students = new ArrayList<>(students);
    }

    public boolean isEmpty() { return students.isEmpty(); }

    /** Overall class average = mean of every student's average marks. */
    public double classAverage() {
        if (students.isEmpty()) return 0;
        double sum = 0;
        for (Student s : students) sum += GradeCalculator.average(s);
        return sum / students.size();
    }

    public Student topper() {
        ArrayList<Student> ranked = ranking();
        return ranked.isEmpty() ? null : ranked.get(0);
    }

    public Student lowestPerformer() {
        ArrayList<Student> ranked = ranking();
        return ranked.isEmpty() ? null : ranked.get(ranked.size() - 1);
    }

    /** Subject name -> class average in that subject (insertion ordered). */
    public LinkedHashMap<String, Double> subjectAverages() {
        LinkedHashMap<String, Double> sums = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        for (Student s : students) {
            ArrayList<String> names = s.getSubjectNames();
            ArrayList<Double> marks = s.getMarks();
            for (int i = 0; i < names.size(); i++) {
                sums.merge(names.get(i), marks.get(i), Double::sum);
                counts.merge(names.get(i), 1, Integer::sum);
            }
        }
        LinkedHashMap<String, Double> averages = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : sums.entrySet()) {
            averages.put(e.getKey(), e.getValue() / counts.get(e.getKey()));
        }
        return averages;
    }

    public int passedCount() {
        int count = 0;
        for (Student s : students) if (GradeCalculator.isPass(GradeCalculator.percentage(s))) count++;
        return count;
    }

    public int failedCount() { return students.size() - passedCount(); }

    public double passRate() {
        return students.isEmpty() ? 0 : passedCount() * 100.0 / students.size();
    }

    /** Grade -> number of students, always in the order A+, A, B, C, D, F. */
    public LinkedHashMap<String, Integer> gradeDistribution() {
        LinkedHashMap<String, Integer> dist = new LinkedHashMap<>();
        for (String g : new String[]{"A+", "A", "B", "C", "D", "F"}) dist.put(g, 0);
        for (Student s : students) {
            dist.merge(GradeCalculator.grade(GradeCalculator.percentage(s)), 1, Integer::sum);
        }
        return dist;
    }

    /** Students sorted from best to worst average (rank 1 = first element). */
    public ArrayList<Student> ranking() {
        ArrayList<Student> ranked = new ArrayList<>(students);
        ranked.sort(Comparator.comparingDouble(GradeCalculator::average).reversed());
        return ranked;
    }

    // ------------------------------------------------------------------
    //  Text reports (shown in dialogs and exported to .txt files)
    // ------------------------------------------------------------------

    public static String studentReport(Student s) {
        String line = "-".repeat(50);
        String bar = "=".repeat(50);
        StringBuilder sb = new StringBuilder();
        sb.append(bar).append('\n');
        sb.append("   SMARTGRADE - STUDENT PERFORMANCE REPORT\n");
        sb.append("   Track. Analyze. Improve.\n");
        sb.append(bar).append('\n');
        sb.append(String.format("Student ID   : %s%n", s.getId()));
        sb.append(String.format("Name         : %s%n", s.getName()));
        sb.append(String.format("Department   : %s%n", s.getDepartment()));
        sb.append(String.format("Semester     : %d%n", s.getSemester()));
        sb.append(line).append('\n');
        sb.append(String.format("%-26s %7s %7s%n", "SUBJECT", "MARKS", "GRADE"));
        ArrayList<String> names = s.getSubjectNames();
        ArrayList<Double> marks = s.getMarks();
        for (int i = 0; i < names.size(); i++) {
            sb.append(String.format("%-26s %7s %7s%n", names.get(i), GradeCalculator.fmt(marks.get(i)),
                    GradeCalculator.grade(marks.get(i))));
        }
        sb.append(line).append('\n');
        double pct = GradeCalculator.percentage(s);
        sb.append(String.format("Total Marks  : %s / %d%n", GradeCalculator.fmt(GradeCalculator.total(s)),
                (int) (s.getSubjectCount() * GradeCalculator.MAX_MARK)));
        sb.append(String.format("Average      : %s%n", GradeCalculator.fmt(GradeCalculator.average(s))));
        sb.append(String.format("Percentage   : %s%%%n", GradeCalculator.fmt(pct)));
        sb.append(String.format("Grade        : %s%n", GradeCalculator.grade(pct)));
        sb.append(String.format("Result       : %s%n", GradeCalculator.status(s)));
        sb.append(String.format("Highest      : %s (%s)%n", GradeCalculator.fmt(GradeCalculator.highest(s)),
                GradeCalculator.strongestSubject(s)));
        sb.append(String.format("Lowest       : %s (%s)%n", GradeCalculator.fmt(GradeCalculator.lowest(s)),
                GradeCalculator.weakestSubject(s)));
        sb.append(String.format("Strongest    : %s%n", GradeCalculator.strongestSubject(s)));
        sb.append(String.format("Weakest      : %s%n", GradeCalculator.weakestSubject(s)));
        sb.append(line).append('\n');
        sb.append("RECOMMENDATIONS\n");
        int n = 1;
        for (String tip : GradeCalculator.recommendations(s)) {
            sb.append(String.format(" %d. %s%n", n++, tip));
        }
        sb.append(bar).append('\n');
        return sb.toString();
    }

    public String classReport() {
        String bar = "=".repeat(50);
        StringBuilder sb = new StringBuilder();
        sb.append(bar).append('\n');
        sb.append("   SMARTGRADE - CLASS SUMMARY REPORT\n");
        sb.append(bar).append('\n');
        if (students.isEmpty()) {
            sb.append("No students registered yet.\n");
            return sb.toString();
        }
        sb.append(String.format("Total students : %d%n", students.size()));
        sb.append(String.format("Class average  : %s%n", GradeCalculator.fmt(classAverage())));
        sb.append(String.format("Passed / Failed: %d / %d%n", passedCount(), failedCount()));
        sb.append(String.format("Top student    : %s (%s)%n", topper().getName(),
                GradeCalculator.fmt(GradeCalculator.average(topper()))));
        sb.append(String.format("Lowest student : %s (%s)%n", lowestPerformer().getName(),
                GradeCalculator.fmt(GradeCalculator.average(lowestPerformer()))));
        sb.append("\nSubject-wise class averages\n");
        for (Map.Entry<String, Double> e : subjectAverages().entrySet()) {
            sb.append(String.format("  %-26s %s%n", e.getKey(), GradeCalculator.fmt(e.getValue())));
        }
        sb.append("\nGrade distribution\n");
        for (Map.Entry<String, Integer> e : gradeDistribution().entrySet()) {
            sb.append(String.format("  %-4s %d%n", e.getKey(), e.getValue()));
        }
        sb.append("\nRanking (by average marks)\n");
        int rank = 1;
        for (Student s : ranking()) {
            sb.append(String.format("  %2d. %-22s %6s  %s%n", rank++, s.getName(),
                    GradeCalculator.fmt(GradeCalculator.average(s)),
                    GradeCalculator.grade(GradeCalculator.percentage(s))));
        }
        sb.append(bar).append('\n');
        return sb.toString();
    }
}
