package com.grad.book.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 补货规则配置
 */
@Data
@TableName("replenish_rule")
public class ReplenishRule {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则编码 */
    private String ruleCode;

    /** 规则名称 */
    private String ruleName;

    /** 版本号 v1/v2… */
    private String ruleVersion;

    /** 近30天销量权重 */
    private BigDecimal wSales;

    /** 库存周转天数权重 */
    private BigDecimal wTurnover;

    /** 在途数量权重 */
    private BigDecimal wOnway;

    /** 季节系数权重 */
    private BigDecimal wSeason;

    /** 近90天缺货权重 */
    private BigDecimal wStockout;

    /** 畅销线，默认80 */
    private BigDecimal hotLine;

    /** 平销线，默认55 */
    private BigDecimal normalLine;

    /** 畅销安全天数，默认15 */
    private Integer safetyHot;

    /** 平销安全天数，默认7 */
    private Integer safetyNormal;

    /** 1启用 0停用 */
    private Integer status;

    /** 生效时间 */
    private LocalDateTime effectiveTime;

    /** 变更说明 */
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
