package com.grad.book.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 字典数据
 */
@Data
@TableName("sys_dict_data")
public class SysDictData {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 字典类型编码 */
    private String dictType;

    /** 字典标签 */
    private String dictLabel;

    /** 字典值 */
    private String dictValue;

    /** 前端标签样式 t-green/t-blue… */
    private String tagClass;

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
