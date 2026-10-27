import java.sql.DriverManager;

public class TestMysql {
    public static void main(String[] a) throws Exception {
        try {
            java.sql.Connection c = DriverManager.getConnection(a[0]);
            System.out.println("CONNECT OK");
            java.sql.Statement st = c.createStatement();
            try { st.executeQuery("SHOW SESSION STATUS"); } catch (Throwable t) {
                System.out.println("Q ERR: " + t.getMessage());
            }
        } catch (Throwable t) {
            String m = t.getMessage();
            System.out.println("CONN ERR: " + (m == null ? t : m.substring(0, Math.min(300, m.length()))));
        }
    }
}
