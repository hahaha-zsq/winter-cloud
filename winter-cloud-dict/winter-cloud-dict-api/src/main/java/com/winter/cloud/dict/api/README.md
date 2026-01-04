# DDD 中 API 模块中的请求对象与返回对象说明文档

## 1. 文档目的

本说明文档用于明确在基于领域驱动设计（DDD）的多模块架构中，**请求对象（输入模型）** 和 **返回对象（输出模型）** 的命名规范、职责划分及其在
**API 模块** 中的放置原则，确保团队在 RPC/前后端分离模式下的模型一致性和可维护性。

---

## 2. API 模块的定位

API 模块是领域上下文对外暴露的 **通信契约（Contract）**，在多模块或微服务架构中被其他服务、前端网关或外部系统直接依赖。

API 模块的典型职责包括：

* 定义对外暴露的 RPC 或 Facade 接口
* 定义请求对象（Command / Query / Request）
* 定义返回对象（DTO / VO / Response）
* 定义对外暴露的枚举、错误码等

API 模块**不包含**：

* 领域模型（Entity、Aggregate、ValueObject）
* 领域服务
* 仓储接口
* 基础设施实现（DAO、Mapper等）

**原因：** API 模块是契约，一旦发布必须保持兼容，不应放入会频繁发生业务变更的内容。

---

## 3. 请求对象（输入模型）

请求对象用于表示客户端（前端或其他服务）传递给系统的数据，是纯数据结构，禁止包含业务逻辑。

### 3.1 常用命名方式

* Command（命令）：用在修改类操作，例如：

    * CreateOrderCommand
    * UpdateUserInfoCommand
* Query（查询）：用在查询行为，例如：

    * GetOrderDetailQuery
    * ListUserQuery
* Request：偏向 REST 风格，例如：

    * LoginRequest

### 3.2 特性

* 必须为可序列化类型
* 不包含业务逻辑
* 字段表达客户端所需传输的数据

### 3.3 放置位置

```
api/
 └── dto/
      ├── command/
      ├── query/
      └── request/
```

---

## 4. 返回对象（输出模型）

返回对象用于系统对客户端的输出展示，是无业务逻辑的纯数据结构。

### 4.1 常用命名方式

* DTO（数据传输对象）
* VO（视图对象）
* Response（响应对象）

具体是如何命名的，看团队的命名规范，一般来说ddd是推荐返回对象使用DTO，在前后端分离的模式中，“返回对象”通常叫 VO（View
Object），表示是视图层的表现对象

示例：

* OrderDetailDTO
* UserInfoVO
* LoginResponse

### 4.2 特性

* 用于前端或外部系统展示
* 可能与领域模型不同，体现防腐层作用
* 不包含业务逻辑

### 4.3 放置位置

```
api/
 └── dto/
      └── response/
```

---

## 5. facade：用于存放 RPC 接口

* 接口文件命名末尾一般用 Facade 或 Service
* 不包含代码实现（实现放在 application 层）
* 参数和返回值都使用 API 模块中的 DTO/Command/Query

```
api/
 ├── facade/
 │     └── OrderFacade.java          ← RPC 接口（对外服务）
 ├── dto/
 │     ├── command/
 │     ├── query/
 │     └── response/
 ├── enums/
 └── error/
```

## 6. API 模块推荐目录结构

以下为推荐的 DDD 风格 API 层结构：

```
order-api/
 ├── facade/
 │     └── OrderFacade.java
 ├── dto/
 │     ├── command/
 │     │     └── CreateOrderCommand.java
 │     ├── query/
 │     │     └── GetOrderDetailQuery.java
 │     └── response/
 │           └── OrderDetailDTO.java
 ├── enums/
 │     └── OrderStatusEnum.java
 └── exception/
       └── ApiErrorCode.java
```

---

## 6. 请求与返回对象的设计原则

### 6.1 不暴露领域模型

API 模块不得直接返回领域对象（如 Entity、AggregateRoot），避免外部依赖内部领域模型导致的耦合。

### 6.2 防腐层职责

API DTO 是**防腐层**的一部分，用于隔离外部系统输入输出与内部领域结构之间的差异。

### 6.3 演进友好

* 字段可增加、避免删除
* 外部系统升级 API 时不破坏兼容性

### 6.4 严禁业务逻辑

所有逻辑必须在 Application 或 Domain 层实现。

---

## 7. 示例

### CreateOrderCommand.java

```java
public class CreateOrderCommand {
    private Long userId;
    private List<Long> productIds;
    private String remark;
}
```

### OrderDetailDTO.java

```java
public class OrderDetailDTO {
    private Long orderId;
    private Long userId;
    private List<OrderItemDTO> items;
    private String status;
}
```

### OrderFacade.java

```java
public interface OrderFacade {
    OrderDetailDTO getOrderDetail(GetOrderDetailQuery query);

    Long createOrder(CreateOrderCommand command);
}
```

---

## 8. 总结

在 DDD 多模块体系中：

* **请求对象（Command/Query/Request）** 和 **返回对象（DTO/VO/Response）** 均应放在 API 模块
* 它们共同构成系统对外的稳定通信契约
* 它们必须是纯数据结构，无业务逻辑
* API 层不暴露领域模型，执行防腐职责

这保证了模块内聚、上下文边界清晰、对外依赖稳定，符合 DDD 的最佳实践。
