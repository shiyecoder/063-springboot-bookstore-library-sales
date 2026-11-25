package com.grad.book.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 入库单
 */
@Data
@TableName("stock_in")
public class StockIn {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 入库单号 RK+yyMMdd+4位序号 */
    private String stockInNo;

    /** 图书 book.id */
    private Long bookId;

    /** 供货书商 */
    private String supplierName;

    /** 入库数量 1至5000 */
    private Integer inQty;

    /** 采购单价 */
    private BigDecimal purchasePrice;

    /** 批次号 图书ID后4位+yyMMdd */
    private String batchNo;

    /** 合格/破损/缺页/错版/待检 */
    private String qualityResult;

    /** 质检库管员 sys_user.id */
    private Long keeperId;

    /** 质检合格入库时间 */
    private LocalDateTime inTime;

    /** 待质检/已入库/已退回/已作废 */
    private String status;

    /** 质检说明与退回原因 */
    private String remark;

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
