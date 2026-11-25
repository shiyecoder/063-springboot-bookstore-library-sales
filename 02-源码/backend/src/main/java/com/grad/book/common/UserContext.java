package com.grad.book.common;

/**
 * 当前登录用户上下文。由 JWT 过滤器写入，Controller 只读。
 * 使用 ThreadLocal 保证同一请求线程内可取到角色与数据范围。
 */
public class UserContext {

    /**
     * dataScope 取值：ALL=全部数据、ORG=本学院/部门、SELF=本人相关数据。
     * 需求文档第2节：读者只看本人订单，店员/库管/采购按本部门，运营与管理员按授权范围。
     */
    public record CurrentUser(Long userId, String username, String realName, String roleCode,
                              Long orgId, String dataScope) {
    }

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static Long userId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.userId();
    }

    public static String realName() {
        CurrentUser u = HOLDER.get();
        return u == null ? "" : u.realName();
    }

    public static String roleCode() {
        CurrentUser u = HOLDER.get();
        return u == null ? "" : u.roleCode();
    }

    public static Long orgId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.orgId();
    }

    public static String dataScope() {
        CurrentUser u = HOLDER.get();
        return u == null ? "SELF" : u.dataScope();
    }

    /** 读者：检索下单、确认收货、写书评、申请退换 */
    public static boolean isReader() {
        return "READER".equals(roleCode());
    }

    /** 书店店员：拣货发货、登记物流、回复书评、受理退换 */
    public static boolean isClerk() {
        return "CLERK".equals(roleCode());
    }

    /** 库管员：入库质检、盘点、拣货出库、退回验收 */
    public static boolean isKeeper() {
        return "KEEPER".equals(roleCode());
    }

    /** 采购员：补货建议、采购单、在途与供应商 */
    public static boolean isBuyer() {
        return "BUYER".equals(roleCode());
    }

    /** 运营管理员：分类专题、书评审核、运费与促销规则 */
    public static boolean isOps() {
        return "OPS".equals(roleCode());
    }

    /** 系统管理员拥有全量数据范围 */
    public static boolean isAdmin() {
        return "ADMIN".equals(roleCode());
    }

    /** 后台业务角色（可见他人订单与库存数据） */
    public static boolean isStaff() {
        return isClerk() || isKeeper() || isBuyer() || isOps() || isAdmin();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
