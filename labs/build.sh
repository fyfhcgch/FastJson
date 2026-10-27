#!/usr/bin/env bash
# 下载多版本 fastjson jar + dbcp/BCEL 依赖 + Tomcat BeanFactory/EL 依赖，编译靶场。
set -e
cd "$(dirname "$0")"
mkdir -p libs build
MV=https://maven.aliyun.com/repository/public

# fastjson 多版本
for v in 1.2.24 1.2.31 1.2.36 1.2.41 1.2.42 1.2.43 1.2.45 1.2.47 1.2.48 1.2.62 1.2.66 1.2.68 1.2.80 1.2.83; do
  f=libs/fastjson-$v.jar
  [ -f "$f" ] || curl -sfL -o "$f" $MV/com/alibaba/fastjson/$v/fastjson-$v.jar
done

# BCEL 链依赖（commons-dbcp 1.4 / commons-pool 1.6）
[ -f libs/commons-dbcp-1.4.jar ] || curl -sfL -o libs/commons-dbcp-1.4.jar $MV/commons-dbcp/commons-dbcp/1.4/commons-dbcp-1.4.jar
[ -f libs/commons-pool-1.6.jar ] || curl -sfL -o libs/commons-pool-1.6.jar $MV/commons-pool/commons-pool/1.6/commons-pool-1.6.jar

# c3p0（JNDI 备选载体测试）
[ -f libs/c3p0-0.9.5.4.jar ] || curl -sfL -o libs/c3p0-0.9.5.4.jar $MV/com/mchange/c3p0/0.9.5.4/c3p0-0.9.5.4.jar
[ -f libs/mchange-commons-java-0.2.19.jar ] || curl -sfL -o libs/mchange-commons-java-0.2.19.jar $MV/com/mchange/mchange-commons-java/0.2.19/mchange-commons-java-0.2.19.jar

# Tomcat BeanFactory + EL（LDAP el 模式测试）
[ -f libs/tomcat-embed-core-8.5.100.jar ] || curl -sfL -o libs/tomcat-embed-core-8.5.100.jar $MV/org/apache/tomcat/embed/tomcat-embed-core/8.5.100/tomcat-embed-core-8.5.100.jar
[ -f libs/tomcat-embed-el-8.5.100.jar ] || curl -sfL -o libs/tomcat-embed-el-8.5.100.jar $MV/org/apache/tomcat/embed/tomcat-embed-el/8.5.100/tomcat-embed-el-8.5.100.jar

# 编译（class 版本 52，兼容 openjdk:8 运行）
javac --release 8 -encoding UTF-8 -cp "libs/fastjson-1.2.83.jar" -d build src/com/vuln/EvilException.java src/LabBean.java src/FastjsonLab.java
echo BUILD_OK
