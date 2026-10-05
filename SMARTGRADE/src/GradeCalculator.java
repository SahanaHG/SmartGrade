import java.util.ArrayList;
import java.util.Locale;

/**
 * GradeCalculator - all the maths for ONE student.
 * Every method is static, so it can be called as GradeCalculator.total(student).
 *
 * Grading scale (each subject is out of 100):
 *   A+ : 90 and above     A : 80 - 89     B : 70 - 79
 *   C  : 60 - 69          D : 50 - 59     F : below 50  (FAIL)
 */
public class GradeCalculator {
    public static final double MAX_MARK = 100.0;
    public static final double PASS_PERCENTAGE = 50.0;

    /** Formats a number with one decimal place, e.g. 82.5 */
    public static String fmt(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    public static double total(Student s) {
        double sum = 0;
        for (double m : s.getMarks()) {
            sum += m;
        }
        return sum;
    }

    public static double average(Student s) {
        int n = s.getSubjectCount();
        return n == 0 ? 0 : total(s) / n;
    }

    public static double percentage(Student s) {
        int n = s.getSubjectCount();
        return n == 0 ? 0 : total(s) / (n * MAX_MARK) * 100;
    }

    public static double highest(Student s) {
        double best = 0;
        ArrayList<Double> marks = s.getMarks();
        for (int i = 0; i < marks.size(); i++) {
            if (i == 0 || marks.get(i) > best) best = marks.get(i);
        }
        return best;
    }

    public static double lowest(Student s) {
        double worst = 0;
        ArrayList<Double> marks = s.getMarks();
        for (int i = 0; i < marks.size(); i++) {
            if (i == 0 || marks.get(i) < worst) worst = marks.get(i);
        }
        return worst;
    }

    public static String grade(double percentage) {
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B";
        if (percentage >= 60) return "C";
        if (percentage >= 50) return "D";
        return "F";
    }

    public static boolean isPass(double percentage) {
        return percentage >= PASS_PERCENTAGE;
    }

    public static String status(Student s) {
        return isPass(percentage(s)) ? "PASS" : "FAIL";
    }

    public static String strongestSubject(Student s) {
        ArrayList<Double> marks = s.getMarks();
        if (marks.isEmpty()) return "-";
        int best = 0;
        for (int i = 1; i < marks.size(); i++) {
            if (marks.get(i) > marks.get(best)) best = i;
        }
        return s.getSubjectNames().get(best);
    }

    public static String weakestSubject(Student s) {
        ArrayList<Double> marks = s.getMarks();
        if (marks.isEmpty()) return "-";
        int worst = 0;
        for (int i = 1; i < marks.size(); i++) {
            if (marks.get(i) < marks.get(worst)) worst = i;
        }
        return s.getSubjectNames().get(worst);
    }

    /** Rule-based, personalised advice built from the student's own numbers. */
    public static ArrayList<String> recommendations(Student s) {
        ArrayList<String> tips = new ArrayList<>();
        if (s.getSubjectCount() == 0) {
            tips.add("No marks have been entered yet.");
            return tips;
        }
        double pct = percentage(s);
        String strong = strongestSubject(s);
        String weak = weakestSubject(s);

        if (pct >= 90) {
            tips.add("Outstanding performance! Keep your consistent study routine.");
            tips.add("Challenge yourself with advanced topics, projects or competitions in " + strong + ".");
        } else if (pct >= 75) {
            tips.add("Very good performance - you are close to the top grade.");
            tips.add("Give extra revision time to " + weak + " to lift your overall percentage.");
        } else if (pct >= 60) {
            tips.add("Good work, but there is room to grow. Make a weekly revision plan for every subject.");
            tips.add("Practise more problems and past papers in " + weak + ".");
        } else if (pct >= 50) {
            tips.add("You passed, but only just. Focus on the fundamentals and revise regularly.");
            tips.add("Ask your teacher for help in " + weak + ".");
        } else {
            tips.add("At risk: you are below the passing percentage. Meet your teachers and make a study plan now.");
            tips.add("Start with " + weak + " and rebuild the basics step by step.");
        }

        ArrayList<String> names = s.getSubjectNames();
        ArrayList<Double> marks = s.getMarks();
        for (int i = 0; i < marks.size(); i++) {
            if (marks.get(i) < PASS_PERCENTAGE && !names.get(i).equals(weak)) {
                tips.add("Needs urgent attention: " + names.get(i) + " (" + fmt(marks.get(i)) + " marks).");
            }
        }
        double gap = highest(s) - lowest(s);
        if (gap >= 30) {
            tips.add("Your marks are uneven (gap of " + fmt(gap) + " marks). Balance your study time across subjects.");
        }
        if (pct < 90 && !strong.equals(weak)) {
            tips.add("Use the study methods that work for " + strong + " in your weaker subjects.");
        }
        return tips;
    }
}
