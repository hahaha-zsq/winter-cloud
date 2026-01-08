package com.winter.cloud.common.log;
import com.winter.cloud.common.util.Context;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
/**
 * TraceId 日志转换器
 * 用于在 Log4j2 日志中输出 TraceId
 */
// 定义Log4j2插件，名称为"TtlPatternConverter"，类别为PatternConverter.CATEGORY
@Plugin(name = "TtlPatternConverter", category = PatternConverter.CATEGORY)
// 定义转换器关键字，支持%ttl和%TTL两种写法，ConverterKeys 的值就是 log4j2.xml 里使用的占位符名
@ConverterKeys({"ttl","TTL"})
public class TtlPatternConverter extends LogEventPatternConverter {

    // 存储TraceId的变量名，用于在日志中输出
    private final String variableName;

    /**
     * 构造函数
     */
    protected TtlPatternConverter(String name, String style, String variableName) {
        super(name, style);
        this.variableName = variableName;
    }

    /**
     * 创建TTL转换器实例的工厂方法
     *
     * @param config Log4j2配置对象
     * @param options 配置选项数组，options 参数就是你在 log4j2.xml 的 PatternLayout 里 占位符后面写的附加配置，如options 就是 log4j2 解析 %traceId{xxx,yyy} 时，传进来的 xxx, yyy
     * @return TtlPatternConverter实例
     */
    @PluginFactory
    public static TtlPatternConverter newInstance(final Configuration config, final String[] options) {
        // 默认获取traceId变量
        String variableName = "traceId";
        // 如果配置了选项参数，则使用第一个参数作为变量名称
        if (options != null && options.length > 0) {
            variableName = options[0];
        }
        // 创建并返回TtlPatternConverter实例
        return new TtlPatternConverter("ttl", "ttl", variableName);
    }

    /**
     * 格式化日志事件，将TTL变量值添加到日志输出中
     *
     * @param event 日志事件对象
     * @param toAppendTo 用于构建日志输出的StringBuilder
     */
    @Override
    public void format(LogEvent event, StringBuilder toAppendTo) {
        String value = null;

        // 根据变量名获取对应的TTL值
        // 当前仅支持traceId变量，后续可以扩展支持更多变量
        if ("traceId".equals(variableName)) {
            // 从Context中获取traceId值
            value = Context.getTraceId();
        }

        // 如果从TTL中没有获取到有效值，尝试从Log4j2的ThreadContext中获取
        // 提供双重保障，确保在TTL不可用时仍能获取到上下文信息
        if (value == null || value.trim().isEmpty()) {
            // 从日志事件的上下文数据中获取对应变量的值
            value = event.getContextData().getValue(variableName);
        }

        // 如果还是没有获取到有效值，使用默认值"-"表示缺失
        // 避免日志中出现null值，保持日志格式的一致性
        if (value == null || value.trim().isEmpty()) {
            value = "-";
        }

        // 将获取到的值追加到日志输出中
        toAppendTo.append(value);
    }
}
