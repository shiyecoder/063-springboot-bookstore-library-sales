package com.grad.book.controller;

import com.grad.book.common.R;
import com.grad.book.common.UserContext;
import com.grad.book.dto.AuthDtos;
import com.grad.book.entity.SysMenu;
import com.grad.book.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 认证、个人中心与系统管理接口。
 * /api/auth/** 在 SecurityConfig 中放行（登录必须先能访问），其余需携带 JWT。
 * 所有写接口都由后端重新校验数据范围，不依赖前端按钮是否隐藏。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/login")
    public R<AuthDtos.LoginResp> login(@Valid @RequestBody AuthDtos.LoginReq req) {
        return R.ok(authService.login(req));
    }

    /** 读者自助注册：注册成功即登录，跳转前台书店 */
    @PostMapping("/auth/register")
    public R<AuthDtos.LoginResp> register(@Valid @RequestBody AuthDtos.RegisterReq req) {
        return R.ok(authService.register(req));
    }

    @PostMapping("/auth/logout")
    public R<Void> logout() {
        UserContext.clear();
        return R.ok();
    }

    /** 当前登录信息：用户 + 菜单树，前端一次拿全，避免二次请求白屏 */
    @GetMapping("/auth/me")
    public R<Map<String, Object>> me() {
        return R.ok(authService.me());
    }

    @GetMapping("/auth/menus")
    public R<List<Map<String, Object>>> menus() {
        return R.ok(authService.menus(UserContext.roleCode()));
    }

    @PutMapping("/auth/profile")
    public R<Void> profile(@RequestBody AuthDtos.ProfileReq req) {
        authService.updateProfile(req);
        return R.ok();
    }

    @PutMapping("/auth/password")
    public R<Void> password(@Valid @RequestBody AuthDtos.PasswordReq req) {
        authService.changePassword(req);
        return R.ok();
    }

    // ---------------- 站内消息 ----------------
    @GetMapping("/system/message/page")
    public R<Map<String, Object>> messagePage(@RequestParam(required = false) String type,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return R.ok(authService.messagePage(type, page, size));
    }

    @GetMapping("/system/message/unread")
    public R<Map<String, Object>> unread() {
        return R.ok(Map.of("count", authService.unreadCount(UserContext.userId())));
    }

    @PutMapping("/system/message/{id}/read")
    public R<Void> read(@PathVariable Long id) {
        authService.readMessage(id);
        return R.ok();
    }

    @PutMapping("/system/message/read-all")
    public R<Void> readAll() {
        authService.readAll();
        return R.ok();
    }

    // ---------------- 角色与菜单 ----------------
    @GetMapping("/system/role/list")
    public R<List<Map<String, Object>>> roles() {
        return R.ok(authService.roleList());
    }

    @GetMapping("/system/menu/list")
    public R<List<SysMenu>> menuList() {
        return R.ok(authService.allMenus());
    }
}
