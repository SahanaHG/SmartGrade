import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * TestRunner - console tests for every item of the CodeAlpha testing checklist.
 * Run it from IntelliJ (right-click > Run 'TestRunner.main()') or from the command line.
 * It uses a temporary file, so your real data/students.csv is never touched.
 */
public class TestRunner {
    static int passed = 0, failed = 0;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
        if (ok) passed++; else failed++;
    }

    static boolean rejects(Runnable0 r) {
        try { r.run(); return false; } catch (ValidationException e) { return true; }
    }

    interface Runnable0 { void run() throws ValidationException; }

    static Student make(String id, String name, double... marks) {
        ArrayList<String> subjects = new ArrayList<>();
        ArrayList<Double> list = new ArrayList<>();
        for (int i = 0; i < marks.length; i++) { subjects.add("Subject" + (i + 1)); list.add(marks[i]); }
        return new Student(id, name, "Computer Science", 3, subjects, list);
    }

    public static void main(String[] args) throws Exception {
        File tmp = File.createTempFile("smartgrade", ".csv");
        tmp.delete();
        StudentManager manager = new StudentManager(new FileManager(tmp.getPath()));

        System.out.println("--- Validation tests ---");
        check("Empty student name is rejected", rejects(() -> Validation.requireName("   ")));
        check("Name with digits is rejected", rejects(() -> Validation.requireName("R0han99")));
        check("Name with spaces is accepted", "Ananya Rao".equals(safe(() -> Validation.requireName("  Ananya   Rao "))));
        check("Empty ID is rejected", rejects(() -> Validation.requireId("")));
        check("Invalid ID (symbols/space) is rejected", rejects(() -> Validation.requireId("S 1@")));
        check("Valid ID accepted and upper-cased", "S101".equals(safe(() -> Validation.requireId("s101"))));
        check("Marks below zero (-5) rejected", rejects(() -> Validation.parseMark("DAA", "-5")));
        check("Marks above 100 (101) rejected", rejects(() -> Validation.parseMark("DAA", "101")));
        check("Letters instead of marks rejected", rejects(() -> Validation.parseMark("DAA", "abc")));
        check("Empty marks rejected", rejects(() -> Validation.parseMark("DAA", "")));
        check("Boundary marks 0 and 100 accepted", safe2(() -> Validation.parseMark("DAA", "0") == 0
                && Validation.parseMark("DAA", "100") == 100));
        check("Semester 9 rejected", rejects(() -> Validation.requireSemester("9")));
        check("Duplicate subject names rejected",
                rejects(() -> Validation.requireNoDuplicates(new ArrayList<>(Arrays.asList("DBMS", "dbms")))));

        System.out.println("--- Grade calculation tests ---");
        Student s = make("S900", "Test Student", 90, 80, 70, 60, 50);   // total 350, avg 70
        check("Total marks = 350", GradeCalculator.total(s) == 350);
        check("Average marks = 70", GradeCalculator.average(s) == 70);
        check("Percentage = 70", GradeCalculator.percentage(s) == 70);
        check("Highest = 90 / Lowest = 50", GradeCalculator.highest(s) == 90 && GradeCalculator.lowest(s) == 50);
        check("Strongest = Subject1, Weakest = Subject5",
                GradeCalculator.strongestSubject(s).equals("Subject1") && GradeCalculator.weakestSubject(s).equals("Subject5"));
        check("Grade boundaries 90/80/70/60/50/49.9 -> A+/A/B/C/D/F",
                GradeCalculator.grade(90).equals("A+") && GradeCalculator.grade(80).equals("A")
                        && GradeCalculator.grade(70).equals("B") && GradeCalculator.grade(60).equals("C")
                        && GradeCalculator.grade(50).equals("D") && GradeCalculator.grade(49.9).equals("F"));
        check("50% is PASS, 49.9% is FAIL", GradeCalculator.isPass(50) && !GradeCalculator.isPass(49.9));
        check("Recommendations are generated", !GradeCalculator.recommendations(s).isEmpty());

        System.out.println("--- Student management tests ---");
        manager.loadSampleData();
        check("Multiple records stored (8 sample students)", manager.size() == 8);
        check("Loading sample data twice adds no duplicates", manager.loadSampleData() == 0 && manager.size() == 8);
        boolean dup = rejects(() -> manager.addStudent(make("S101", "Another Person", 50, 60)));
        check("Duplicate student ID rejected", dup && manager.size() == 8);
        check("Search by ID (case-insensitive)", manager.findById("s103") != null
                && manager.findById("s103").getName().equals("Meera Nair"));
        check("Search by partial name", manager.search("kumar").size() == 1);
        check("Search with unknown text returns nothing", manager.search("zzzz").isEmpty());
        manager.updateDetails("S102", "Rohan K", "Information Science", 4);
        check("Update details works", manager.findById("S102").getName().equals("Rohan K")
                && manager.findById("S102").getSemester() == 4);
        manager.updateMarks("S102", new ArrayList<>(Arrays.asList("DAA", "DBMS")), new ArrayList<>(Arrays.asList(100.0, 90.0)));
        check("Update marks works (avg 95 -> A+)", GradeCalculator.average(manager.findById("S102")) == 95
                && GradeCalculator.grade(GradeCalculator.percentage(manager.findById("S102"))).equals("A+"));

        System.out.println("--- Class analytics tests ---");
        PerformanceAnalyzer pa = new PerformanceAnalyzer(manager.getAll());
        check("Passed + Failed = total students", pa.passedCount() + pa.failedCount() == manager.size());
        check("Exactly 1 failing sample student (Karthik)", pa.failedCount() == 1);
        check("Topper is Rohan K (avg 95)", pa.topper().getId().equals("S102"));
        check("Lowest performer is Karthik Gowda", pa.lowestPerformer().getName().equals("Karthik Gowda"));
        check("Ranking is sorted descending", GradeCalculator.average(pa.ranking().get(0))
                >= GradeCalculator.average(pa.ranking().get(1)));
        int gradeSum = 0;
        for (int v : pa.gradeDistribution().values()) gradeSum += v;
        check("Grade distribution adds up to total students", gradeSum == manager.size());
        check("Subject averages calculated", pa.subjectAverages().containsKey("DAA"));

        System.out.println("--- File persistence tests ---");
        StudentManager reloaded = new StudentManager(new FileManager(tmp.getPath()));
        check("Records are loaded again after 'restart'", reloaded.size() == 8);
        check("Updated data survived restart", reloaded.findById("S102").getName().equals("Rohan K"));
        check("Marks survived restart", reloaded.findById("S101").getMarks().size() == 5);

        System.out.println("--- Delete tests ---");
        check("Delete existing student returns true", manager.deleteStudent("S106"));
        check("Delete unknown student returns false", !manager.deleteStudent("NOPE1"));
        StudentManager afterDelete = new StudentManager(new FileManager(tmp.getPath()));
        check("Deleted student is also removed from the file", afterDelete.size() == 7 && afterDelete.findById("S106") == null);

        tmp.delete();
        System.out.println("\nRESULT: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    interface Getter { String get() throws ValidationException; }
    interface BoolGetter { boolean get() throws ValidationException; }

    static String safe(Getter g) { try { return g.get(); } catch (ValidationException e) { return "ERR:" + e.getMessage(); } }
    static boolean safe2(BoolGetter g) { try { return g.get(); } catch (ValidationException e) { return false; } }
}
