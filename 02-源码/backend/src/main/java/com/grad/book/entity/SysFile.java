package com.grad.book.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 文件
 */
@Data
@TableName("sys_file")
public class SysFile {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文件名 */
    private String fileName;

    /** 相对路径 */
    private String filePath;

    /** 字节数 */
    private Long fileSize;

    /** 扩展名 */
    private String fileType;

    /** 业务类型 seed/avatar/review/voucher */
    private String bizType;

    /** 业务主键 */
    private Long bizId;

    /** 上传人 sys_user.id */
    private Long uploaderId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 0 否 1 是 */
    @TableLogic
    private Integer deleted;
}
