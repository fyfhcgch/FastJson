import java.io.InputStream;

public class TestProbe {
    public static void main(String[] args) throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String[] ress = {
            "jar:http://2130706433:8000/probe2!/POC.class",   // 冒号形态资源
            "jar/http://2130706433:8000/probe2!/POC.class",   // 点号形态资源
        };
        for (String res : ress) {
            long t0 = System.currentTimeMillis();
            try {
                InputStream is = cl.getResourceAsStream(res);
                int n = 0;
                if (is != null) { n = is.available(); is.close(); }
                System.out.println((is != null ? "[DL-OK] " : "[NULL]  ") + res.substring(4, 12)
                        + " bytes=" + n + " ms=" + (System.currentTimeMillis() - t0));
            } catch (Throwable t) {
                System.out.println("[ERR] " + t);
            }
        }
        // 列出缓存
        java.io.File tmp = new java.io.File("/tmp");
        int c = 0;
        for (java.io.File f : tmp.listFiles()) if (f.getName().startsWith("jar_cache")) c++;
        System.out.println("jar_cache count=" + c);
        Thread.sleep(1000);
    }
}
