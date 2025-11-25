import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttendanceManager {

    private static final String DB_URL  = "jdbc:mysql://127.0.0.1:3306/attendance_db?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";   // change if needed
    private static final String DB_PASS = "root";   // change to your actual password

    private Connection conn;

    public AttendanceManager() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            System.out.println("Database connected successfully.");
        } catch (Exception e) {
            System.out.println("Failed to connect to database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ---------- Lecturer login ----------
    public boolean validateLecturerLogin(String username, String password) {
        if (conn == null) {
            System.out.println("DB connection is null in validateLecturerLogin");
            return false;
        }

        String sql = "SELECT lecturer_id FROM lecturers WHERE username=? AND password=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Error in validateLecturerLogin: " + e.getMessage());
            return false;
        }
    }

    // ---------- Students CRUD ----------
    public void addStudent(String name, int rollNo) {
        addStudent(name, rollNo, "1234");
    }

    public void addStudent(String name, int rollNo, String password) {
        if (conn == null) {
            System.out.println("DB connection is null in addStudent");
            return;
        }

        String sql = "INSERT INTO students (name, roll_no, password) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, rollNo);
            ps.setString(3, password);
            ps.executeUpdate();
            System.out.println("Student added: " + name + " (Roll: " + rollNo + ")");
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                System.out.println("Roll number already exists: " + rollNo);
            } else {
                System.out.println("Error adding student: " + e.getMessage());
            }
        }
    }

    public List<Student> getStudents() {
        List<Student> list = new ArrayList<>();
        if (conn == null) {
            System.out.println("DB connection is null in getStudents");
            return list;
        }

        String sql = "SELECT student_id, name, roll_no, total_classes, attended_classes FROM students ORDER BY roll_no";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Student s = new Student(
                        rs.getInt("student_id"),
                        rs.getString("name"),
                        rs.getInt("roll_no"),
                        rs.getInt("total_classes"),
                        rs.getInt("attended_classes")
                );
                list.add(s);
            }
        } catch (SQLException e) {
            System.out.println("Error fetching students: " + e.getMessage());
        }
        return list;
    }

    public Student getStudentByRollNo(int rollNo) {
        if (conn == null) {
            System.out.println("DB connection is null in getStudentByRollNo");
            return null;
        }

        String sql = "SELECT student_id, name, roll_no, total_classes, attended_classes " +
                     "FROM students WHERE roll_no=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rollNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Student(
                            rs.getInt("student_id"),
                            rs.getString("name"),
                            rs.getInt("roll_no"),
                            rs.getInt("total_classes"),
                            rs.getInt("attended_classes")
                    );
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching student by roll: " + e.getMessage());
        }
        return null;
    }

    public void deleteStudent(int rollNo) {
        if (conn == null) {
            System.out.println("DB connection is null in deleteStudent");
            return;
        }

        String sql = "DELETE FROM students WHERE roll_no=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rollNo);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                System.out.println("No student found with roll no: " + rollNo);
            } else {
                System.out.println("Student deleted (Roll: " + rollNo + ")");
            }
        } catch (SQLException e) {
            System.out.println("Error deleting student: " + e.getMessage());
        }
    }

    // ---------- Attendance ----------
    public boolean markAttendance(int rollNo, int totalClasses, int attendedClasses) {
        if (conn == null) {
            System.out.println("DB connection is null in markAttendance");
            return false;
        }

        String sql = "UPDATE students SET total_classes=?, attended_classes=? WHERE roll_no=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, totalClasses);
            ps.setInt(2, attendedClasses);
            ps.setInt(3, rollNo);

            int rows = ps.executeUpdate();
            if (rows == 0) {
                System.out.println("No student found with roll no: " + rollNo);
                return false;
            } else {
                System.out.println("Attendance updated (Roll: " + rollNo + ")");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error updating attendance: " + e.getMessage());
            return false;
        }
    }

    public void close() {
        if (conn != null) {
            try {
                conn.close();
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                System.out.println("Error closing DB connection: " + e.getMessage());
            }
        }
    }
}
