package com.grad.book.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 图书
 */
@Data
@TableName("book")
public class Book {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 图书编号 BK+yyMMdd+4位序号 */
    private String bookNo;

    /** ISBN 13位数字，允许带连字符 */
    private String isbn;

    /** 书名 2至80字 */
    private String bookName;

    /** 副标题 */
    private String subTitle;

    /** 作者，多作者用顿号分隔 */
    private String author;

    /** 出版社 */
    private String publisher;

    /** 分类 book_category.id（二级分类） */
    private Long categoryId;

    /** 定价 */
    private BigDecimal listPrice;

    /** 售价，不得高于定价1.2倍 */
    private BigDecimal salePrice;

    /** 出版日期 */
    private String publishDate;

    /** 版次 */
    private String edition;

    /** 待上架/在售/缺货/已下架/停售 */
    private String status;

    /** 可售库存（文档补充字段） */
    private Integer stockQty;

    /** 下单锁定库存（文档补充字段） */
    private Integer lockedQty;

    /** 累计销量（已签收计入） */
    private Integer totalSales;

    /** 近90天缺货次数（创新点因子） */
    private Integer shortageCount;

    /** 封面图相对路径 */
    private String coverImage;

    /** 内容摘要/目录，详情页展示 */
    private String summary;

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
