package com.grad.book.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 图书书评
 */
@Data
@TableName("book_review")
public class BookReview {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 书评编号 SP+yyMMdd+6位序号 */
    private String reviewNo;

    /** 图书 book.id */
    private Long bookId;

    /** 订单 book_order.id，须已签收 */
    private Long orderId;

    /** 读者 sys_user.id */
    private Long customerId;

    /** 评分 1至5星 */
    private Integer score;

    /** 书评标题 */
    private String title;

    /** 书评内容，不少于15字 */
    private String content;

    /** 晒图相对路径，最多9张，逗号分隔 */
    private String imageUrls;

    /** 有用数 */
    private Integer usefulCount;

    /** 店员回复，仅一条 */
    private String replyContent;

    /** 回复时间（文档补充字段） */
    private LocalDateTime replyTime;

    /** 待审核/已展示/已隐藏/已屏蔽 */
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
