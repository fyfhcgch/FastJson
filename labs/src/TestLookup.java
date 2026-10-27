import javax.naming.InitialContext;

public class TestLookup {
    public static void main(String[] a) throws Exception {
        try {
            Object o = new InitialContext().lookup(a[0]);
            System.out.println("LOOKUP OK: " + o);
        } catch (Throwable t) {
            System.out.println("ERR: " + t);
            Throwable c = t.getCause();
            while (c != null) { System.out.println("  caused: " + c); c = c.getCause(); }
        }
    }
}
