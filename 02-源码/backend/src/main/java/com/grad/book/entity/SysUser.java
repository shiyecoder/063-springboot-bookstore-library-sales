package com.grad.book.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户
 */
@Data
@TableName("sys_user")
public class SysUser {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    private String username;

    /** BCrypt 密码 */
    private String password;

    /** 姓名 */
    private String realName;

    /** 读者编号（读者角色） */
    private String studentNo;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 头像相对路径 */
    private String avatar;

    /** 所属学院/部门 sys_org.id */
    private Long deptId;

    /** 主角色 sys_role.id（菜单与数据范围据此取） */
    private Long roleId;

    /** 会员等级 */
    private String className;

    /** 岗位 / 职务 */
    private String title;

    /** 信用分（恶意退货扣减） */
    private BigDecimal creditScore;

    /** 连续登录失败次数 */
    private Integer failCount;

    /** 锁定截止时间，为空表示未锁定 */
    private LocalDateTime lockTime;

    /** 最近登录时间 */
    private LocalDateTime lastLoginTime;

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
