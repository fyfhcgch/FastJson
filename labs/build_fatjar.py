#!/usr/bin/env python3
"""构建 Spring Boot FatJar 形态的 fastjson 靶场（@JSONType 资源探测链复现用）。

布局（Spring Boot 2.x JarLauncher 规范）：
  META-INF/MANIFEST.MF        Main-Class=org.springframework.boot.loader.JarLauncher
  org/springframework/boot/loader/**   spring-boot-loader 解压内容
  BOOT-INF/classes/**         靶场应用类（FastjsonLab 等）
  BOOT-INF/lib/fastjson-*.jar 指定版本 fastjson

LaunchedURLClassLoader 的 jar:http fallback 是 @JSONType 探测链（1.2.68-1.2.83）
RCE 的必要条件之一。
"""

import sys
import zipfile
from pathlib import Path

LABS = Path(__file__).resolve().parent


def build(fastjson_ver: str, out: Path) -> None:
    fastjson_jar = LABS / "libs" / f"fastjson-{fastjson_ver}.jar"
    loader_jar = LABS / "libs" / "spring-boot-loader-2.7.18.jar"
    build_cls = LABS / "build"
    assert fastjson_jar.exists() and loader_jar.exists() and build_cls.exists()

    manifest = "\r\n".join([
        "Manifest-Version: 1.0",
        "Main-Class: org.springframework.boot.loader.JarLauncher",
        "Start-Class: FastjsonLab",
        "Spring-Boot-Version: 2.7.18",
        "",
    ])
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("META-INF/MANIFEST.MF", manifest)
        # 目录条目（Spring Boot loader 依赖目录项识别嵌套 archive 根）
        for d in ("BOOT-INF/", "BOOT-INF/classes/", "BOOT-INF/lib/"):
            di = zipfile.ZipInfo(d)
            di.compress_type = zipfile.ZIP_STORED
            di.external_attr = 0o40755 << 16 | 0x10
            z.writestr(di, b"")
        with zipfile.ZipFile(loader_jar) as lz:
            for info in lz.infolist():
                if info.filename == "META-INF/MANIFEST.MF":
                    continue
                if info.filename.endswith("/"):
                    di = zipfile.ZipInfo(info.filename)
                    di.compress_type = zipfile.ZIP_STORED
                    di.external_attr = 0o40755 << 16 | 0x10
                    z.writestr(di, b"")
                    continue
                z.writestr(info.filename, lz.read(info.filename))
        for cls in build_cls.rglob("*.class"):
            z.writestr("BOOT-INF/classes/" + cls.relative_to(build_cls).as_posix(),
                       cls.read_bytes())
        # Spring Boot 规范：嵌套 jar 必须以 STORED（不压缩）方式写入
        info = zipfile.ZipInfo("BOOT-INF/lib/" + fastjson_jar.name)
        info.compress_type = zipfile.ZIP_STORED
        z.writestr(info, fastjson_jar.read_bytes())
    print(f"[fatjar] {out} ({out.stat().st_size} bytes)")


if __name__ == "__main__":
    ver = sys.argv[1] if len(sys.argv) > 1 else "1.2.83"
    build(ver, LABS / f"fastjson-{ver}-fatjar.jar")
