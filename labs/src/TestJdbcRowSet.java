public class TestJdbcRowSet {
    public static void main(String[] a) throws Exception {
        com.sun.rowset.JdbcRowSetImpl rs = new com.sun.rowset.JdbcRowSetImpl();
        rs.setDataSourceName(a[0]);
        try {
            rs.setAutoCommit(true);
            System.out.println("AUTOCOMMIT OK");
        } catch (Throwable t) {
            System.out.println("ERR: " + t);
            Throwable c = t.getCause();
            while (c != null) { System.out.println("  caused by: " + c); c = c.getCause(); }
        }
    }
}
