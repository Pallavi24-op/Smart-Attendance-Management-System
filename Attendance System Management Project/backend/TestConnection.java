import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestConnection {
    public static void main(String[] args) {
        // ✅ Update these based on your system
        String url = "jdbc:mysql://127.0.0.1:3306/attendance_db?useSSL=false&allowPublicKeyRetrieval=true";
        String username = "root";
        String password = "Ac@01212002";

        System.out.println("Attempting to connect to MySQL...");

        try {
            // Load MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish connection
            Connection conn = DriverManager.getConnection(url, username, password);

            System.out.println(" Database connected successfully!");
            conn.close();

        } catch (ClassNotFoundException e) {
            System.out.println(" MySQL JDBC Driver not found. Make sure mysql-connector-j.jar is in your classpath.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println(" Connection failed: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println(" Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
