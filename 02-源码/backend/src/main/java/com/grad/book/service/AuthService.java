package com.grad.book.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.grad.book.common.BizException;
import com.grad.book.common.JwtUtil;
import com.grad.book.common.UserContext;
import com.grad.book.dto.AuthDtos;
import com.grad.book.entity.SysMenu;
import com.grad.book.entity.SysMessage;
import com.grad.book.entity.SysRole;
import com.grad.book.entity.SysUser;
import com.grad.book.mapper.SysMenuMapper;
import com.grad.book.mapper.SysMessageMapper;
import com.grad.book.mapper.SysRoleMapper;
import com.grad.book.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证与个人中心。
 * 登录失败累计 5 次锁定（阈值取自配置），锁定期间即使密码正确也拒绝，避免暴力破解；
 * 任何返回体与日志都不带密码字段。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysMessageMapper messageMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JdbcTemplate jdbc;

    @Value("${app.login-fail-limit:5}")
    private int failLimit;
    @Value("${app.login-lock-minutes:15}")
    private int lockMinutes;

    /** 账号 → 用户；同时带上角色编码与数据范围 */
    private SysUser findUser(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username).last("limit 1"));
    }

    /** 账号 → 角色。sys_user.role_id 是唯一来源，未配置时按读者处理。 */
    private SysRole findRole(SysUser u) {
        if (u.getRoleId() == null) {
            return null;
        }
        return roleMapper.selectById(u.getRoleId());
    }

    public AuthDtos.LoginResp login(AuthDtos.LoginReq req) {
        SysUser user = findUser(req.username().trim());
        if (user == null) {
            throw new BizException("账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已停用，请联系管理员");
        }
        if (user.getLockTime() != null && user.getLockTime().isAfter(LocalDateTime.now())) {
            throw new BizException("账号已锁定，请 " + lockMinutes + " 分钟后再试");
        }
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            int fail = (user.getFailCount() == null ? 0 : user.getFailCount()) + 1;
            user.setFailCount(fail);
            if (fail >= failLimit) {
                user.setLockTime(LocalDateTime.now().plusMinutes(lockMinutes));
                user.setFailCount(0);
                userMapper.updateById(user);
                throw new BizException("密码连续错误 " + failLimit + " 次，账号已锁定 " + lockMinutes + " 分钟");
            }
            userMapper.updateById(user);
            throw new BizException("账号或密码错误，还可尝试 " + (failLimit - fail) + " 次");
        }
        // 登录成功清零失败计数
        user.setFailCount(0);
        user.setLockTime(null);
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        SysRole role = findRole(user);
        String roleCode = role == null ? "READER" : role.getRoleCode();
        String scope = role == null ? "SELF" : role.getDataScope();
        UserContext.CurrentUser cu = new UserContext.CurrentUser(user.getId(), user.getUsername(),
                user.getRealName(), roleCode, user.getDeptId(), scope);
        String token = jwtUtil.create(cu);
        return new AuthDtos.LoginResp(token, userBrief(user, role), menus(roleCode));
    }

    /**
     * 读者自助注册。
     * 注册入口只开放 READER 角色，其他角色必须由系统管理员在用户管理里创建，
     * 避免用注册接口提权。
     */
    public AuthDtos.LoginResp register(AuthDtos.RegisterReq req) {
        String uname = req.username().trim();
        if (uname.length() < 3 || uname.length() > 20) {
            throw new BizException("账号长度需为 3-20 位");
        }
        if (req.password() == null || req.password().length() < 6) {
            throw new BizException("密码至少 6 位");
        }
        if (findUser(uname) != null) {
            throw new BizException("账号已被占用，请更换");
        }
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, "READER").last("limit 1"));
        if (role == null) {
            throw new BizException("读者角色未初始化，请联系管理员");
        }
        SysUser u = new SysUser();
        u.setUsername(uname);
        u.setPassword(passwordEncoder.encode(req.password()));
        u.setRealName(req.realName().trim());
        u.setPhone(req.phone());
        u.setRoleId(role.getId());
        u.setAvatar("/uploads/seed/avatar/" + String.format("%02d", (Math.abs(uname.hashCode()) % 8) + 1) + ".jpg");
        u.setCreditScore(new java.math.BigDecimal("100"));
        u.setFailCount(0);
        u.setStatus(1);
        u.setCreateTime(LocalDateTime.now());
        u.setUpdateTime(LocalDateTime.now());
        userMapper.insert(u);

        UserContext.CurrentUser cu = new UserContext.CurrentUser(u.getId(), u.getUsername(),
                u.getRealName(), role.getRoleCode(), u.getDeptId(), role.getDataScope());
        String token = jwtUtil.create(cu);
        return new AuthDtos.LoginResp(token, userBrief(u, role), menus(role.getRoleCode()));
    }

    private Map<String, Object> userBrief(SysUser u, SysRole role) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("realName", u.getRealName());
        m.put("studentNo", u.getStudentNo());
        m.put("className", u.getClassName());
        m.put("phone", u.getPhone());
        m.put("email", u.getEmail());
        m.put("avatar", u.getAvatar());
        m.put("deptId", u.getDeptId());
        m.put("title", u.getTitle());
        m.put("creditScore", u.getCreditScore());
        m.put("roleCode", role == null ? "READER" : role.getRoleCode());
        m.put("roleName", role == null ? "读者" : role.getRoleName());
        m.put("dataScope", role == null ? "SELF" : role.getDataScope());
        if (u.getDeptId() != null) {
            List<Map<String, Object>> org = jdbc.queryForList(
                    "select org_name from sys_org where id = ?", u.getDeptId());
            m.put("orgName", org.isEmpty() ? "" : org.get(0).get("org_name"));
        }
        return m;
    }

    /** 当前登录信息：用户 + 菜单树（前端据此渲染侧栏） */
    public Map<String, Object> me() {
        Long uid = UserContext.userId();
        if (uid == null) {
            throw new BizException(401, "登录状态已失效，请重新登录");
        }
        SysUser u = userMapper.selectById(uid);
        if (u == null) {
            throw new BizException(401, "账号不存在");
        }
        SysRole role = findRole(u);
        String roleCode = role == null ? "READER" : role.getRoleCode();
        Map<String, Object> m = userBrief(u, role);
        m.put("menus", menus(roleCode));
        m.put("unread", unreadCount(uid));
        return m;
    }

    /** 按角色取可见菜单并组装为两级树 */
    public List<Map<String, Object>> menus(String roleCode) {
        String sql = """
                select m.id, m.parent_id, m.menu_name, m.path, m.icon, m.sort_no, m.perms
                from sys_menu m
                join sys_role_menu rm on rm.menu_id = m.id
                join sys_role r on r.id = rm.role_id
                where r.role_code = ? and m.visible = 1
                order by m.sort_no
                """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, roleCode);
        Map<Long, Map<String, Object>> idx = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", ((Number) r.get("id")).longValue());
            n.put("parentId", ((Number) r.get("parent_id")).longValue());
            n.put("name", r.get("menu_name"));
            n.put("path", r.get("path"));
            n.put("icon", r.get("icon"));
            n.put("perms", r.get("perms"));
            n.put("sortNo", r.get("sort_no"));
            n.put("children", new ArrayList<Map<String, Object>>());
            idx.put(((Number) r.get("id")).longValue(), n);
        }
        List<Map<String, Object>> tree = new ArrayList<>();
        for (Map<String, Object> n : idx.values()) {
            long pid = (long) n.get("parentId");
            if (pid == 0 || !idx.containsKey(pid)) {
                tree.add(n);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> kids = (List<Map<String, Object>>) idx.get(pid).get("children");
                kids.add(n);
            }
        }
        return tree;
    }

    public int unreadCount(Long uid) {
        Long c = jdbc.queryForObject(
                "select count(*) from sys_message where receiver_id = ? and read_flag = 0", Long.class, uid);
        return c == null ? 0 : c.intValue();
    }

    /** 修改资料；姓名与联系方式为空时保持原值 */
    public void updateProfile(AuthDtos.ProfileReq req) {
        SysUser u = userMapper.selectById(UserContext.userId());
        if (u == null) {
            throw new BizException("账号不存在");
        }
        if (req.realName() != null && !req.realName().isBlank()) {
            u.setRealName(req.realName().trim());
        }
        if (req.phone() != null && !req.phone().isBlank()) {
            u.setPhone(req.phone().trim());
        }
        if (req.email() != null && !req.email().isBlank()) {
            u.setEmail(req.email().trim());
        }
        if (req.avatar() != null && !req.avatar().isBlank()) {
            u.setAvatar(req.avatar());
        }
        userMapper.updateById(u);
    }

    public void changePassword(AuthDtos.PasswordReq req) {
        SysUser u = userMapper.selectById(UserContext.userId());
        if (u == null) {
            throw new BizException("账号不存在");
        }
        if (!passwordEncoder.matches(req.oldPassword(), u.getPassword())) {
            throw new BizException("原密码不正确");
        }
        if (req.newPassword() == null || req.newPassword().length() < 6) {
            throw new BizException("新密码至少 6 位");
        }
        u.setPassword(passwordEncoder.encode(req.newPassword()));
        userMapper.updateById(u);
    }

    /**
     * 站内消息分页（个人中心与消息中心共用）。
     * 口径与业务接口保持一致：返回 records 且字段驼峰，避免前端为消息页单独记一套命名。
     */
    public Map<String, Object> messagePage(String type, int page, int size) {
        Long uid = UserContext.userId();
        String where = " where receiver_id = ? ";
        List<Object> args = new ArrayList<>();
        args.add(uid);
        if (type != null && !type.isBlank() && !"全部".equals(type)) {
            where += " and msg_type = ? ";
            args.add(type);
        }
        Long total = jdbc.queryForObject("select count(*) from sys_message" + where, Long.class, args.toArray());
        args.add(size);
        args.add((Math.max(page, 1) - 1) * size);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select id, title, content, msg_type as msgType, biz_type as bizType, biz_id as bizId, "
                        + "link_url as linkUrl, read_flag as isRead, create_time as createTime from sys_message"
                        + where + " order by create_time desc limit ? offset ?", args.toArray());
        Map<String, Object> out = new HashMap<>();
        out.put("total", total == null ? 0 : total);
        out.put("page", page);
        out.put("size", size);
        out.put("records", rows);
        out.put("unread", unreadCount(uid));
        return out;
    }

    public void readMessage(Long id) {
        SysMessage m = messageMapper.selectById(id);
        if (m == null) {
            throw new BizException("消息不存在");
        }
        if (!m.getReceiverId().equals(UserContext.userId())) {
            throw new BizException(403, "只能操作本人的消息");
        }
        m.setReadFlag(1);
        messageMapper.updateById(m);
    }

    public void readAll() {
        jdbc.update("update sys_message set read_flag = 1 where receiver_id = ? and read_flag = 0",
                UserContext.userId());
    }

    /** 角色列表与菜单数量，供用户与角色页使用 */
    public List<Map<String, Object>> roleList() {
        return jdbc.queryForList("""
                select r.id, r.role_code as roleCode, r.role_name as roleName, r.data_scope as dataScope,
                       r.remark,
                       (select count(*) from sys_role_menu rm where rm.role_id = r.id) as menuCount,
                       (select count(*) from sys_user u where u.role_id = r.id) as userCount
                from sys_role r order by r.sort_no
                """);
    }

    /** 菜单管理列表（含隐藏项，仅管理员可见） */
    public List<SysMenu> allMenus() {
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSortNo));
    }
}
