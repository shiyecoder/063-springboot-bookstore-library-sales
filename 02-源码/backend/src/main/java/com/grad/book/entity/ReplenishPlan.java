package com.grad.book.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 补货建议单
 */
@Data
@TableName("replenish_plan")
public class ReplenishPlan {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 建议编号 BH+yyMMdd+4位序号 */
    private String planNo;

    /** 图书 book.id */
    private Long bookId;

    /** 近30天销量 */
    private Integer sales30d;

    /** 当前可售库存快照 */
    private Integer stockQty;

    /** 在途数量 */
    private Integer onwayQty;

    /** 周转天数，无销量记999 */
    private BigDecimal turnoverDays;

    /** 季节与开学系数 0.80至1.50 */
    private BigDecimal seasonFactor;

    /** 建议补货量（公式取整） */
    private Integer suggestQty;

    /** 畅销/平销/滞销/断销 */
    private String salesLevel;

    /** 未采纳/已采纳/已忽略 */
    private String adoptStatus;

    /** 采纳或忽略操作人 */
    private Long handlerId;

    /** 日均销量（文档补充字段） */
    private BigDecimal avgDaily;

    /** 近30天销量得分（文档补充字段） */
    private BigDecimal sSales;

    /** 周转天数得分（文档补充字段） */
    private BigDecimal sTurnover;

    /** 在途数量得分（文档补充字段） */
    private BigDecimal sOnway;

    /** 季节系数得分（文档补充字段） */
    private BigDecimal sSeason;

    /** 缺货次数得分（文档补充字段） */
    private BigDecimal sStockout;

    /** 补货优先级分（文档补充字段） */
    private BigDecimal totalScore;

    /** 规则原始等级（文档补充字段） */
    private String originLevel;

    /** 结果解释：主要影响因素与未满足条件 */
    private String levelDesc;

    /** 人工修正建议量（文档补充字段） */
    private Integer manualQty;

    /** 人工修正原因，不少于5字 */
    private String manualNote;

    /** 计算所用规则版本 */
    private String ruleVersion;

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
