package com.winter.cloud.common.response;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PageAndOrderDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private List<OrderDTO> orders = new ArrayList<>(); // 修复：添加字段名 'orders' 并初始化

    @Data
    public static class OrderDTO {
        private String field;
        private String order = "ASC"; // 假设 CommonConstants.Order.ASC 是字符串 "ASC"
    }
}