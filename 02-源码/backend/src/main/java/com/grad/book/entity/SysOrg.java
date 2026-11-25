package com.grad.book.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 组织
 */
@Data
@TableName("sys_org")
public class SysOrg {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 组织名称 如总店/仓储中心 */
    private String orgName;

    /** 组织编码 */
    private String orgCode;

    /** 上级组织 */
    private Long parentId;

    /** 类型 总部/门店/仓储/采购 */
    private String orgType;

    /** 负责人 */
    private String leader;

    /** 联系电话 */
    private String phone;

    /** 排序 */
    private Integer sortNo;

    /** 状态 1正常 0停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 0 否 1 是 */
    @TableLogic
    private Integer deleted;
}
