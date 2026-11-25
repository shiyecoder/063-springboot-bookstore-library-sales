package com.grad.book.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜单
 */
@Data
@TableName("sys_menu")
public class SysMenu {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上级菜单 0为根 */
    private Long parentId;

    /** 菜单名称 */
    private String menuName;

    /** 类型 DIR目录/MENU菜单/BTN按钮 */
    private String menuType;

    /** 路由地址 */
    private String path;

    /** 组件路径 */
    private String component;

    /** 权限标识 */
    private String perms;

    /** 图标 */
    private String icon;

    /** 排序 */
    private Integer sortNo;

    /** 是否可见 1是 0否 */
    private Integer visible;

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
