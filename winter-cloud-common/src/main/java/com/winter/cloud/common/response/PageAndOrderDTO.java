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
        /* 当 ORDER BY 存在多个排序字段且方向不同时，字段的先后顺序有非常重要的影响。排序是按字段从左到右依次进行的
         SELECT * FROM scores ORDER BY name ASC, score DESC;
         先按 name 升序 → 所有 Alice 在前，Bob 在后；
         在 Alice 内部，按 score DESC → 90 > 85，所以 Math 在前；
         在 Bob 内部，按 score DESC → 95 > 90，所以 Eng 在前。
         */
        private Integer sequence = 1; // 存在多个排序时，表示排序的顺序
    }
}