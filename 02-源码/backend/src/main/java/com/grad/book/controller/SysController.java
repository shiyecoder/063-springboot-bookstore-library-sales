package com.grad.book.controller;

import com.grad.book.common.BizException;
import com.grad.book.common.R;
import com.grad.book.common.UserContext;
import com.grad.book.service.BizService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 系统管理接口：用户、角色、菜单、字典。
 *
 * <p>菜单按角色动态下发，因此「角色-菜单」勾选结果直接落 sys_role_menu；
 * 业务接口的写操作仍需在服务端复核角色，前端隐藏按钮不作为鉴权依据。</p>
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SysController {

    private final JdbcTemplate jdbc;
    private final BizService biz;

    private void requireAdmin() {
        String r = UserContext.roleCode();
        if (!"ADMIN".equals(r)) {
            throw new BizException(403, "仅系统管理员可操作");
        }
    }

    // ============================================================ 用户
    @GetMapping("/user/page")
    public R<Map<String, Object>> userPage(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Long roleId,
                                           @RequestParam(required = false) Integer status) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" from sys_user a left join sys_role r on r.id = a.role_id "
                + "left join sys_org o on o.id = a.dept_id where a.deleted = 0");
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (a.username like ? or a.real_name like ? or a.phone like ?)");
            String kw = "%" + keyword.trim() + "%";
            args.add(kw);
            args.add(kw);
            args.add(kw);
        }
        if (roleId != null) {
            where.append(" and a.role_id = ?");
            args.add(roleId);
        }
        if (status != null) {
            where.append(" and a.status = ?");
            args.add(status);
        }
        String select = "a.id, a.username, a.real_name, a.phone, a.email, a.avatar, a.role_id, a.dept_id, "
                + "a.title, r.data_scope, a.status, a.last_login_time, a.create_time, "
                + "r.role_name, r.role_code, o.org_name";
        return R.ok(biz.paged(select, where.toString(), args, "a.id", page, size));
    }

    @PostMapping("/user")
    public R<Long> userCreate(@RequestBody Map<String, Object> b) {
        requireAdmin();
        String username = str(b.get("username"));
        if (username == null) {
            throw new BizException("账号不能为空");
        }
        Long dup = jdbc.queryForObject("select count(*) from sys_user where username = ?", Long.class, username);
        if (dup != null && dup > 0) {
            throw new BizException("账号已存在");
        }
        String pwd = b.get("password") == null ? "123456" : str(b.get("password"));
        String hash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(pwd);
        Long id = biz.insert("insert into sys_user (username,password,real_name,phone,email,avatar,role_id,dept_id,"
                        + "title,status,fail_count,deleted,create_time,update_time) "
                        + "values (?,?,?,?,?,?,?,?,?,1,0,0,now(),now())",
                username, hash, str(b.get("realName")), str(b.get("phone")), str(b.get("email")),
                "/uploads/seed/avatar/01.jpg", b.get("roleId"), b.get("orgId"), str(b.get("title")));
        biz.update("insert into sys_user_role (user_id,role_id,deleted,create_time,update_time) values (?,?,0,now(),now())",
                id, b.get("roleId"));
        biz.log("用户管理", "新增", username, "新增账号 " + username);
        return R.ok(id);
    }

    @PutMapping("/user/{id}")
    public R<Boolean> userUpdate(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        requireAdmin();
        Map<String, Object> u = jdbc.queryForList("select * from sys_user where id = ? and deleted = 0", id)
                .stream().findFirst().orElseThrow(() -> new BizException("账号不存在"));
        biz.update("update sys_user set real_name = ?, phone = ?, email = ?, role_id = ?, dept_id = ?, title = ?, "
                        + "status = ?, update_time = now() where id = ?",
                b.get("realName") == null ? u.get("real_name") : str(b.get("realName")),
                b.get("phone") == null ? u.get("phone") : str(b.get("phone")),
                b.get("email") == null ? u.get("email") : str(b.get("email")),
                b.get("roleId") == null ? u.get("role_id") : b.get("roleId"),
                b.get("orgId") == null ? u.get("dept_id") : b.get("orgId"),
                b.get("title") == null ? u.get("title") : str(b.get("title")),
                b.get("status") == null ? u.get("status") : b.get("status"), id);
        biz.log("用户管理", "修改", String.valueOf(u.get("username")), "修改账号资料");
        return R.ok(true);
    }

    @PostMapping("/user/{id}/reset")
    public R<Boolean> userReset(@PathVariable Long id) {
        requireAdmin();
        String hash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("123456");
        biz.update("update sys_user set password = ?, fail_count = 0, lock_time = null, update_time = now() "
                + "where id = ?", hash, id);
        biz.log("用户管理", "重置密码", String.valueOf(id), "重置为初始密码 123456");
        return R.ok(true);
    }

    // ============================================================ 角色
    @GetMapping("/role/page")
    public R<Map<String, Object>> rolePage(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        String select = "a.*, (select count(*) from sys_user u where u.role_id = a.id and u.deleted = 0) as user_count";
        return R.ok(biz.paged(select, " from sys_role a where a.deleted = 0", new ArrayList<>(), "a.id", page, size));
    }

    /** 角色已勾选的菜单 id 列表 */
    @GetMapping("/role/{id}/menus")
    public R<List<Long>> roleMenus(@PathVariable Long id) {
        List<Long> out = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList(
                "select menu_id from sys_role_menu where role_id = ?", id)) {
            out.add(((Number) r.get("menu_id")).longValue());
        }
        return R.ok(out);
    }

    /** 新增角色 */
    @PostMapping("/role")
    public R<Long> roleCreate(@RequestBody Map<String, Object> b) {
        requireAdmin();
        String code = str(b.get("roleCode"));
        String name = str(b.get("roleName"));
        if (code == null || name == null) {
            throw new BizException("角色编码与名称不能为空");
        }
        Long dup = jdbc.queryForObject("select count(*) from sys_role where role_code = ? and deleted = 0",
                Long.class, code);
        if (dup != null && dup > 0) {
            throw new BizException("角色编码已存在");
        }
        Long id = biz.insert("insert into sys_role (role_code,role_name,data_scope,sort_no,status,remark,"
                        + "deleted,create_time,update_time) values (?,?,?,?,1,?,0,now(),now())",
                code, name, str(b.get("dataScope")) == null ? "SELF" : str(b.get("dataScope")),
                b.get("sortNo") == null ? 10 : Integer.parseInt(String.valueOf(b.get("sortNo"))),
                str(b.get("remark")));
        biz.log("角色管理", "新增", name, "新增角色 " + code);
        return R.ok(id);
    }

    /** 修改角色基础信息 */
    @PutMapping("/role/{id}")
    public R<Boolean> roleUpdate(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        requireAdmin();
        biz.update("update sys_role set role_name = ?, data_scope = ?, sort_no = ?, status = ?, remark = ?, "
                        + "update_time = now() where id = ?", str(b.get("roleName")), str(b.get("dataScope")),
                b.get("sortNo") == null ? 10 : Integer.parseInt(String.valueOf(b.get("sortNo"))),
                b.get("status") == null ? 1 : Integer.parseInt(String.valueOf(b.get("status"))),
                str(b.get("remark")), id);
        biz.log("角色管理", "修改", String.valueOf(id), "修改角色信息");
        return R.ok(true);
    }

    /** 保存角色菜单：先清后插，菜单管理项本身不可勾选（避免越权授予） */
    @PostMapping("/role/{id}/menus")
    public R<Boolean> roleSaveMenus(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        requireAdmin();
        Object ids = b.get("menuIds");
        biz.update("delete from sys_role_menu where role_id = ?", id);
        if (ids instanceof List<?> l) {
            for (Object o : l) {
                biz.update("insert into sys_role_menu (role_id,menu_id,deleted,create_time,update_time) "
                        + "values (?,?,0,now(),now())", id, o);
            }
        }
        biz.log("角色管理", "分配菜单", String.valueOf(id), "更新角色菜单权限");
        return R.ok(true);
    }

    // ============================================================ 菜单
    @GetMapping("/menu/tree")
    public R<List<Map<String, Object>>> menuTree() {
        List<Map<String, Object>> rows = biz.rows(jdbc.queryForList(
                "select id, parent_id, menu_name, path, perms as perm, icon, menu_type, sort_no, visible from sys_menu "
                        + "where deleted = 0 order by sort_no, id"));
        Map<Long, Map<String, Object>> idx = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> n = new LinkedHashMap<>(r);
            n.put("children", new ArrayList<Map<String, Object>>());
            idx.put(((Number) r.get("id")).longValue(), n);
        }
        List<Map<String, Object>> tree = new ArrayList<>();
        for (Map<String, Object> n : idx.values()) {
            long pid = ((Number) n.get("parentId")).longValue();
            if (pid == 0 || !idx.containsKey(pid)) {
                tree.add(n);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> kids = (List<Map<String, Object>>) idx.get(pid).get("children");
                kids.add(n);
            }
        }
        return R.ok(tree);
    }

    // ============================================================ 字典
    @GetMapping("/dict/page")
    public R<Map<String, Object>> dictPage(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String dictType) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" from sys_dict_data a where a.deleted = 0");
        if (dictType != null && !dictType.isBlank()) {
            where.append(" and a.dict_type = ?");
            args.add(dictType);
        }
        String select = "a.*, (select dict_name from sys_dict_type t where t.dict_type = a.dict_type) as dict_name";
        return R.ok(biz.paged(select, where.toString(), args, "a.dict_type, a.sort_no", page, size));
    }

    /** 全部字典（前端下拉统一从这里取，避免逐页硬编码枚举） */
    @GetMapping("/dict/all")
    public R<Map<String, Object>> dictAll() {
        Map<String, Object> m = new LinkedHashMap<>();
        for (Map<String, Object> r : jdbc.queryForList(
                "select dict_type, dict_label, dict_value from sys_dict_data where status = 1 and deleted = 0 "
                        + "order by dict_type, sort_no")) {
            String k = String.valueOf(r.get("dict_type"));
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) m.computeIfAbsent(k, x -> new ArrayList<>());
            list.add(r.get("dict_label"));
        }
        return R.ok(m);
    }

    private static String str(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o).trim();
        return s.isEmpty() || "null".equals(s) ? null : s;
    }
}
