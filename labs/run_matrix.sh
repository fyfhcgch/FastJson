#!/usr/bin/env bash
# 启动多版本 fastjson 靶场矩阵（docker openjdk:8, host 网络, 端口 180xx）。
# 用法: ./run_matrix.sh [start|stop|restart]
set -e
cd "$(dirname "$0")"
IMG=eclipse-temurin:8-jdk
# 模拟漏洞时代环境：旧 JDK(信任远程 codebase/序列化数据) + 旧 Tomcat(未移除 forceString)
CP="build:libs/fastjson-VERSION.jar:libs/commons-dbcp-1.4.jar:libs/commons-pool-1.6.jar:libs/c3p0-0.9.5.4.jar:libs/mchange-commons-java-0.2.19.jar:libs/tomcat-embed-core-8.5.0.jar:libs/tomcat-embed-el-8.5.0.jar:libs/xbean-reflect-4.20.jar"
JVM="-Dcom.sun.jndi.ldap.object.trustURLCodebase=true -Dcom.sun.jndi.ldap.object.trustSerialData=true"

stop_all() {
  docker ps -aq --filter "name=fjlab-" | xargs -r docker rm -f >/dev/null
}

start_all() {
  i=1
  start() { # version mode port [extra_jvm_flags]
    local ver=$1 mode=$2 port=$3 flags=${4:-}
    local name=fjlab-$port
    docker rm -f $name >/dev/null 2>&1 || true
    docker run -d --name $name --network host -v "$PWD":/lab -w /lab \
      $IMG sh -c "java $JVM $flags -cp \"${CP/VERSION/$ver}\" FastjsonLab $port $mode" >/dev/null
    echo "  $port  fastjson $ver  mode=$mode $flags"
  }
  echo "启动靶场矩阵:"
  start 1.2.24 parse 18001
  start 1.2.24 npf    18002
  start 1.2.31 parse 18003 "-Dfastjson.parser.autoTypeSupport=true"
  start 1.2.36 parse 18004 "-Dfastjson.parser.autoTypeSupport=true"
  start 1.2.41 parse 18005 "-Dfastjson.parser.autoTypeSupport=true"
  start 1.2.42 parse 18006 "-Dfastjson.parser.autoTypeSupport=true"
  start 1.2.43 parse 18007 "-Dfastjson.parser.autoTypeSupport=true"
  start 1.2.45 parse 18008 "-Dfastjson.parser.autoTypeSupport=true"
  start 1.2.47 parse 18009
  start 1.2.47 npf    18010
  start 1.2.48 parse 18011 "-Dfastjson.parser.autoTypeSupport=true"
  start 1.2.62 parse 18012
  start 1.2.66 parse 18013
  start 1.2.68 parse 18014
  start 1.2.68 bean  18015
  start 1.2.80 parse 18016
  start 1.2.83 parse 18017
  start 1.2.83 bean  18018
  start 1.2.24 safe  18019
  start 1.2.68 safe  18020
  start 1.2.62 bean  18021
  # 18026-18028: 新链靶场（yso 序列化 / PostgreSQL JDBC / H2 INIT）
  CP_CC="$CP:libs/commons-collections-3.2.1.jar"
  docker rm -f fjlab-18026 >/dev/null 2>&1 || true
  docker run -d --name fjlab-18026 --network host -v "$PWD":/lab -w /lab \
    $IMG sh -c "java $JVM -cp \"${CP_CC/VERSION/1.2.47}\" FastjsonLab 18026 parse" >/dev/null
  echo "  18026  fastjson 1.2.47  mode=parse +commons-collections-3.2.1 (yso)"
  docker rm -f fjlab-18027 >/dev/null 2>&1 || true
  docker run -d --name fjlab-18027 --network host -v "$PWD":/lab -w /lab \
    $IMG sh -c "java -cp \"${CP/VERSION/1.2.68}:libs/postgresql-42.2.25.jar\" FastjsonLab 18027 ds" >/dev/null
  echo "  18027  fastjson 1.2.68  mode=ds +postgresql-42.2.25 (pg jdbc)"
  docker rm -f fjlab-18028 >/dev/null 2>&1 || true
  docker run -d --name fjlab-18028 --network host -v "$PWD":/lab -w /lab \
    $IMG sh -c "java -Dfastjson.parser.autoTypeSupport=true -cp \"${CP/VERSION/1.2.31}:libs/h2-1.4.200.jar\" FastjsonLab 18028 ds" >/dev/null
  echo "  18028  fastjson 1.2.31  mode=ds +h2-1.4.200 (h2 init)"
  # 18022: 1.2.47 wrap 二段式（--param 测试），单独启动
  # 18023: 1.2.68 + mysql-connector（MySQL JDBC 组合链）
  CP_MYSQL="$CP:libs/mysql-connector-java-8.0.19.jar"
  docker rm -f fjlab-18023 >/dev/null 2>&1 || true
  docker run -d --name fjlab-18023 --network host -v "$PWD":/lab -w /lab \
    $IMG sh -c "java $JVM -cp \"${CP_MYSQL/VERSION/1.2.68}\" FastjsonLab 18023 parse" >/dev/null
  echo "  18023  fastjson 1.2.68  mode=parse +mysql-connector-8.0.19"
  # 18024: 1.2.83 Spring Boot FatJar（@JSONType 资源探测链）
  python3 build_fatjar.py 1.2.83
  docker rm -f fjlab-18024 >/dev/null 2>&1 || true
  docker run -d --name fjlab-18024 --network host -v "$PWD":/lab -w /lab \
    $IMG sh -c "java $JVM -jar fastjson-1.2.83-fatjar.jar 18024 parse" >/dev/null
  echo "  18024  fastjson 1.2.83  mode=parse (Spring Boot FatJar)"
  sleep 3
  echo "就绪检查:"
  for p in $(seq 18001 18020); do
    curl -s -m 3 -o /dev/null -w "$p %{http_code}\n" -d '{"a":1}' http://127.0.0.1:$p/ || echo "$p FAIL"
  done
}

case "${1:-start}" in
  stop) stop_all ;;
  restart) stop_all; start_all ;;
  *) start_all ;;
esac
