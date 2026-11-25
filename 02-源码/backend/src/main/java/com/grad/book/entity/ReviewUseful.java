package com.grad.book.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 书评有用记录
 */
@Data
@TableName("review_useful")
public class ReviewUseful {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 书评 book_review.id */
    private Long reviewId;

    /** 点有用的用户 sys_user.id */
    private Long userId;

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
