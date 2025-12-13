#!/bin/bash

error_exit () {
  echo "ERROR: $1 !!"
  exit 1
}

find_java_home() {
  case "`uname`" in
    Darwin)
      JAVA_HOME=$(/usr/libexec/java_home)
      ;;
    *)
      JAVA_HOME=$(dirname $(dirname $(readlink -f $(which javac))))
      ;;
  esac
}

find_java_home

[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=$HOME/jdk/java
[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=/usr/java
[ ! -e "$JAVA_HOME/bin/java" ] && error_exit "Please set the JAVA_HOME variable in your environment, We need java(x64)!"

export JAVA_HOME
export JAVA="$JAVA_HOME/bin/java"
export BASE_DIR=$(dirname $0)/..
export CLASSPATH=.:${BASE_DIR}/conf:${CLASSPATH}

Xms="${RMQ_BROKER_XMS:-128m}"
Xmx="${RMQ_BROKER_XMX:-128m}"
Xmn="${RMQ_BROKER_XMN:-64m}"
MaxDirectMemorySize="${RMQ_BROKER_DIRECT_MEM:-128m}"

JAVA_OPT="${JAVA_OPT} -server -Xms${Xms} -Xmx${Xmx} -Xmn${Xmn}"
JAVA_OPT="${JAVA_OPT} -XX:+UseG1GC -XX:-AlwaysPreTouch -XX:MaxDirectMemorySize=${MaxDirectMemorySize}"
JAVA_OPT="${JAVA_OPT} -XX:-OmitStackTraceInFastThrow -XX:-UseLargePages"
JAVA_OPT="${JAVA_OPT} -Djava.ext.dirs=${JAVA_HOME}/jre/lib/ext:${BASE_DIR}/lib"
JAVA_OPT="${JAVA_OPT} ${JAVA_OPT_EXT}"
JAVA_OPT="${JAVA_OPT} -cp ${CLASSPATH}"

exec $JAVA ${JAVA_OPT} $@

