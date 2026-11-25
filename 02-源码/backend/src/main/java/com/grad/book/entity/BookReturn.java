package com.grad.book.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退换单
 */
@Data
@TableName("book_return")
public class BookReturn {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 退换单号 TH+yyMMdd+4位序号 */
    private String returnNo;

    /** 订单 book_order.id */
    private Long orderId;

    /** 图书 book.id，须属于该订单 */
    private Long bookId;

    /** 退货退款/换货/仅退款 */
    private String returnType;

    /** 申请数量 1至购买件数 */
    private Integer applyQty;

    /** 退款金额，不超过明细实付 */
    private BigDecimal refundAmount;

    /** 质量问题/错发漏发/不喜欢/物流破损 */
    private String reasonType;

    /** 问题描述，不少于10字 */
    private String description;

    /** 完好/缺页污损/非本店图书/待质检 */
    private String inspectResult;

    /** 读者寄回物流单号 */
    private String backExpressNo;

    /** 审核意见（文档补充字段） */
    private String auditNote;

    /** 审核时间（文档补充字段） */
    private LocalDateTime auditTime;

    /** 退款完成时间（文档补充字段） */
    private LocalDateTime refundTime;

    /** 待审核/待寄回/已寄回/已退款/已驳回/已关闭 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 0 否 1 是 */
    @TableLogic
    private Integer deleted;

    /** 创建人 */
    private Long createBy;

    /** 最后修改人 */
    private Long updateBy;

    /** 乐观锁版本号 */
    private Integer version;
}
