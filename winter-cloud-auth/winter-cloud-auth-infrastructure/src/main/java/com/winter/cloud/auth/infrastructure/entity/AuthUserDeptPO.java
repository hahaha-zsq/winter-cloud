package com.winter.cloud.auth.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 用户角色关联表
 */
@Data
@Builder
@TableName(value = "sys_user_dept")
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class AuthUserDeptPO implements Serializable {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 用户id
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 部门id
     */
    @TableField(value = "dept_id")
    private Long deptId;

    private static final long serialVersionUID = 1L;
}