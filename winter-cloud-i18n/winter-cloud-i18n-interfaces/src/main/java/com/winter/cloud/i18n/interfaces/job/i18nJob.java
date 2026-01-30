package com.winter.cloud.i18n.interfaces.job;

import com.winter.cloud.i18n.application.service.I18nMessageAppService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class I18nJob {
    private final I18nMessageAppService i18nMessageAppService;
    /**
     * 定时任务：重建布隆过滤器
     * <p>
     * 目的：布隆过滤器不支持删除操作，定时重建可以清除已物理删除的历史数据标记，重置误判率。
     * 默认策略：每天凌晨 3 点执行。
     * 配置项：winter.i18n.bloom-rebuild-cron
     * </p>
     */
    @XxlJob("scheduledRebuildBloomFilter")
    public void scheduledRebuildBloomFilter() {
        log.info("开始执行定时任务：重建 I18n 布隆过滤器...");
        i18nMessageAppService.scheduledRebuildBloomFilter();
    }
}
