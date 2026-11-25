package com.grad.book.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 图书分类
 */
@Data
@TableName("book_category")
public class BookCategory {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类编码，大写字母与数字 */
    private String categoryCode;

    /** 分类名称，同级不可重名 */
    private String categoryName;

    /** 父分类，0 表示一级分类 */
    private Long parentId;

    /** 排序号，越小越靠前 */
    private Integer sortNo;

    /** 0否 1是，首页推荐位 */
    private Integer isHome;

    /** 分类说明 */
    private String description;

    /** 关联图书数量 */
    private Integer bookCount;

    /** 1启用 0停用 */
    private Integer status;

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
