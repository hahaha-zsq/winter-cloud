package com.winter.cloud.auth.interfaces.job;

import com.winter.cloud.auth.application.service.IconAppService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class Job {

    private final IconAppService iconAppService;
    // 可参考Sample示例执行器中的 "com.xxl.job.executor.jobhandler.SampleXxlJob" ，如下：
    @XxlJob("demoJobHandler")
    public void demoJobHandler() throws Exception {

        // 获取xxljob页面填入的参数，参数时=是字符串类型
        String jobParam = XxlJobHelper.getJobParam();
        // 获取分页广播类型时当前服务的分片序号
//        int shardIndex = XxlJobHelper.getShardIndex();
        // 获取分页广播类型时所有的分片序号
//        int shardTotal = XxlJobHelper.getShardTotal();
        String[] paramArr = jobParam.split(",");
        //控制台输出日志
        log.info("myXxlJobHandler execute...");
        for (String s : paramArr) {
            log.info("参数：{}", s);
            iconAppService.insert(s);
        }
    }
}
