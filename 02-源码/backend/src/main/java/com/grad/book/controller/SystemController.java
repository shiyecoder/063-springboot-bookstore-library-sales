package com.grad.book.controller;

import com.grad.book.common.BizException;
import com.grad.book.common.PageResult;
import com.grad.book.common.R;
import com.grad.book.common.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 系统辅助接口：操作日志分页、文件上传。
 *
 * 文件上传统一落盘到项目根目录 uploads/，数据库只存相对路径 /uploads/xxx，
 * 前端永远不出现「手填图片 URL」的输入框（需求文档硬性要求）。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SystemController {

    private final JdbcTemplate jdbc;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    /** 操作日志分页：支持关键字（操作人/动作/内容/IP）、模块、日期区间服务端检索 */
    @GetMapping("/system/log/page")
    public R<PageResult<Map<String, Object>>> logPage(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String module,
                                                       @RequestParam(required = false) String start,
                                                       @RequestParam(required = false) String end) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);
        // 操作日志落在 sys_oper_log（BizService.log() 也写这张表），列表只读、不提供修改删除
        StringBuilder w = new StringBuilder(" WHERE deleted=0");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            w.append(" AND (oper_name LIKE ? OR oper_type LIKE ? OR module LIKE ? OR biz_no LIKE ? "
                    + "OR content LIKE ? OR ip LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            for (int i = 0; i < 6; i++) {
                args.add(like);
            }
        }
        if (module != null && !module.isBlank()) {
            w.append(" AND module = ?");
            args.add(module.trim());
        }
        // 日期区间按「含首含尾整天」处理，前端传 yyyy-MM-dd
        if (start != null && !start.isBlank()) {
            w.append(" AND oper_time >= ?");
            args.add(start.trim() + " 00:00:00");
        }
        if (end != null && !end.isBlank()) {
            w.append(" AND oper_time <= ?");
            args.add(end.trim() + " 23:59:59");
        }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM sys_oper_log" + w, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(s);
        pageArgs.add((long) (p - 1) * s);
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT id, module, oper_type, biz_no, content, oper_id, oper_name, oper_time, ip
                FROM sys_oper_log""" + w + " ORDER BY id DESC LIMIT ? OFFSET ?",
                (rs, i) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("module", rs.getString("module"));
                    m.put("operType", rs.getString("oper_type"));
                    m.put("bizNo", rs.getString("biz_no"));
                    m.put("content", rs.getString("content"));
                    m.put("operId", rs.getLong("oper_id"));
                    m.put("operName", rs.getString("oper_name"));
                    m.put("ip", rs.getString("ip"));
                    java.sql.Timestamp t = rs.getTimestamp("oper_time");
                    m.put("operTime", t == null ? null : t.toLocalDateTime()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    m.put("createTime", m.get("operTime"));
                    return m;
                }, pageArgs.toArray());
        return R.ok(PageResult.of(total == null ? 0 : total, p, s, rows));
    }

    /** 文件上传：返回相对路径，前端直接回填到表单字段 */
    @PostMapping("/file/upload")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BizException("上传文件为空");
        String name = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase() : "";
        if (!List.of(".jpg", ".jpeg", ".png", ".webp", ".gif", ".pdf").contains(ext)) {
            throw new BizException("仅支持 jpg/png/webp/gif/pdf 格式");
        }
        if (file.getSize() > 20L * 1024 * 1024) throw new BizException("单文件不能超过 20MB");

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Path dir = Paths.get(uploadDir, date).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(fileName).toFile());
        } catch (IOException e) {
            throw new BizException("文件保存失败：" + e.getMessage());
        }
        String rel = "/uploads/" + date + "/" + fileName;
        jdbc.update("""
                INSERT INTO sys_file(file_name, file_path, file_size, file_type, uploader_id,
                    create_time, update_time, deleted)
                VALUES(?,?,?,?,?,NOW(),NOW(),0)""",
                name, rel, file.getSize(), file.getContentType(), UserContext.userId());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("url", rel);
        m.put("path", rel);
        m.put("name", name);
        m.put("size", file.getSize());
        return R.ok(m);
    }
}
