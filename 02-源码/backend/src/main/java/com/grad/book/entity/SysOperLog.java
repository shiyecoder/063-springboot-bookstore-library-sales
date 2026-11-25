package com.grad.book.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 操作日志
 */
@Data
@TableName("sys_oper_log")
public class SysOperLog {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模块 */
    private String module;

    /** 操作类型 新增/修改/审核/发货 */
    private String operType;

    /** 业务编号 */
    private String bizNo;

    /** 内容摘要 */
    private String content;

    /** 操作人 sys_user.id */
    private Long operId;

    /** 操作人姓名 */
    private String operName;

    /** 操作时间 */
    private LocalDateTime operTime;

    /** IP */
    private String ip;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 0 否 1 是 */
    @TableLogic
    private Integer deleted;
}
