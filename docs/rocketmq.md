# RocketMQ 本地安装（Docker）

本仓库提供一套开箱即用的 RocketMQ（NameServer + Broker + Dashboard）本地环境，适合开发/学习。

## 前置条件
- 已安装并启动 Docker（你当前环境是 `colima`，也可以用 Docker Desktop）。

## 启动
在项目根目录执行：
```bash
docker compose -f docker/rocketmq/compose.yml up -d
```

## 验证
```bash
docker ps --filter name=rocketmq
docker logs -n 50 rocketmq-namesrv
docker logs -n 50 rocketmq-broker
```

### Dashboard
- 访问：`http://localhost:18080`
- NameServer：已通过容器环境变量配置为 `namesrv:9876`

## 客户端连接信息（宿主机上跑 Java）
- NameServer：`127.0.0.1:9876`

> 说明：Broker 配置里设置了 `brokerIP1=host.docker.internal`，既方便宿主机访问，也方便 Dashboard（容器内）访问。

## 停止与清理
停止：
```bash
docker compose -f docker/rocketmq/compose.yml down
```

## 资源占用（可选）
本配置为了适配低内存 Docker 环境，默认给 NameServer/Broker 设置了较小的 JVM 参数（通过覆盖容器内 `runserver.sh`/`runbroker.sh` 实现）。

如需调大内存，可在 `docker/rocketmq/compose.yml` 给对应服务加环境变量，例如：
- `RMQ_BROKER_XMX=512m`
- `RMQ_BROKER_DIRECT_MEM=512m`

清理（会删掉 RocketMQ 持久化数据）：
```bash
rm -rf docker/rocketmq/data
```
