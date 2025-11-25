public class Student {
    private int id;
    private String name;
    private int rollNo;
    private int totalClasses;
    private int attendedClasses;

    public Student(int id, String name, int rollNo, int totalClasses, int attendedClasses) {
        this.id = id;
        this.name = name;
        this.rollNo = rollNo;
        this.totalClasses = totalClasses;
        this.attendedClasses = attendedClasses;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getRollNo() { return rollNo; }
    public int getTotalClasses() { return totalClasses; }
    public int getAttendedClasses() { return attendedClasses; }

    public double getAttendancePercentage() {
        if (totalClasses == 0) return 0.0;
        return (attendedClasses * 100.0) / totalClasses;
    }
}
