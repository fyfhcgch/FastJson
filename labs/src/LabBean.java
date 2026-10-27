/** expectClass 链测试 Bean：detail 字段为 Exception 类型（非 10 基类判定触发点）。 */
public class LabBean {
    private Exception detail;

    public Exception getDetail() { return detail; }
    public void setDetail(Exception d) { this.detail = d; }

    @Override
    public String toString() { return "LabBean{detail=" + detail + "}"; }
}
