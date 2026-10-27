import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;

/**
 * 时间盲打模板：static 块 sleep 指定毫秒，纯 Java 实现，不依赖 /bin/sh。
 */
public class TransletSleep extends AbstractTranslet {
    static {
        try {
            Thread.sleep(Long.parseLong("MILLIS_PLACEHOLDER"));
        } catch (Exception e) {
            // 静默失败
        }
    }

    public void transform(DOM document, DTMAxisIterator iterator, SerializationHandler handler)
            throws TransletException {
    }

    public void transform(DOM document, SerializationHandler[] handlers) throws TransletException {
    }
}
