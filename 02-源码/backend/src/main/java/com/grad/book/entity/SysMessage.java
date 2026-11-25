package com.grad.book.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 站内消息
 */
@Data
@TableName("sys_message")
public class SysMessage {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人 sys_user.id */
    private Long receiverId;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 类型 订单/退换/库存/审核/系统 */
    private String msgType;

    /** 业务类型 order/return/stock/review */
    private String bizType;

    /** 业务主键 */
    private Long bizId;

    /** 跳转路径 */
    private String linkUrl;

    /** 是否已读 0未读 1已读 */
    private Integer readFlag;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 0 否 1 是 */
    @TableLogic
    private Integer deleted;
}
