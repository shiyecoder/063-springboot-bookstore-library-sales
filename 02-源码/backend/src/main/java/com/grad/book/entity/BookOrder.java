package com.grad.book.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 图书订单
 */
@Data
@TableName("book_order")
public class BookOrder {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号 BO+yyMMdd+6位序号 */
    private String orderNo;

    /** 读者 sys_user.id */
    private Long customerId;

    /** 图书 book.id（文档补充字段） */
    private Long bookId;

    /** 图书名称快照 */
    private String bookName;

    /** 成交单价快照 */
    private BigDecimal unitPrice;

    /** 购买件数 1至50 */
    private Integer itemCount;

    /** 售价乘数量合计 */
    private BigDecimal bookAmount;

    /** 满99免运费，否则6.00 */
    private BigDecimal freight;

    /** 优惠金额，不超过图书金额50% */
    private BigDecimal discountAmount;

    /** 图书金额加运费减优惠 */
    private BigDecimal payAmount;

    /** 收货人 */
    private String receiverName;

    /** 收货电话，前台脱敏 */
    private String receiverPhone;

    /** 收货地址文字描述 */
    private String addressDesc;

    /** 快递公司（文档补充字段） */
    private String expressCompany;

    /** 物流单号 */
    private String expressNo;

    /** 支付时间（文档补充字段） */
    private LocalDateTime payTime;

    /** 发货时间（文档补充字段） */
    private LocalDateTime deliverTime;

    /** 签收时间（文档补充字段） */
    private LocalDateTime signTime;

    /** 最近物流节点时间（文档补充字段） */
    private LocalDateTime logisticsTime;

    /** 取消原因 */
    private String cancelReason;

    /** 待支付/待发货/已发货/已签收/已完成/退换中/已取消 */
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
