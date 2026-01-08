2. MQ 生产者 (Producer)
   角色：属于被动适配器（Driven Adapter），是业务逻辑调用的基础设施。为了不让业务层依赖具体的 MQ 实现（如 RocketMQTemplate），需要采用依赖倒置策略。

(1) 生产者接口 (Interface)
模块：winter-cloud-auth-domain 说明：在领域层定义“发送消息”的接口，只规定“我要发什么”，不关心“怎么发”。

推荐代码位置： winter-cloud-auth/winter-cloud-auth-domain/src/main/java/com/winter/cloud/auth/domain/producer (例如在此处创建 MessageProducer.java 接口)

(2) 生产者实现 (Implementation)
模块：winter-cloud-auth-infrastructure 说明：在基础设施层实现上述接口，这里引用具体的 MQ SDK（如 RocketMQTemplate）进行真正的发送。

推荐代码位置： winter-cloud-auth/winter-cloud-auth-infrastructure/src/main/java/com/winter/cloud/auth/infrastructure/producer/impl (例如在此处创建 RocketMQMessageGatewayImpl.java)