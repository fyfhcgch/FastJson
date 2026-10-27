import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Fastjson 本地靶场：POST 任意 JSON 到任意路径，回显解析结果或异常原文（供指纹探测）。
 * 用法: java [-Dfastjson.parser.autoTypeSupport=true] FastjsonLab <port> <mode>
 * mode:
 *   parse  JSON.parse / parseObject(带 AutoTypeSupport 特性开关)
 *   npf    parseObject + Feature.SupportNonPublicField（templates 链）
 *   bean   parseObject(json, LabBean.class)（expectClass 链，detail 字段为 Exception）
 *   safe   parseObject + Feature.SafeMode（safeMode 场景）
 */
public class FastjsonLab {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        String mode = args.length > 1 ? args[1] : "parse";
        HttpServer s = HttpServer.create(new InetSocketAddress(port), 0);
        s.createContext("/", exchange -> {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            try (InputStream is = exchange.getRequestBody()) {
                int n;
                while ((n = is.read(chunk)) > 0) buf.write(chunk, 0, n);
            }
            String json = new String(buf.toByteArray(), StandardCharsets.UTF_8);
            String out;
            try {
                if (mode.equals("npf")) {
                    Object o = JSON.parseObject(json, Object.class, Feature.SupportNonPublicField);
                    out = "OK " + o;
                } else if (mode.equals("bean")) {
                    Object o = JSON.parseObject(json, LabBean.class);
                    out = "OK " + o;
                } else if (mode.equals("wrap")) {
                    // 二段式解析：外层取 "json" 字段再做 fastjson 解析（--param 注入点测试用）
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> outer =
                            (java.util.Map<String, Object>) JSON.parse(json);
                    Object inner = outer.get("json");
                    Object o = JSON.parse(String.valueOf(inner));
                    out = "OK " + o;
                } else if (mode.equals("safe")) {
                    Object o;
                    try {
                        // Feature.SafeMode 仅 >=1.2.68 存在，旧版本反射兜底
                        Feature safe = (Feature) Feature.class.getField("SafeMode").get(null);
                        o = JSON.parseObject(json, safe);
                    } catch (NoSuchFieldException e) {
                        o = JSON.parse(json);
                    }
                    out = "OK " + o;
                } else {
                    // autoTypeSupport 由 -Dfastjson.parser.autoTypeSupport=true 控制
                    // （ParserConfig 构造时读取该系统属性，全版本通用）
                    Object o = JSON.parse(json);
                    out = "OK " + o;
                }
            } catch (Exception e) {
                out = "ERR " + e;
            }
            byte[] ob = out.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, ob.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(ob); }
        });
        s.start();
        System.out.println("Lab mode=" + mode + " port=" + port
                + " autoType=" + Boolean.getBoolean("fastjson.parser.autoTypeSupport"));
    }
}
