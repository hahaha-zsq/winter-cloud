职责：编排。从 Repository 取出领域对象，执行领域方法，保存回去。

service : 应用服务接口 (e.g., DictAppService)

impl : 应用服务实现

assembler : DTO 与 Domain Entity 的转换器

event : 订阅领域事件的处理器 (e.g., DictUpdatedHandler)，通常在这里发 MQ 消息或处理副作用。

runner : 启动后的任务 (e.g., CacheWarmUpRunner) 