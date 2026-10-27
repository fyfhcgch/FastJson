import javax.naming.Context;
import javax.naming.InitialContext;

public class TestLookup {
    public static void main(String[] args) throws Exception {
        try {
            Object o = new InitialContext().lookup(args[0]);
            System.out.println("LOOKUP OK: " + o);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
