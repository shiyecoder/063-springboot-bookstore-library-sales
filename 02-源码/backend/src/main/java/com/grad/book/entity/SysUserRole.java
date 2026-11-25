package com.grad.book.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户角色
 */
@Data
@TableName("sys_user_role")
public class SysUserRole {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 sys_user.id */
    private Long userId;

    /** 角色 sys_role.id */
    private Long roleId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 0 否 1 是 */
    @TableLogic
    private Integer deleted;
}
