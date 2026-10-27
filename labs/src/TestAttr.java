import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import java.util.Hashtable;

public class TestAttr {
    public static void main(String[] a) throws Exception {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, a[0]);
        Context ctx = new InitialContext(env);
        DirContext dctx = (DirContext) ctx.lookup("");
        Attributes attrs = dctx.getAttributes("", new String[]{"*", "+"});
        NamingEnumeration<? extends javax.naming.directory.Attribute> e = attrs.getAll();
        while (e.hasMore()) {
            javax.naming.directory.Attribute at = e.next();
            Object v = at.get();
            System.out.println(at.getID() + " = " + (v instanceof byte[] ? "bytes:" + ((byte[]) v).length : v));
        }
        System.out.println("trustSerialData=" + System.getProperty("com.sun.jndi.ldap.object.trustSerialData", "<unset>"));
    }
}
