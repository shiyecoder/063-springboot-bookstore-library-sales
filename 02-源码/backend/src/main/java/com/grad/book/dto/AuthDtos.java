package com.grad.book.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

/** 认证与个人中心相关入参。 */
public class AuthDtos {

    /** 登录入参 */
    public record LoginReq(@NotBlank(message = "请输入账号") String username,
                           @NotBlank(message = "请输入密码") String password) {
    }

    /** 注册入参：注册即指定角色，管理员可后续调整 */
    public record RegisterReq(@NotBlank(message = "请输入账号") String username,
                              @NotBlank(message = "请输入密码") String password,
                              @NotBlank(message = "请输入姓名") String realName,
                              String phone,
                              String roleCode) {
    }

    public record ProfileReq(String realName, String phone, String email, String avatar) {
    }

    public record PasswordReq(@NotBlank String oldPassword, @NotBlank String newPassword) {
    }

    /** 登录返回：令牌 + 菜单权限 + 用户摘要 */
    public record LoginResp(String token, Map<String, Object> user, List<Map<String, Object>> menus) {
    }
}
