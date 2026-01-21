package com.winter.cloud.auth.starter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.mybatis.spring.annotation.MapperScan;

import java.util.ArrayList;

/**
 * 认证服务启动类
 */
@SpringBootApplication
@ComponentScan("com.winter.cloud")
@MapperScan("com.winter.**.mapper")
@EnableDiscoveryClient
@EnableDubbo(scanBasePackages = {"com.winter.cloud.auth"})
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
//        // 从环境变量获取 API Key（也可硬编码用于测试）
//        String apiKey = "";
//        if (StrUtil.isBlank(apiKey)) {
//            throw new IllegalArgumentException("NVIDIA_API_KEY environment variable is not set.");
//        }
//
//        String invokeUrl = "https://ai.api.nvidia.com/v1/genai/black-forest-labs/flux.1-schnell";
//
//        // 构建 JSON 请求体
//        JSONObject jsonBody = new JSONObject();
//        jsonBody.set("prompt", "大熊猫打雪仗")
//                .set("width", 1024)
//                .set("height", 1024)
//                .set("seed", 0)
//                .set("steps", 4);
//
//        // 发送 POST 请求
//        HttpResponse response = HttpRequest.post(invokeUrl)
//                .header("Authorization", "Bearer " + apiKey)
//                .header("Accept", "application/json")
//                .header("Content-Type", "application/json")
//                .body(jsonBody.toString())
//                .timeout(60000) // 可选：设置超时（毫秒）
//                .execute();
//
//        ArrayList<String> data = new ArrayList<>();
//        int httpCode = response.getStatus();
//        String body = response.body();
//        JSONObject jsonObject = JSONUtil.parseObj(body);
//        Object o = jsonObject.get("artifacts");
//        JSONArray objects = JSONUtil.parseArray(o);
//        objects.forEach(obj -> {
//            JSONObject artifact = JSONUtil.parseObj(obj);
//            String url = artifact.getStr("base64");
//            data.add(url);
//            System.out.println("Artifact URL: " + url);
//        });
//        // 输出响应体（不包含状态行和 headers，仅 JSON 内容）
//        System.out.println(data);
//
//        // 如果需要检查 HTTP 状态码
//        if (httpCode != 200) {
//            System.err.println("Request failed with HTTP code: " + httpCode);
//        }
    }
}
