package com.grad.book.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存变动流水
 */
@Data
@TableName("stock_flow")
public class StockFlow {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 流水号 KC+yyMMdd+6位序号 */
    private String flowNo;

    /** 图书 book.id */
    private Long bookId;

    /** 入库/销售出库/退货入库/盘盈/盘亏/报损 */
    private String changeType;

    /** 正数增加、负数减少，不得为0 */
    private Integer changeQty;

    /** 变动前库存 */
    private Integer beforeQty;

    /** 变动后库存=变动前+变动数量 */
    private Integer afterQty;

    /** 关联入库单号或订单号 */
    private String bizNo;

    /** 操作人 sys_user.id */
    private Long operatorId;

    /** 盘亏与报损必填原因 */
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
