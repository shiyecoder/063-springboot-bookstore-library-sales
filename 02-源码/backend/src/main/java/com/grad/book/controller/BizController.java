package com.grad.book.controller;

import com.grad.book.common.BizException;
import com.grad.book.common.R;
import com.grad.book.common.UserContext;
import com.grad.book.service.BizService;
import com.grad.book.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 书店业务接口。
 *
 * <p>权限在这里做第一道拦截（角色白名单），数据范围与状态机在 Service 里兜底：
 * 前端隐藏按钮只是体验，接口被直接调用时同样会被拒绝。</p>
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BizController {

    private final ShopService shop;
    private final BizService biz;

    /** 角色白名单：未在名单内的角色调用写接口直接 403 */
    private void allow(String... roles) {
        String r = UserContext.roleCode();
        for (String x : roles) {
            if (x.equals(r)) {
                return;
            }
        }
        throw new BizException(403, "当前角色无权执行该操作");
    }

    // ==================================================== 看板与选项

    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboard() {
        return R.ok(shop.dashboard());
    }

    @GetMapping("/options")
    public R<Map<String, Object>> options() {
        return R.ok(shop.options());
    }

    // ==================================================== 图书

    @GetMapping("/book/page")
    public R<Map<String, Object>> bookPage(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) Long categoryId,
                                           @RequestParam(required = false) String publisher,
                                           @RequestParam(required = false) String sort,
                                           @RequestParam(required = false) Boolean inStock) {
        return R.ok(shop.bookPage(page, size, keyword, status, categoryId, sort, inStock, publisher));
    }

    @GetMapping("/book/{id}")
    public R<Map<String, Object>> bookDetail(@PathVariable Long id) {
        return R.ok(shop.bookDetail(id));
    }

    @PostMapping("/book")
    public R<Long> bookAdd(@RequestBody Map<String, Object> d) {
        allow("ADMIN", "OPS");
        d.remove("id");
        return R.ok(shop.bookSave(d));
    }

    @PutMapping("/book/{id}")
    public R<Long> bookUpdate(@PathVariable Long id, @RequestBody Map<String, Object> d) {
        allow("ADMIN", "OPS");
        d.put("id", id);
        return R.ok(shop.bookSave(d));
    }

    @DeleteMapping("/book/{id}")
    public R<Void> bookDelete(@PathVariable Long id) {
        allow("ADMIN");
        shop.bookDelete(id);
        return R.ok();
    }

    /** 上架/下架：校验库存、分类状态与售价区间 */
    @PostMapping("/book/{id}/on-shelf")
    public R<Map<String, Object>> onShelf(@PathVariable Long id, @RequestBody Map<String, Object> d) {
        allow("ADMIN", "OPS");
        String target = d.get("targetStatus") == null ? "" : String.valueOf(d.get("targetStatus"));
        String reason = d.get("reason") == null ? null : String.valueOf(d.get("reason"));
        return R.ok(shop.bookShelf(id, target, reason));
    }

    // ==================================================== 分类

    @GetMapping("/book-category/tree")
    public R<Object> categoryTree() {
        return R.ok(shop.categoryTree());
    }

    @GetMapping("/book-category/page")
    public R<Map<String, Object>> categoryPage(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(required = false) String keyword) {
        return R.ok(shop.categoryPage(page, size, keyword));
    }

    @PostMapping("/book-category")
    public R<Long> categoryAdd(@RequestBody Map<String, Object> d) {
        allow("ADMIN", "OPS");
        d.remove("id");
        return R.ok(shop.categorySave(d));
    }

    @PutMapping("/book-category/{id}")
    public R<Long> categoryUpdate(@PathVariable Long id, @RequestBody Map<String, Object> d) {
        allow("ADMIN", "OPS");
        d.put("id", id);
        return R.ok(shop.categorySave(d));
    }

    @DeleteMapping("/book-category/{id}")
    public R<Void> categoryDelete(@PathVariable Long id) {
        allow("ADMIN");
        shop.categoryDelete(id);
        return R.ok();
    }

    // ==================================================== 入库、盘点、流水

    @GetMapping("/stock-in/page")
    public R<Map<String, Object>> stockInPage(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String status) {
        return R.ok(shop.stockInPage(page, size, keyword, status));
    }

    @PostMapping("/stock-in")
    public R<Long> stockInAdd(@RequestBody Map<String, Object> d) {
        allow("ADMIN", "KEEPER", "BUYER");
        return R.ok(shop.stockInSave(d));
    }

    /** 质检：合格写库存与流水，不合格整批退回且库存不增加 */
    @PostMapping("/stock-in/{id}/inspect")
    public R<Map<String, Object>> stockInInspect(@PathVariable Long id, @RequestBody Map<String, Object> d) {
        allow("ADMIN", "KEEPER");
        return R.ok(shop.stockInInspect(id,
                d.get("qualityResult") == null ? "合格" : String.valueOf(d.get("qualityResult")),
                d.get("remark") == null ? null : String.valueOf(d.get("remark"))));
    }

    @GetMapping("/stock/flow/page")
    public R<Map<String, Object>> stockFlowPage(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String changeType,
                                                @RequestParam(required = false) Long bookId) {
        return R.ok(shop.stockFlowPage(page, size, keyword, changeType, bookId));
    }

    /** 盘点：盘盈盘亏必须填原因，差异走流水 */
    @PostMapping("/stock/check")
    public R<Map<String, Object>> stockCheck(@RequestBody Map<String, Object> d) {
        allow("ADMIN", "KEEPER");
        Long bookId = d.get("bookId") == null ? null : Long.valueOf(String.valueOf(d.get("bookId")));
        int actual = d.get("actualQty") == null ? 0 : Integer.parseInt(String.valueOf(d.get("actualQty")));
        String reason = d.get("reason") == null ? null : String.valueOf(d.get("reason"));
        if (bookId == null) {
            throw new BizException("请选择要盘点的图书");
        }
        return R.ok(shop.stockCheck(bookId, actual, reason));
    }

    // ==================================================== 订单

    @GetMapping("/order/page")
    public R<Map<String, Object>> orderPage(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) Long bookId) {
        return R.ok(shop.orderPage(page, size, keyword, status, bookId));
    }

    @GetMapping("/order/{id}")
    public R<Map<String, Object>> orderDetail(@PathVariable Long id) {
        return R.ok(shop.orderDetail(id));
    }

    @PostMapping("/order")
    public R<Map<String, Object>> orderCreate(@RequestBody Map<String, Object> d) {
        return R.ok(shop.orderCreate(d));
    }

    @PostMapping("/order/{id}/pay")
    public R<Void> orderPay(@PathVariable Long id) {
        shop.orderPay(id);
        return R.ok();
    }

    /** 店员发货：登记物流单号与首个节点，缺货可登记原因 */
    @PostMapping("/order/{id}/deliver")
    public R<Void> orderDeliver(@PathVariable Long id, @RequestBody Map<String, Object> d) {
        allow("ADMIN", "CLERK");
        shop.orderDeliver(id,
                d.get("expressNo") == null ? null : String.valueOf(d.get("expressNo")),
                d.get("company") == null ? null : String.valueOf(d.get("company")),
                d.get("shortageReason") == null ? null : String.valueOf(d.get("shortageReason")));
        return R.ok();
    }

    @PostMapping("/order/{id}/confirm")
    public R<Void> orderConfirm(@PathVariable Long id) {
        shop.orderConfirm(id);
        return R.ok();
    }

    @PostMapping("/order/{id}/cancel")
    public R<Void> orderCancel(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> d) {
        String reason = d == null || d.get("reason") == null ? null : String.valueOf(d.get("reason"));
        shop.orderCancel(id, reason);
        return R.ok();
    }

    // ==================================================== 书评

    @GetMapping("/review/page")
    public R<Map<String, Object>> reviewPage(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) Long bookId) {
        return R.ok(shop.reviewPage(page, size, keyword, status, bookId));
    }

    @PostMapping("/review")
    public R<Map<String, Object>> reviewCreate(@RequestBody Map<String, Object> d) {
        return R.ok(shop.reviewCreate(d));
    }

    @PostMapping("/review/{id}/audit")
    public R<Void> reviewAudit(@PathVariable Long id, @RequestBody Map<String, Object> d) {
        allow("ADMIN", "OPS", "CLERK");
        shop.reviewAudit(id, String.valueOf(d.get("targetStatus")),
                d.get("reason") == null ? null : String.valueOf(d.get("reason")));
        return R.ok();
    }

    @PostMapping("/review/{id}/reply")
    public R<Void> reviewReply(@PathVariable Long id, @RequestBody Map<String, Object> d) {
        allow("ADMIN", "OPS", "CLERK");
        shop.reviewReply(id, d.get("content") == null ? null : String.valueOf(d.get("content")));
        return R.ok();
    }

    @PostMapping("/review/{id}/useful")
    public R<Map<String, Object>> reviewUseful(@PathVariable Long id) {
        return R.ok(shop.reviewUseful(id));
    }

    // ==================================================== 退换

    @GetMapping("/return/page")
    public R<Map<String, Object>> returnPage(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String status) {
        return R.ok(shop.returnPage(page, size, keyword, status));
    }

    @PostMapping("/return")
    public R<Map<String, Object>> returnApply(@RequestBody Map<String, Object> d) {
        return R.ok(shop.returnApply(d));
    }

    @PostMapping("/return/{id}/audit")
    public R<Void> returnAudit(@PathVariable Long id, @RequestBody Map<String, Object> d) {
        allow("ADMIN", "CLERK");
        BigDecimal refund = d.get("refundAmount") == null ? null : new BigDecimal(String.valueOf(d.get("refundAmount")));
        shop.returnAudit(id, String.valueOf(d.get("targetStatus")),
                d.get("note") == null ? null : String.valueOf(d.get("note")), refund);
        return R.ok();
    }

    @PostMapping("/return/{id}/receive")
    public R<Void> returnReceive(@PathVariable Long id, @RequestBody Map<String, Object> d) {
        allow("ADMIN", "KEEPER");
        BigDecimal refund = d.get("refundAmount") == null ? null : new BigDecimal(String.valueOf(d.get("refundAmount")));
        shop.returnReceive(id,
                d.get("inspectResult") == null ? "完好" : String.valueOf(d.get("inspectResult")),
                refund, d.get("remark") == null ? null : String.valueOf(d.get("remark")));
        return R.ok();
    }

    // ==================================================== 补货（创新点）

    @GetMapping("/replenish/page")
    public R<Map<String, Object>> replenishPage(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) String level,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String adopt) {
        return R.ok(shop.replenishPage(page, size, level, keyword, adopt));
    }

    @GetMapping("/replenish/stat")
    public R<Map<String, Object>> replenishStat() {
        return R.ok(shop.replenishStat());
    }

    @GetMapping("/replenish/rule")
    public R<Object> ruleList() {
        return R.ok(shop.ruleList());
    }

    @PostMapping("/replenish/rule")
    public R<Map<String, Object>> ruleSave(@RequestBody Map<String, Object> d) {
        allow("ADMIN", "OPS", "BUYER");
        return R.ok(shop.ruleSave(d));
    }

    @PostMapping("/replenish/generate")
    public R<Map<String, Object>> replenishGenerate() {
        allow("ADMIN", "OPS", "BUYER");
        return R.ok(shop.replenishGenerate());
    }

    @GetMapping("/replenish/{id}")
    public R<Map<String, Object>> replenishDetail(@PathVariable Long id) {
        return R.ok(shop.replenishDetail(id));
    }

    @PostMapping("/replenish/{id}/adopt")
    public R<Void> replenishAdopt(@PathVariable Long id, @RequestBody Map<String, Object> d) {
        allow("ADMIN", "OPS", "BUYER");
        Integer qty = d.get("qty") == null ? null : Integer.parseInt(String.valueOf(d.get("qty")));
        shop.replenishAdopt(id, String.valueOf(d.get("adoptStatus")), qty,
                d.get("note") == null ? null : String.valueOf(d.get("note")));
        return R.ok();
    }

    // ==================================================== 报表

    @GetMapping("/report/sales")
    public R<Map<String, Object>> report(@RequestParam(defaultValue = "category") String dim,
                                         @RequestParam(defaultValue = "30") int days) {
        return R.ok(shop.report(dim, days <= 0 ? 30 : days));
    }

    // ==================================================== 门户与我的

    @GetMapping("/portal/home")
    public R<Map<String, Object>> portalHome() {
        return R.ok(shop.portalHome());
    }

    @GetMapping("/portal/books")
    public R<Map<String, Object>> portalBooks(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "12") int size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Long categoryId,
                                              @RequestParam(required = false) String sort) {
        return R.ok(shop.portalBooks(page, size, keyword, categoryId, sort));
    }

    @GetMapping("/portal/books/{id}")
    public R<Map<String, Object>> portalBook(@PathVariable Long id) {
        return R.ok(shop.bookDetail(id));
    }

    @GetMapping("/my/orders")
    public R<Map<String, Object>> myOrders(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String status) {
        return R.ok(shop.myOrders(page, size, status));
    }
}
