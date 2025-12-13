# Redis 本地安装（Docker）

## 启动
在项目根目录执行：
```bash
docker compose -f docker/redis/compose.yml up -d
```

## 验证
```bash
docker ps --filter name=redis
docker exec redis redis-cli ping
```

## 连接信息（宿主机）
- 地址：`127.0.0.1`
- 端口：`6379`

## 停止与清理
停止：
```bash
docker compose -f docker/redis/compose.yml down
```

清理（会删掉 Redis 持久化数据）：
```bash
rm -rf docker/redis/data
```

