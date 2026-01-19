# Winter Cloud I18n Client (Smart SDK)

`winter-cloud-i18n-client` 是 Winter Cloud 国际化服务的**富客户端 SDK**。它不仅仅是一个简单的远程调用封装，而是内置了**多级缓存架构**的高性能组件。

该模块旨在解决国际化场景下“读取极其频繁”的性能瓶颈，通过直接读取 Redis 缓存、本地布隆过滤器和分布式锁机制，极大地减轻服务端压力，实现毫秒级的消息获取。

## 核心特性

* **多级缓存架构**：优先读取 Redis 缓存，减少 RPC 调用。
* **防缓存穿透**：内置 **Bloom Filter（布隆过滤器）** 检查，拦截数据库中不存在的 Key，防止无效请求打穿到数据库。
* **防缓存击穿**：使用 **Redisson 分布式锁**，在热点 Key 失效时，严格控制并发，只允许一个线程回源查询，其他线程自旋等待。
* **防缓存雪崩**：回写缓存时采用**随机过期时间 (Random TTL)**，避免同一时刻大量 Key 集体失效。
* **高性能兜底**：具备自旋等待（Spin Wait）和双重检查锁（DCL）机制，最大程度保证请求成功率。

---

## 快速开始 (Quick Start)

### 1. 引入依赖

在需要使用国际化功能的微服务（如 `winter-cloud-order`, `winter-cloud-user`）的 `pom.xml` 中引入本模块：

```xml
<dependency>
    <groupId>com.winter</groupId>
    <artifactId>winter-cloud-i18n-client</artifactId>
    <version>${winter-cloud.version}</version>
</dependency>
```
### 1. 2. 必须配置：提供回源实现 (I18nMessageInfo)

本 SDK 采用 SPI 模式解耦数据源。在缓存未命中时，需要通过 I18nMessageInfo 接口回源查询真实数据。
调用方必须在 Spring 容器中提供该接口的实现 Bean。

注意：如果不提供此 Bean，Spring 启动时会报错 NoSuchBeanDefinitionException: I18nMessageInfo。