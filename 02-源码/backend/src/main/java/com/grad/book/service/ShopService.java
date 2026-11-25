package com.grad.book.service;

import com.grad.book.common.BizException;
import com.grad.book.common.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 书店业务查询与动作服务（图书、库存、订单、书评、退换、补货、报表）。
 *
 * <p>所有写操作都要过三道关：状态机（{@link BizService#requireNext}）、
 * 数据范围（{@link BizService#orderScope}）与库存校验（{@link BizService#changeStock}），
 * 前端按钮只是提示，真正的授权与校验都在后端。</p>
 */
@Service
@RequiredArgsConstructor
public class ShopService {

    private final JdbcTemplate jdbc;
    private final BizService biz;

    // ==================================================== 看板

    /** 首页 8 项 KPI + 6 组图表（需求文档第 9 节），整页按 1440×900 一屏设计 */
    public Map<String, Object> dashboard() {
        Map<String, Object> m = new LinkedHashMap<>();
        String today = LocalDate.now().toString();
        m.put("todayOrders", biz.one("select count(*) from book_order where date(create_time)=? and deleted=0", today));
        m.put("waitDeliver", biz.one("select count(*) from book_order where status='待发货' and deleted=0"));
        m.put("onSaleBooks", biz.one("select count(*) from book where status='在售' and deleted=0"));
        m.put("todaySales", biz.dec("select coalesce(sum(pay_amount),0) from book_order where date(create_time)=? "
                + "and status not in ('待支付','已取消') and deleted=0", today).setScale(2, RoundingMode.HALF_UP));
        m.put("shortageBooks", biz.one("select count(*) from book where status='缺货' and deleted=0"));
        m.put("avgOrderAmount", biz.dec("select coalesce(avg(pay_amount),0) from book_order "
                + "where status not in ('待支付','已取消') and deleted=0").setScale(2, RoundingMode.HALF_UP));
        long allOrders = biz.one("select count(*) from book_order where status<>'待支付' and deleted=0");
        long returns = biz.one("select count(*) from book_return where deleted=0");
        m.put("returnRate", allOrders == 0 ? 0.0 : Math.round(returns * 1000.0 / allOrders) / 10.0);
        m.put("needReplenish", biz.one("select count(*) from replenish_plan where suggest_qty>0 "
                + "and adopt_status='未采纳' and deleted=0"));

        // 近30天销售额与订单量：先造日期序列再左连接，缺数据的日期补 0，折线才连续
        m.put("trend", biz.list("select date_format(d.dt,'%m-%d') label, coalesce(o.cnt,0) orders, "
                + "coalesce(o.amt,0) amount from (select date(now()) - interval n day dt from "
                + "(select 0 n union select 1 union select 2 union select 3 union select 4 union select 5 "
                + "union select 6 union select 7 union select 8 union select 9 union select 10 union select 11 "
                + "union select 12 union select 13 union select 14 union select 15 union select 16 union select 17 "
                + "union select 18 union select 19 union select 20 union select 21 union select 22 union select 23 "
                + "union select 24 union select 25 union select 26 union select 27 union select 28 union select 29) x) d "
                + "left join (select date(create_time) dt, count(*) cnt, sum(pay_amount) amt from book_order "
                + "where create_time >= date(now()) - interval 29 day and status not in ('待支付','已取消') "
                + "and deleted=0 group by date(create_time)) o on o.dt=d.dt order by d.dt"));
        m.put("categoryShare", biz.list("select pc.category_name name, round(sum(o.pay_amount),2) value "
                + "from book_order o join book b on b.id=o.book_id join book_category c on c.id=b.category_id "
                + "join book_category pc on pc.id=c.parent_id where o.status not in ('待支付','已取消') "
                + "and o.deleted=0 group by pc.id order by value desc"));
        m.put("publisherTop", biz.list("select b.publisher name, sum(o.item_count) value from book_order o "
                + "join book b on b.id=o.book_id where o.status not in ('待支付','已取消') and o.deleted=0 "
                + "group by b.publisher order by value desc limit 10"));
        m.put("turnoverDist", biz.list("select case when turnover_days<15 then '<15天' "
                + "when turnover_days<30 then '15-30天' when turnover_days<60 then '30-60天' "
                + "when turnover_days<120 then '60-120天' when turnover_days<999 then '120天+' "
                + "else '无销量' end name, count(*) value from replenish_plan where deleted=0 "
                + "group by name order by value desc"));
        List<Map<String, Object>> funnel = new ArrayList<>();
        funnel.add(kv("提交订单", biz.one("select count(*) from book_order where deleted=0")));
        funnel.add(kv("已支付", biz.one("select count(*) from book_order where status in "
                + "('待发货','已发货','已签收','已完成','退换中') and deleted=0")));
        funnel.add(kv("已发货", biz.one("select count(*) from book_order where status in "
                + "('已发货','已签收','已完成','退换中') and deleted=0")));
        funnel.add(kv("已签收", biz.one("select count(*) from book_order where status in "
                + "('已签收','已完成','退换中') and deleted=0")));
        funnel.add(kv("已完成", biz.one("select count(*) from book_order where status='已完成' and deleted=0")));
        m.put("funnel", funnel);
        m.put("returnReason", biz.list("select reason_type name, count(*) value from book_return "
                + "where deleted=0 group by reason_type order by value desc"));
        return m;
    }

    private Map<String, Object> kv(String k, long v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", k);
        m.put("value", v);
        return m;
    }

    // ==================================================== 图书与分类

    public Map<String, Object> bookPage(int page, int size, String keyword, String status, Long categoryId,
                                        String sort, Boolean inStock, String publisher) {
        List<Object> args = new ArrayList<>();
        StringBuilder w = new StringBuilder("from book b left join book_category c on c.id=b.category_id "
                + "left join book_category pc on pc.id=c.parent_id where b.deleted=0");
        if (keyword != null && !keyword.isBlank()) {
            w.append(" and (b.book_name like ? or b.book_no like ? or b.author like ? or b.isbn like ?)");
            String k = "%" + keyword.trim() + "%";
            args.add(k);
            args.add(k);
            args.add(k);
            args.add(k);
        }
        if (status != null && !status.isBlank()) {
            w.append(" and b.status=?");
            args.add(status);
        }
        if (categoryId != null) {
            w.append(" and (b.category_id=? or c.parent_id=?)");
            args.add(categoryId);
            args.add(categoryId);
        }
        if (publisher != null && !publisher.isBlank()) {
            w.append(" and b.publisher=?");
            args.add(publisher);
        }
        if (Boolean.TRUE.equals(inStock)) {
            w.append(" and b.stock_qty>0");
        }
        String order = switch (sort == null ? "" : sort) {
            case "sales" -> "b.total_sales desc, b.id desc";
            case "new" -> "b.publish_date desc, b.id desc";
            case "priceAsc" -> "b.sale_price asc, b.id desc";
            case "priceDesc" -> "b.sale_price desc, b.id desc";
            default -> "b.create_time desc, b.id desc";
        };
        return biz.page("b.*, c.category_name, pc.category_name parentCategory", w.toString(), args, order, page, size);
    }

    public Map<String, Object> bookDetail(Long id) {
        Map<String, Object> b = biz.find("select b.*, c.category_name, pc.category_name parentCategory "
                + "from book b left join book_category c on c.id=b.category_id "
                + "left join book_category pc on pc.id=c.parent_id where b.id=?", id);
        if (b == null) {
            throw new BizException("图书不存在或已删除");
        }
        b.put("reviews", biz.list("select r.*, u.real_name customerName, u.avatar from book_review r "
                + "join sys_user u on u.id=r.customer_id where r.book_id=? and r.status='已展示' and r.deleted=0 "
                + "order by r.create_time desc limit 5", id));
        b.put("reviewStat", biz.find("select count(*) count, coalesce(round(avg(score),1),0) avgScore "
                + "from book_review where book_id=? and status='已展示' and deleted=0", id));
        b.put("sameCategory", biz.list("select id, book_name, author, sale_price, cover_image from book "
                + "where category_id=(select category_id from book where id=?) and id<>? and deleted=0 "
                + "and status<>'停售' limit 4", id, id));
        b.put("stockFlows", biz.list("select flow_no, change_type, change_qty, after_qty, create_time "
                + "from stock_flow where book_id=? order by id desc limit 5", id));
        return b;
    }

    /** 新增/修改图书：ISBN 唯一、售价不超定价 1.2 倍（文档 5.1） */
    @Transactional
    public Long bookSave(Map<String, Object> d) {
        Object idObj = d.get("id");
        String isbn = str(d.get("isbn")).trim();
        String name = str(d.get("bookName")).trim();
        if (isbn.isEmpty() || name.length() < 2) {
            throw new BizException("ISBN 与书名（至少 2 字）为必填项");
        }
        long dup = biz.one("select count(*) from book where isbn=? and deleted=0 and id<>?",
                isbn, idObj == null ? -1 : num(d.get("id")));
        if (dup > 0) {
            throw new BizException("ISBN 已存在：" + isbn);
        }
        BigDecimal list = BizService.bd(d.get("listPrice"));
        BigDecimal sale = BizService.bd(d.get("salePrice"));
        if (sale.compareTo(list.multiply(new BigDecimal("1.2"))) > 0) {
            throw new BizException("售价不得高于定价的 1.2 倍");
        }
        if (idObj == null) {
            String no = biz.no("BK", "book", "book_no", 4);
            Long id = biz.insert("insert into book (book_no, isbn, book_name, sub_title, author, publisher, "
                            + "category_id, list_price, sale_price, publish_date, edition, status, stock_qty, "
                            + "locked_qty, total_sales, shortage_count, cover_image, summary, create_by, create_time, "
                            + "update_by, update_time, deleted, version) "
                            + "values (?,?,?,?,?,?,?,?,?,?,?,'待上架',0,0,0,0,?,?,?,?,?,?,0,0)",
                    no, isbn, name, d.get("subTitle"), d.get("author"), d.get("publisher"),
                    num(d.get("categoryId")), list, sale, d.get("publishDate"),
                    d.get("edition") == null ? "第1版" : d.get("edition"),
                    d.get("coverImage"), d.get("summary"), UserContext.userId(), LocalDateTime.now(),
                    UserContext.userId(), LocalDateTime.now());
            biz.log("图书管理", "新增", no, "新增图书《" + name + "》，定价 " + list + "，售价 " + sale);
            return id;
        }
        Long id = num(d.get("id"));
        biz.update("update book set isbn=?, book_name=?, sub_title=?, author=?, publisher=?, category_id=?, "
                        + "list_price=?, sale_price=?, publish_date=?, edition=?, cover_image=?, summary=?, "
                        + "update_by=?, update_time=?, version=version+1 where id=?",
                isbn, name, d.get("subTitle"), d.get("author"), d.get("publisher"), num(d.get("categoryId")),
                list, sale, d.get("publishDate"), d.get("edition"), d.get("coverImage"), d.get("summary"),
                UserContext.userId(), LocalDateTime.now(), id);
        biz.log("图书管理", "修改", "", "修改图书《" + name + "》");
        return id;
    }

    /** 上架/下架：库存、分类状态与售价区间三重校验（文档 5.1、7.2） */
    @Transactional
    public Map<String, Object> bookShelf(Long id, String target, String reason) {
        Map<String, Object> b = biz.find("select b.*, c.status catStatus from book b "
                + "left join book_category c on c.id=b.category_id where b.id=?", id);
        if (b == null) {
            throw new BizException("图书不存在");
        }
        String cur = str(b.get("status"));
        if (cur.equals(target)) {
            throw new BizException("图书已是「" + target + "」状态，无需重复操作");
        }
        if (!List.of("在售", "已下架", "停售", "待上架").contains(target)) {
            throw new BizException("目标状态不合法");
        }
        if ("在售".equals(target)) {
            int stock = ((Number) b.get("stockQty")).intValue();
            if (stock <= 0) {
                throw new BizException("可售库存为 0，无法上架，请先入库");
            }
            if (b.get("catStatus") != null && ((Number) b.get("catStatus")).intValue() == 0) {
                throw new BizException("所属分类已停用，无法上架");
            }
            BigDecimal list = (BigDecimal) b.get("listPrice");
            BigDecimal sale = (BigDecimal) b.get("salePrice");
            if (sale.compareTo(list.multiply(new BigDecimal("1.2"))) > 0) {
                throw new BizException("售价高于定价 1.2 倍，不允许上架");
            }
            if (sale.compareTo(list.multiply(new BigDecimal("0.7"))) < 0) {
                throw new BizException("售价低于定价 70%，需运营管理员审批后才可生效");
            }
        }
        if (("已下架".equals(target) || "停售".equals(target)) && str(reason).length() < 5) {
            throw new BizException("下架需填写不少于 5 字的原因");
        }
        biz.update("update book set status=?, update_by=?, update_time=?, version=version+1 where id=?",
                target, UserContext.userId(), LocalDateTime.now(), id);
        biz.log("图书管理", "审核", str(b.get("bookNo")), "《" + b.get("bookName") + "》状态 "
                + cur + " → " + target + (str(reason).isEmpty() ? "" : "，原因：" + reason));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("status", target);
        return m;
    }

    @Transactional
    public void bookDelete(Long id) {
        long busy = biz.one("select count(*) from book_order where book_id=? and status in "
                + "('待支付','待发货','已发货','退换中') and deleted=0", id);
        if (busy > 0) {
            throw new BizException("该图书存在 " + busy + " 笔未完成订单，不能删除");
        }
        biz.update("update book set deleted=1, update_time=? where id=?", LocalDateTime.now(), id);
        biz.log("图书管理", "删除", "", "逻辑删除图书 ID " + id);
    }

    public List<Map<String, Object>> categoryTree() {
        List<Map<String, Object>> all = biz.list("select * from book_category where deleted=0 order by sort_no, id");
        Map<Long, Map<String, Object>> idx = new LinkedHashMap<>();
        List<Map<String, Object>> top = new ArrayList<>();
        for (Map<String, Object> c : all) {
            c.put("children", new ArrayList<Map<String, Object>>());
            idx.put(((Number) c.get("id")).longValue(), c);
        }
        for (Map<String, Object> c : all) {
            long pid = ((Number) c.get("parentId")).longValue();
            if (pid == 0) {
                top.add(c);
            } else if (idx.containsKey(pid)) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> ch = (List<Map<String, Object>>) idx.get(pid).get("children");
                ch.add(c);
            }
        }
        return top;
    }

    public Map<String, Object> categoryPage(int page, int size, String keyword) {
        List<Object> args = new ArrayList<>();
        StringBuilder w = new StringBuilder("from book_category c left join book_category p on p.id=c.parent_id "
                + "where c.deleted=0");
        if (keyword != null && !keyword.isBlank()) {
            w.append(" and (c.category_name like ? or c.category_code like ?)");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        return biz.page("c.*, p.category_name parentName", w.toString(), args,
                "c.parent_id asc, c.sort_no asc", page, size);
    }

    @Transactional
    public Long categorySave(Map<String, Object> d) {
        Object idObj = d.get("id");
        long dup = biz.one("select count(*) from book_category where category_code=? and deleted=0 and id<>?",
                str(d.get("categoryCode")), idObj == null ? -1 : num(d.get("id")));
        if (dup > 0) {
            throw new BizException("分类编码已存在");
        }
        if (idObj == null) {
            Long id = biz.insert("insert into book_category (category_code, category_name, parent_id, sort_no, "
                            + "is_home, description, book_count, status, create_by, create_time, update_by, "
                            + "update_time, deleted, version) values (?,?,?,?,?,?,0,?,?,?,?,?,0,0)",
                    d.get("categoryCode"), d.get("categoryName"),
                    num(d.get("parentId")) == null ? 0 : num(d.get("parentId")),
                    num(d.get("sortNo")) == null ? 0 : num(d.get("sortNo")),
                    num(d.get("isHome")) == null ? 0 : num(d.get("isHome")), d.get("description"),
                    num(d.get("status")) == null ? 1 : num(d.get("status")),
                    UserContext.userId(), LocalDateTime.now(), UserContext.userId(), LocalDateTime.now());
            biz.log("分类维护", "新增", str(d.get("categoryCode")), "新增分类《" + d.get("categoryName") + "》");
            return id;
        }
        Long id = num(d.get("id"));
        biz.update("update book_category set category_code=?, category_name=?, parent_id=?, sort_no=?, is_home=?, "
                        + "description=?, status=?, update_by=?, update_time=?, version=version+1 where id=?",
                d.get("categoryCode"), d.get("categoryName"), num(d.get("parentId")), num(d.get("sortNo")),
                num(d.get("isHome")), d.get("description"), num(d.get("status")),
                UserContext.userId(), LocalDateTime.now(), id);
        return id;
    }

    @Transactional
    public void categoryDelete(Long id) {
        long used = biz.one("select count(*) from book where category_id=? and deleted=0", id);
        if (used > 0) {
            throw new BizException("该分类下有 " + used + " 本图书，不能删除");
        }
        long sub = biz.one("select count(*) from book_category where parent_id=? and deleted=0", id);
        if (sub > 0) {
            throw new BizException("该分类下有 " + sub + " 个子分类，不能删除");
        }
        biz.update("update book_category set deleted=1, update_time=? where id=?", LocalDateTime.now(), id);
    }

    // ==================================================== 入库、盘点、流水

    public Map<String, Object> stockInPage(int page, int size, String keyword, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder w = new StringBuilder("from stock_in s left join book b on b.id=s.book_id "
                + "left join sys_user u on u.id=s.keeper_id where s.deleted=0");
        if (keyword != null && !keyword.isBlank()) {
            w.append(" and (s.stock_in_no like ? or b.book_name like ? or s.batch_no like ?)");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        if (status != null && !status.isBlank()) {
            w.append(" and s.status=?");
            args.add(status);
        }
        return biz.page("s.*, b.book_name, b.book_no, b.cover_image, b.stock_qty, u.real_name keeper_name",
                w.toString(), args, "s.create_time desc, s.id desc", page, size);
    }

    @Transactional
    public Long stockInSave(Map<String, Object> d) {
        Long bookId = num(d.get("bookId"));
        int qty = num(d.get("inQty")) == null ? 0 : num(d.get("inQty")).intValue();
        if (bookId == null || qty < 1 || qty > 5000) {
            throw new BizException("请选择图书，入库数量须在 1-5000 之间");
        }
        String no = biz.no("RK", "stock_in", "stock_in_no", 4);
        Long id = biz.insert("insert into stock_in (stock_in_no, book_id, supplier_name, in_qty, purchase_price, "
                        + "batch_no, quality_result, keeper_id, in_time, status, remark, create_by, create_time, "
                        + "update_by, update_time, deleted, version) values (?,?,?,?,?,?,'待检',?,null,'待质检',?,?,?,?,?,0,0)",
                no, bookId, d.get("supplierName"), qty, BizService.bd(d.get("purchasePrice")),
                bookId + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd")),
                UserContext.userId(), d.get("remark"), UserContext.userId(), LocalDateTime.now(),
                UserContext.userId(), LocalDateTime.now());
        biz.log("入库登记", "新增", no, "登记入库单：图书 " + bookId + "，数量 " + qty + "，状态待质检");
        return id;
    }

    /**
     * 质检：合格才写库存与流水；破损/缺页/错版整批退回，库存不增加、不生成流水（文档 5.2）。
     * 重复提交返回首次结果，避免同一批次被重复入库。
     */
    @Transactional
    public Map<String, Object> stockInInspect(Long id, String result, String remark) {
        Map<String, Object> si = biz.find("select * from stock_in where id=? and deleted=0", id);
        if (si == null) {
            throw new BizException("入库单不存在");
        }
        String status = str(si.get("status"));
        Map<String, Object> m = new LinkedHashMap<>();
        if (!"待质检".equals(status)) {
            m.put("id", id);
            m.put("status", status);
            m.put("firstResult", true);
            return m;
        }
        Long bookId = ((Number) si.get("bookId")).longValue();
        int qty = ((Number) si.get("inQty")).intValue();
        if ("合格".equals(result)) {
            biz.changeStock(bookId, qty, "入库", str(si.get("stockInNo")), "入库质检合格");
            biz.update("update stock_in set quality_result=?, status='已入库', in_time=?, keeper_id=?, remark=?, "
                            + "update_by=?, update_time=?, version=version+1 where id=?",
                    result, LocalDateTime.now(), UserContext.userId(), remark, UserContext.userId(),
                    LocalDateTime.now(), id);
            biz.log("入库质检", "审核", str(si.get("stockInNo")), "质检合格，入库 " + qty + " 册并生成流水");
            m.put("status", "已入库");
        } else {
            if (str(remark).length() < 5) {
                throw new BizException("不合格需填写不少于 5 字的退回原因");
            }
            biz.update("update stock_in set quality_result=?, status='已退回', keeper_id=?, remark=?, "
                            + "update_by=?, update_time=?, version=version+1 where id=?",
                    result, UserContext.userId(), remark, UserContext.userId(), LocalDateTime.now(), id);
            biz.log("入库质检", "审核", str(si.get("stockInNo")),
                    "质检不合格（" + result + "），整批退回，库存不增加、不生成流水");
            m.put("status", "已退回");
        }
        m.put("id", id);
        return m;
    }

    public Map<String, Object> stockFlowPage(int page, int size, String keyword, String changeType, Long bookId) {
        List<Object> args = new ArrayList<>();
        StringBuilder w = new StringBuilder("from stock_flow f left join book b on b.id=f.book_id "
                + "left join sys_user u on u.id=f.operator_id where f.deleted=0");
        if (keyword != null && !keyword.isBlank()) {
            w.append(" and (f.flow_no like ? or b.book_name like ? or f.biz_no like ?)");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        if (changeType != null && !changeType.isBlank()) {
            w.append(" and f.change_type=?");
            args.add(changeType);
        }
        if (bookId != null) {
            w.append(" and f.book_id=?");
            args.add(bookId);
        }
        return biz.page("f.*, b.book_name, b.cover_image, u.real_name operator_name", w.toString(), args,
                "f.create_time desc, f.id desc", page, size);
    }

    /** 盘点：差异必须填原因；盘盈盘亏都走流水并同步库存 */
    @Transactional
    public Map<String, Object> stockCheck(Long bookId, int actualQty, String reason) {
        if (str(reason).trim().length() < 5) {
            throw new BizException("盘盈盘亏必须填写不少于 5 字的原因");
        }
        Map<String, Object> b = biz.find("select stock_qty, book_name from book where id=?", bookId);
        if (b == null) {
            throw new BizException("图书不存在");
        }
        int before = ((Number) b.get("stockQty")).intValue();
        int diff = actualQty - before;
        if (diff == 0) {
            throw new BizException("实盘数与账面数一致，无需提交差异");
        }
        biz.changeStock(bookId, diff, diff > 0 ? "盘盈" : "盘亏", null, reason);
        biz.log("库存盘点", "修改", "", "《" + b.get("bookName") + "》账面 " + before + " → 实盘 " + actualQty
                + "，" + (diff > 0 ? "盘盈 " : "盘亏 ") + Math.abs(diff) + " 册");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("bookId", bookId);
        m.put("beforeQty", before);
        m.put("afterQty", actualQty);
        m.put("diff", diff);
        return m;
    }

    // ==================================================== 订单

    public Map<String, Object> orderPage(int page, int size, String keyword, String status, Long bookId) {
        List<Object> args = new ArrayList<>();
        StringBuilder w = new StringBuilder("from book_order o left join book b on b.id=o.book_id "
                + "left join sys_user u on u.id=o.customer_id where o.deleted=0");
        if (keyword != null && !keyword.isBlank()) {
            w.append(" and (o.order_no like ? or b.book_name like ? or o.receiver_name like ? or o.express_no like ?)");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        if (status != null && !status.isBlank()) {
            w.append(" and o.status=?");
            args.add(status);
        }
        if (bookId != null) {
            w.append(" and o.book_id=?");
            args.add(bookId);
        }
        w.append(biz.orderScope(args));
        return biz.page("o.*, b.book_name, b.book_no, b.cover_image, b.author, u.real_name customer_name",
                w.toString(), args, "o.create_time desc, o.id desc", page, size);
    }

    public Map<String, Object> orderDetail(Long id) {
        Map<String, Object> o = biz.find("select o.*, b.book_name, b.book_no, b.cover_image, b.author, b.publisher, "
                + "b.sale_price, b.stock_qty, u.real_name customer_name, u.phone customer_phone from book_order o "
                + "left join book b on b.id=o.book_id left join sys_user u on u.id=o.customer_id where o.id=?", id);
        if (o == null) {
            throw new BizException("订单不存在");
        }
        // 读者只能看本人订单，越权直接 403（文档第 2 节）
        if (UserContext.isReader()
                && !UserContext.userId().equals(((Number) o.get("customerId")).longValue())) {
            throw new BizException(403, "无权查看他人订单");
        }
        List<Map<String, Object>> tl = new ArrayList<>();
        tl.add(node("提交订单", o.get("createTime")));
        if (o.get("payTime") != null) {
            tl.add(node("支付成功", o.get("payTime")));
        }
        if (o.get("deliverTime") != null) {
            tl.add(node("商家发货", o.get("deliverTime")));
        }
        if (o.get("signTime") != null) {
            tl.add(node("确认收货", o.get("signTime")));
        }
        o.put("timeline", tl);
        o.put("returns", biz.list("select * from book_return where order_id=? and deleted=0 "
                + "order by create_time desc", id));
        o.put("review", biz.find("select * from book_review where order_id=? and deleted=0 limit 1", id));
        return o;
    }

    private Map<String, Object> node(String name, Object time) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("time", time);
        return m;
    }

    /** 下单：锁库存、运费满 99 免、优惠不超图书金额 50%（文档 5.3） */
    @Transactional
    public Map<String, Object> orderCreate(Map<String, Object> d) {
        Long bookId = num(d.get("bookId"));
        int qty = num(d.get("itemCount")) == null ? 1 : num(d.get("itemCount")).intValue();
        if (bookId == null || qty < 1 || qty > 50) {
            throw new BizException("购买件数须在 1-50 之间");
        }
        Map<String, Object> b = biz.find("select * from book where id=? and deleted=0", bookId);
        if (b == null) {
            throw new BizException("图书不存在");
        }
        if (!"在售".equals(str(b.get("status")))) {
            throw new BizException("该书当前为「" + b.get("status") + "」，暂不可购买");
        }
        int stock = ((Number) b.get("stockQty")).intValue();
        int locked = b.get("lockedQty") == null ? 0 : ((Number) b.get("lockedQty")).intValue();
        if (stock - locked < qty) {
            throw new BizException("可售库存不足，仅剩 " + Math.max(0, stock - locked) + " 册，可预约到货提醒");
        }
        BigDecimal price = (BigDecimal) b.get("salePrice");
        BigDecimal amount = price.multiply(new BigDecimal(qty)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal freight = amount.compareTo(new BigDecimal("99")) >= 0 ? BigDecimal.ZERO : new BigDecimal("6.00");
        BigDecimal discount = BizService.bd(d.get("discountAmount"));
        if (discount.compareTo(amount.multiply(new BigDecimal("0.5"))) > 0) {
            throw new BizException("优惠金额不得超过图书金额的 50%");
        }
        BigDecimal pay = amount.add(freight).subtract(discount).setScale(2, RoundingMode.HALF_UP);
        if (pay.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("实付金额不得为负");
        }
        String no = biz.no("BO", "book_order", "order_no", 6);
        Long customer = UserContext.isReader() ? UserContext.userId()
                : (num(d.get("customerId")) == null ? UserContext.userId() : num(d.get("customerId")));
        Long id = biz.insert("insert into book_order (order_no, book_id, customer_id, item_count, book_amount, "
                        + "freight, discount_amount, pay_amount, receiver_name, receiver_phone, address_desc, express_no, "
                        + "pay_time, deliver_time, sign_time, logistics_time, cancel_reason, status, create_by, "
                        + "create_time, update_by, update_time, deleted, version) "
                        + "values (?,?,?,?,?,?,?,?,?,?,?,null,null,null,null,null,null,'待支付',?,?,?,?,0,0)",
                no, bookId, customer, qty, amount, freight, discount, pay,
                d.get("receiverName") == null ? UserContext.realName() : d.get("receiverName"),
                d.get("receiverPhone"), d.get("addressDesc"), customer, LocalDateTime.now(),
                customer, LocalDateTime.now());
        // 锁定库存：可售不减、锁定加，发货时再真正出库，取消时释放（与文档 5.3「提交订单时锁库存」一致）
        biz.update("update book set locked_qty=locked_qty+?, update_time=? where id=?", qty, LocalDateTime.now(), bookId);
        biz.log("订单管理", "新增", no, "创建订单《" + b.get("bookName") + "》" + qty + " 册，实付 " + pay);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("orderNo", no);
        m.put("payAmount", pay);
        m.put("freight", freight);
        return m;
    }

    /** 支付登记：待支付 → 待发货，超过 30 分钟拒绝（文档 5.3） */
    @Transactional
    public void orderPay(Long id) {
        Map<String, Object> o = biz.find("select * from book_order where id=?", id);
        if (o == null) {
            throw new BizException("订单不存在");
        }
        biz.requireNext(str(o.get("status")), "待发货");
        java.sql.Timestamp ts = (java.sql.Timestamp) jdbc.queryForObject(
                "select create_time from book_order where id=?", java.sql.Timestamp.class, id);
        if (ts != null && ts.toLocalDateTime().plusMinutes(30).isBefore(LocalDateTime.now())) {
            throw new BizException("订单已超过 30 分钟支付时限，请取消后重新下单");
        }
        biz.update("update book_order set status='待发货', pay_time=?, update_by=?, update_time=?, "
                + "version=version+1 where id=?", LocalDateTime.now(), UserContext.userId(), LocalDateTime.now(), id);
        biz.log("订单管理", "审核", str(o.get("orderNo")), "支付登记完成，订单转待发货");
    }

    /** 发货：校验锁定库存、登记物流单号与首个节点；同一单号不可复用（文档 5.4） */
    @Transactional
    public void orderDeliver(Long id, String expressNo, String company, String shortageReason) {
        Map<String, Object> o = biz.find("select o.*, b.book_name, b.stock_qty, b.locked_qty from book_order o "
                + "left join book b on b.id=o.book_id where o.id=?", id);
        if (o == null) {
            throw new BizException("订单不存在");
        }
        biz.requireNext(str(o.get("status")), "已发货");
        String en = str(expressNo).trim();
        if (en.length() < 6) {
            throw new BizException("请填写有效的物流单号（至少 6 位）");
        }
        if (biz.one("select count(*) from book_order where express_no=? and deleted=0", en) > 0) {
            throw new BizException("该物流单号已被其他订单使用");
        }
        Long bookId = ((Number) o.get("bookId")).longValue();
        int qty = ((Number) o.get("itemCount")).intValue();
        int stock = ((Number) o.get("stockQty")).intValue();
        int locked = o.get("lockedQty") == null ? 0 : ((Number) o.get("lockedQty")).intValue();
        if (stock < qty) {
            throw new BizException("实际库存不足（" + stock + " 册），请先在缺货明细中取消并说明原因"
                    + (str(shortageReason).isEmpty() ? "" : "：" + shortageReason));
        }
        // 出库：可售减、锁定同步释放，并写销售出库流水
        biz.update("update book set locked_qty=greatest(locked_qty-?,0) where id=?", qty, bookId);
        biz.changeStock(bookId, -qty, "销售出库", str(o.get("orderNo")), "订单发货出库");
        biz.update("update book_order set status='已发货', express_no=?, deliver_time=?, logistics_time=?, "
                        + "update_by=?, update_time=?, version=version+1 where id=?",
                en, LocalDateTime.now(), LocalDateTime.now(), UserContext.userId(), LocalDateTime.now(), id);
        biz.message(((Number) o.get("customerId")).longValue(), "订单", "您的订单已发货",
                "订单 " + o.get("orderNo") + " 已发出，物流单号 " + en
                        + (company == null ? "" : "（" + company + "）") + "，可在我的订单查看物流节点。", "/my/orders", id);
        biz.log("订单发货", "发货", str(o.get("orderNo")), "发货完成，物流单号 " + en
                + (company == null ? "" : "，快递 " + company));
    }

    /** 确认收货：已发货/已签收 → 已完成，销量计入统计 */
    @Transactional
    public void orderConfirm(Long id) {
        Map<String, Object> o = biz.find("select * from book_order where id=?", id);
        if (o == null) {
            throw new BizException("订单不存在");
        }
        String st = str(o.get("status"));
        if (!List.of("已发货", "已签收").contains(st)) {
            throw new BizException("订单当前为「" + st + "」，不能确认收货");
        }
        if (UserContext.isReader() && !UserContext.userId().equals(((Number) o.get("customerId")).longValue())) {
            throw new BizException(403, "只能确认本人的订单");
        }
        biz.update("update book_order set status='已完成', sign_time=?, logistics_time=?, update_by=?, "
                        + "update_time=?, version=version+1 where id=?",
                LocalDateTime.now(), LocalDateTime.now(), UserContext.userId(), LocalDateTime.now(), id);
        biz.update("update book set total_sales=total_sales+?, update_time=? where id=?",
                ((Number) o.get("itemCount")).intValue(), LocalDateTime.now(), o.get("bookId"));
        biz.log("订单管理", "审核", str(o.get("orderNo")), "读者确认收货，订单完成并计入销量");
    }

    /** 取消：待支付/待发货可取消，释放锁定并回补可售库存 */
    @Transactional
    public void orderCancel(Long id, String reason) {
        if (str(reason).trim().length() < 5) {
            throw new BizException("取消原因不少于 5 字");
        }
        Map<String, Object> o = biz.find("select * from book_order where id=?", id);
        if (o == null) {
            throw new BizException("订单不存在");
        }
        String st = str(o.get("status"));
        biz.requireNext(st, "已取消");
        Long bookId = ((Number) o.get("bookId")).longValue();
        int qty = ((Number) o.get("itemCount")).intValue();
        biz.update("update book set locked_qty=greatest(locked_qty-?,0) where id=?", qty, bookId);
        if ("待发货".equals(st)) {
            // 已支付订单取消需回补库存并写流水，便于账实核对
            biz.changeStock(bookId, qty, "退货入库", str(o.get("orderNo")), "整单取消回补库存");
        }
        biz.update("update book_order set status='已取消', cancel_reason=?, update_by=?, update_time=?, "
                + "version=version+1 where id=?", reason, UserContext.userId(), LocalDateTime.now(), id);
        biz.log("订单管理", "修改", str(o.get("orderNo")), "订单取消：" + reason);
    }

    // ==================================================== 书评

    public Map<String, Object> reviewPage(int page, int size, String keyword, String status, Long bookId) {
        List<Object> args = new ArrayList<>();
        StringBuilder w = new StringBuilder("from book_review r left join book b on b.id=r.book_id "
                + "left join sys_user u on u.id=r.customer_id where r.deleted=0");
        if (keyword != null && !keyword.isBlank()) {
            w.append(" and (r.content like ? or b.book_name like ? or r.review_no like ?)");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        if (status != null && !status.isBlank()) {
            w.append(" and r.status=?");
            args.add(status);
        }
        if (bookId != null) {
            w.append(" and r.book_id=?");
            args.add(bookId);
        }
        if (UserContext.isReader()) {
            w.append(" and r.customer_id=?");
            args.add(UserContext.userId());
        }
        return biz.page("r.*, b.book_name, b.cover_image, u.real_name customer_name, u.avatar", w.toString(), args,
                "r.create_time desc, r.id desc", page, size);
    }

    @Transactional
    public void reviewAudit(Long id, String target, String reason) {
        Map<String, Object> r = biz.find("select * from book_review where id=?", id);
        if (r == null) {
            throw new BizException("书评不存在");
        }
        if (UserContext.userId().equals(((Number) r.get("customerId")).longValue())) {
            throw new BizException("审核人不得审核本人提交的内容");
        }
        if (!List.of("已展示", "已隐藏", "已屏蔽").contains(target)) {
            throw new BizException("审核目标状态不合法");
        }
        if (str(r.get("status")).equals(target)) {
            throw new BizException("书评已是「" + target + "」状态");
        }
        biz.update("update book_review set status=?, update_by=?, update_time=?, version=version+1 where id=?",
                target, UserContext.userId(), LocalDateTime.now(), id);
        biz.message(((Number) r.get("customerId")).longValue(), "审核", "您的书评审核结果：" + target,
                "书评 " + r.get("reviewNo") + (str(reason).isEmpty() ? "" : "，说明：" + reason),
                "/my/reviews", id);
        biz.log("书评审核", "审核", str(r.get("reviewNo")), "书评状态 " + r.get("status") + " → " + target);
    }

    /** 回复：只允许回复一次，回复后不可修改（文档 5.5） */
    @Transactional
    public void reviewReply(Long id, String content) {
        if (str(content).trim().length() < 5) {
            throw new BizException("回复内容不少于 5 字");
        }
        Map<String, Object> r = biz.find("select * from book_review where id=?", id);
        if (r == null) {
            throw new BizException("书评不存在");
        }
        if (!str(r.get("replyContent")).isBlank()) {
            throw new BizException("该书评已回复，回复后不可修改");
        }
        biz.update("update book_review set reply_content=?, reply_time=?, update_by=?, update_time=?, "
                        + "version=version+1 where id=?",
                content.trim(), LocalDateTime.now(), UserContext.userId(), LocalDateTime.now(), id);
        biz.message(((Number) r.get("customerId")).longValue(), "审核", "店员回复了您的书评", content.trim(),
                "/my/reviews", id);
        biz.log("书评管理", "新增", str(r.get("reviewNo")), "店员回复书评");
    }

    /** 点有用：同一用户同一书评只能点一次，取消后回减 */
    @Transactional
    public Map<String, Object> reviewUseful(Long id) {
        Long uid = UserContext.userId();
        long exists = biz.one("select count(*) from review_useful where review_id=? and user_id=?", id, uid);
        boolean added;
        if (exists > 0) {
            biz.update("delete from review_useful where review_id=? and user_id=?", id, uid);
            biz.update("update book_review set useful_count=greatest(useful_count-1,0) where id=?", id);
            added = false;
        } else {
            biz.update("insert into review_useful (review_id, user_id, create_time, update_time, deleted) "
                    + "values (?,?,?,?,0)", id, uid, LocalDateTime.now(), LocalDateTime.now());
            biz.update("update book_review set useful_count=useful_count+1 where id=?", id);
            added = true;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("useful", added);
        m.put("count", biz.one("select useful_count from book_review where id=?", id));
        return m;
    }

    /** 提交书评：仅已签收/已完成订单可评，内容≥15 字，命中敏感词自动隐藏转人工 */
    @Transactional
    public Map<String, Object> reviewCreate(Map<String, Object> d) {
        Long orderId = num(d.get("orderId"));
        String content = str(d.get("content")).trim();
        if (content.length() < 15) {
            throw new BizException("书评内容不少于 15 字");
        }
        Map<String, Object> o = biz.find("select * from book_order where id=?", orderId);
        if (o == null) {
            throw new BizException("订单不存在");
        }
        if (!List.of("已签收", "已完成").contains(str(o.get("status")))) {
            throw new BizException("只有已签收的订单可以评价");
        }
        if (!UserContext.userId().equals(((Number) o.get("customerId")).longValue())) {
            throw new BizException(403, "只能评价本人的订单");
        }
        if (biz.one("select count(*) from book_review where order_id=? and book_id=? and deleted=0",
                orderId, o.get("bookId")) > 0) {
            throw new BizException("该订单的这本书已评价过，30 天内可追评一次");
        }
        boolean sensitive = content.matches(".*(微信|加V|加v|QQ|qq|电话|手机号|http|www).*");
        String status = sensitive ? "已隐藏" : "待审核";
        String no = biz.no("SP", "book_review", "review_no", 6);
        Long id = biz.insert("insert into book_review (review_no, book_id, order_id, customer_id, score, title, "
                        + "content, image_urls, useful_count, reply_content, reply_time, status, create_by, create_time, "
                        + "update_by, update_time, deleted, version) values (?,?,?,?,?,?,?,?,0,null,null,?,?,?,?,?,0,0)",
                no, o.get("bookId"), orderId, UserContext.userId(),
                num(d.get("score")) == null ? 5 : num(d.get("score")), d.get("title"), content,
                d.get("imageUrls"), status, UserContext.userId(), LocalDateTime.now(),
                UserContext.userId(), LocalDateTime.now());
        biz.log("书评管理", "新增", no, "读者提交书评，评分 " + d.get("score")
                + (sensitive ? "，命中敏感词已自动隐藏待人工审核" : "，进入待审核"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("status", status);
        return m;
    }

    // ==================================================== 退换

    public Map<String, Object> returnPage(int page, int size, String keyword, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder w = new StringBuilder("from book_return r left join book b on b.id=r.book_id "
                + "left join book_order o on o.id=r.order_id left join sys_user u on u.id=o.customer_id "
                + "where r.deleted=0");
        if (keyword != null && !keyword.isBlank()) {
            w.append(" and (r.return_no like ? or b.book_name like ? or o.order_no like ?)");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        if (status != null && !status.isBlank()) {
            w.append(" and r.status=?");
            args.add(status);
        }
        if (UserContext.isReader()) {
            w.append(" and o.customer_id=?");
            args.add(UserContext.userId());
        }
        return biz.page("r.*, b.book_name, b.cover_image, o.order_no, o.pay_amount, o.sign_time, "
                        + "u.real_name customer_name", w.toString(), args, "r.create_time desc, r.id desc", page, size);
    }

    /** 申请退换：签收 7 天内、数量不超件数、同订单同图书仅一个进行中（文档 5.6） */
    @Transactional
    public Map<String, Object> returnApply(Map<String, Object> d) {
        Long orderId = num(d.get("orderId"));
        Map<String, Object> o = biz.find("select * from book_order where id=?", orderId);
        if (o == null) {
            throw new BizException("订单不存在");
        }
        String st = str(o.get("status"));
        if (!List.of("已签收", "已完成").contains(st)) {
            throw new BizException("订单当前为「" + st + "」，只有已签收或已完成的订单可申请退换");
        }
        if (!UserContext.userId().equals(((Number) o.get("customerId")).longValue()) && UserContext.isReader()) {
            throw new BizException(403, "只能为本人的订单申请退换");
        }
        java.sql.Timestamp sign = (java.sql.Timestamp) jdbc.queryForObject(
                "select sign_time from book_order where id=?", java.sql.Timestamp.class, orderId);
        if (sign == null) {
            throw new BizException("该订单缺少签收时间，无法判断售后时限");
        }
        LocalDateTime s = sign.toLocalDateTime();
        if (s.plusDays(15).isBefore(LocalDateTime.now())) {
            throw new BizException("签收已超过 15 天，不再受理退换");
        }
        if (s.plusDays(7).isBefore(LocalDateTime.now())) {
            throw new BizException("签收已超过 7 天，只能走人工客服处理");
        }
        if (biz.one("select count(*) from book_return where order_id=? and book_id=? and status in "
                + "('待审核','待寄回','已寄回') and deleted=0", orderId, o.get("bookId")) > 0) {
            throw new BizException("该订单这本书已有进行中的退换单");
        }
        int qty = num(d.get("applyQty")) == null ? 1 : num(d.get("applyQty")).intValue();
        int bought = ((Number) o.get("itemCount")).intValue();
        if (qty < 1 || qty > bought) {
            throw new BizException("退换数量须在 1-" + bought + " 之间");
        }
        String desc = str(d.get("description")).trim();
        if (desc.length() < 10) {
            throw new BizException("问题描述不少于 10 字");
        }
        BigDecimal refund = BizService.bd(d.get("refundAmount"));
        if (refund.compareTo((BigDecimal) o.get("payAmount")) > 0) {
            throw new BizException("退款金额不得超过该明细实付金额 " + o.get("payAmount"));
        }
        String no = biz.no("TH", "book_return", "return_no", 4);
        Long id = biz.insert("insert into book_return (return_no, order_id, book_id, return_type, apply_qty, "
                        + "refund_amount, reason_type, description, inspect_result, back_express_no, audit_note, audit_time, "
                        + "refund_time, status, create_by, create_time, update_by, update_time, deleted, version) "
                        + "values (?,?,?,?,?,?,?,?,'待质检',null,null,null,null,'待审核',?,?,?,?,0,0)",
                no, orderId, o.get("bookId"), d.get("returnType"), qty, refund, d.get("reasonType"), desc,
                UserContext.userId(), LocalDateTime.now(), UserContext.userId(), LocalDateTime.now());
        biz.log("退换处理", "新增", no, "提交退换申请：" + d.get("returnType") + "，数量 " + qty);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("returnNo", no);
        return m;
    }

    /** 审核：待审核 → 待寄回 / 已驳回；通过后订单冻结为「退换中」不可评价 */
    @Transactional
    public void returnAudit(Long id, String target, String note, BigDecimal refund) {
        Map<String, Object> r = biz.find("select * from book_return where id=?", id);
        if (r == null) {
            throw new BizException("退换单不存在");
        }
        if (!"待审核".equals(str(r.get("status")))) {
            throw new BizException("退换单当前为「" + r.get("status") + "」，不能重复审核");
        }
        if (!List.of("待寄回", "已驳回").contains(target)) {
            throw new BizException("审核目标状态不合法");
        }
        if ("已驳回".equals(target) && str(note).length() < 5) {
            throw new BizException("驳回需填写不少于 5 字的说明");
        }
        BigDecimal amt = refund == null ? (BigDecimal) r.get("refundAmount") : refund;
        if (amt.compareTo(BizService.bd(r.get("refundAmount"))) > 0) {
            throw new BizException("退款金额不得超过申请金额");
        }
        biz.update("update book_return set status=?, audit_note=?, audit_time=?, refund_amount=?, back_express_no=?, "
                        + "update_by=?, update_time=?, version=version+1 where id=?",
                target, note, LocalDateTime.now(), amt,
                "待寄回".equals(target) ? null : r.get("backExpressNo"),
                UserContext.userId(), LocalDateTime.now(), id);
        if ("待寄回".equals(target)) {
            // 退换期间订单冻结（文档 4）：状态置退换中，评价入口关闭
            biz.update("update book_order set status='退换中', update_time=? where id=?",
                    LocalDateTime.now(), r.get("orderId"));
        }
        Long cust = jdbc.queryForObject("select customer_id from book_order where id=?",
                Long.class, r.get("orderId"));
        biz.message(cust, "退换", "退换申请" + ("待寄回".equals(target) ? "已通过审核" : "已被驳回"),
                "退换单 " + r.get("returnNo") + (str(note).isEmpty() ? "" : "，说明：" + note), "/my/orders", id);
        biz.log("退换处理", "审核", str(r.get("returnNo")), "退换单 " + r.get("status") + " → " + target
                + (str(note).isEmpty() ? "" : "，意见：" + note));
    }

    /**
     * 验收：退回图书入库并回补可售库存，只回补一次。
     * 已退款重复提交直接拒绝（文档 11 核心并发用例）。
     */
    @Transactional
    public void returnReceive(Long id, String inspect, BigDecimal refund, String remark) {
        Map<String, Object> r = biz.find("select * from book_return where id=?", id);
        if (r == null) {
            throw new BizException("退换单不存在");
        }
        String st = str(r.get("status"));
        if ("已退款".equals(st)) {
            throw new BizException("该退换单已退款，库存只会回补一次");
        }
        if (!List.of("已寄回", "待寄回").contains(st)) {
            throw new BizException("退换单当前为「" + st + "」，不能验收");
        }
        if (str(inspect).isEmpty()) {
            throw new BizException("请登记质检结果");
        }
        Long bookId = ((Number) r.get("bookId")).longValue();
        int qty = ((Number) r.get("applyQty")).intValue();
        String type = str(r.get("returnType"));
        // 仅退款不涉及实物回库；非本店图书不入库，避免污染库存
        if (!"仅退款".equals(type) && !"非本店图书".equals(inspect)) {
            biz.changeStock(bookId, qty, "退货入库", str(r.get("returnNo")),
                    "缺页污损".equals(inspect) ? "退回图书缺页污损，按折扣退款后入库" : "退回图书质检完好，回补可售库存");
        }
        BigDecimal amt = refund == null ? BizService.bd(r.get("refundAmount")) : refund;
        BigDecimal paid = jdbc.queryForObject("select pay_amount from book_order where id=?",
                BigDecimal.class, r.get("orderId"));
        if (amt.compareTo(paid) > 0) {
            throw new BizException("退款金额不得超过订单实付金额 " + paid);
        }
        biz.update("update book_return set status='已退款', inspect_result=?, refund_amount=?, refund_time=?, "
                        + "audit_note=?, update_by=?, update_time=?, version=version+1 where id=?",
                inspect, amt, LocalDateTime.now(), remark, UserContext.userId(), LocalDateTime.now(), id);
        // 退换结束后订单转已完成，金额与明细只读
        biz.update("update book_order set status='已完成', update_time=? where id=? and status='退换中'",
                LocalDateTime.now(), r.get("orderId"));
        Long cust = jdbc.queryForObject("select customer_id from book_order where id=?",
                Long.class, r.get("orderId"));
        biz.message(cust, "退换", "退换已完成退款",
                "退换单 " + r.get("returnNo") + " 已退款 " + amt + " 元，请留意到账信息。", "/my/orders", id);
        biz.log("退换处理", "审核", str(r.get("returnNo")), "验收（" + inspect + "）并退款 " + amt + " 元");
    }

    // ==================================================== 补货（创新点）

    public List<Map<String, Object>> ruleList() {
        return biz.list("select * from replenish_rule where deleted=0 order by id desc");
    }

    /** 保存规则：权重合计必须 100%，每次保存生成新版本，历史计算不受影响（文档 8.1） */
    @Transactional
    public Map<String, Object> ruleSave(Map<String, Object> d) {
        BigDecimal sum = BizService.bd(d.get("wSales")).add(BizService.bd(d.get("wTurnover")))
                .add(BizService.bd(d.get("wOnway"))).add(BizService.bd(d.get("wSeason")))
                .add(BizService.bd(d.get("wStockout")));
        if (sum.subtract(new BigDecimal("100")).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new BizException("五项权重合计必须等于 100%，当前为 " + sum);
        }
        Long maxId = jdbc.queryForObject("select coalesce(max(id),0) from replenish_rule", Long.class);
        String ver = "v" + ((maxId == null ? 0 : maxId) + 1);
        biz.update("update replenish_rule set status=0, update_time=? where status=1", LocalDateTime.now());
        Long id = biz.insert("insert into replenish_rule (rule_code, rule_name, rule_version, w_sales, w_turnover, "
                        + "w_onway, w_season, w_stockout, hot_line, normal_line, safety_hot, safety_normal, status, "
                        + "effective_time, remark, create_by, create_time, update_by, update_time, deleted, version) "
                        + "values ('BH-RULE','默认补货规则',?,?,?,?,?,?,?,?,?,?,1,?,?,?,?,?,?,0,0)",
                ver, BizService.bd(d.get("wSales")), BizService.bd(d.get("wTurnover")),
                BizService.bd(d.get("wOnway")), BizService.bd(d.get("wSeason")),
                BizService.bd(d.get("wStockout")), BizService.bd(d.get("hotLine")),
                BizService.bd(d.get("normalLine")),
                num(d.get("safetyHot")) == null ? 15 : num(d.get("safetyHot")),
                num(d.get("safetyNormal")) == null ? 7 : num(d.get("safetyNormal")),
                LocalDateTime.now(), d.get("remark"), UserContext.userId(), LocalDateTime.now(),
                UserContext.userId(), LocalDateTime.now());
        biz.log("规则配置", "新增", "BH-RULE " + ver, "启用新版权重规则 " + ver + "（权重合计 100%）");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("version", ver);
        return m;
    }

    /** 批量重算：逐本算五因子、总分、等级、建议量，输入快照与解释一并落库 */
    @Transactional
    public Map<String, Object> replenishGenerate() {
        Map<String, Object> rule = biz.currentRule();
        List<Map<String, Object>> books = biz.list("select b.id, b.book_name, b.stock_qty, b.shortage_count, "
                + "c.category_name from book b left join book_category c on c.id=b.category_id where b.deleted=0");
        int n = 0;
        Map<String, Integer> dist = new LinkedHashMap<>();
        for (Map<String, Object> b : books) {
            Map<String, Object> p = biz.computePlan(b, rule);
            // 同一本书同一天只保留一条，重算时先清掉当天的旧建议，避免列表里堆重复
            biz.update("delete from replenish_plan where book_id=? and date(create_time)=curdate()",
                    p.get("bookId"));
            biz.update("insert into replenish_plan (plan_no, book_id, sales_30d, stock_qty, onway_qty, turnover_days, "
                            + "season_factor, suggest_qty, sales_level, adopt_status, handler_id, avg_daily, s_sales, "
                            + "s_turnover, s_onway, s_season, s_stockout, total_score, origin_level, level_desc, "
                            + "manual_qty, manual_note, rule_version, create_by, create_time, update_by, update_time, "
                            + "deleted, version) values (?,?,?,?,?,?,?,?,?,'未采纳',null,?,?,?,?,?,?,?,?,?,null,null,?,?,?,?,?,0,0)",
                    biz.no("BH", "replenish_plan", "plan_no", 4) + "-" + p.get("bookId"),
                    p.get("bookId"), p.get("sales30d"), p.get("stockQty"), p.get("onwayQty"),
                    p.get("turnoverDays"), p.get("seasonFactor"), p.get("suggestQty"), p.get("salesLevel"),
                    p.get("avgDaily"), p.get("sSales"), p.get("sTurnover"), p.get("sOnway"),
                    p.get("sSeason"), p.get("sStockout"), p.get("totalScore"), p.get("originLevel"),
                    p.get("levelDesc"), p.get("ruleVersion"), UserContext.userId(), LocalDateTime.now(),
                    UserContext.userId(), LocalDateTime.now());
            dist.merge(str(p.get("salesLevel")), 1, Integer::sum);
            n++;
        }
        biz.log("补货计算", "重算", "BH-" + rule.get("ruleVersion"),
                "按规则 " + rule.get("ruleVersion") + " 重算 " + n + " 本图书的销量等级与建议补货量");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("count", n);
        m.put("version", rule.get("ruleVersion"));
        m.put("dist", dist);
        return m;
    }

    public Map<String, Object> replenishPage(int page, int size, String level, String keyword, String adopt) {
        List<Object> args = new ArrayList<>();
        StringBuilder w = new StringBuilder("from replenish_plan p left join book b on b.id=p.book_id "
                + "left join book_category c on c.id=b.category_id where p.deleted=0");
        if (level != null && !level.isBlank()) {
            w.append(" and p.sales_level=?");
            args.add(level);
        }
        if (adopt != null && !adopt.isBlank()) {
            w.append(" and p.adopt_status=?");
            args.add(adopt);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.append(" and (b.book_name like ? or p.plan_no like ?)");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        return biz.page("p.*, b.book_name, b.book_no, b.cover_image, b.author, b.publisher, c.category_name",
                w.toString(), args, "p.total_score desc, p.id desc", page, size);
    }

    public Map<String, Object> replenishDetail(Long id) {
        Map<String, Object> p = biz.find("select p.*, b.book_name, b.book_no, b.cover_image, b.author, b.publisher, "
                + "b.list_price, b.sale_price, c.category_name from replenish_plan p "
                + "left join book b on b.id=p.book_id left join book_category c on c.id=b.category_id "
                + "where p.id=?", id);
        if (p == null) {
            throw new BizException("补货建议不存在");
        }
        p.put("weights", biz.currentRule());
        p.put("history", biz.list("select plan_no, sales_30d, total_score, sales_level, rule_version, create_time "
                + "from replenish_plan where book_id=? and deleted=0 order by id desc limit 6", p.get("bookId")));
        p.put("stockFlows", biz.list("select flow_no, change_type, change_qty, after_qty, create_time "
                + "from stock_flow where book_id=? order by id desc limit 6", p.get("bookId")));
        return p;
    }

    /** 采纳/忽略：人工上浮≤20%，下调需填理由（文档 5.7、8.4） */
    @Transactional
    public void replenishAdopt(Long id, String status, Integer qty, String note) {
        Map<String, Object> p = biz.find("select * from replenish_plan where id=?", id);
        if (p == null) {
            throw new BizException("补货建议不存在");
        }
        if (!"未采纳".equals(str(p.get("adoptStatus")))) {
            throw new BizException("该建议已处理（" + p.get("adoptStatus") + "），无需重复操作");
        }
        if (!List.of("已采纳", "已忽略").contains(status)) {
            throw new BizException("处理结果不合法");
        }
        int suggest = ((Number) p.get("suggestQty")).intValue();
        if ("已采纳".equals(status) && qty != null) {
            int cap = (int) Math.ceil(suggest * 1.2);
            if (qty > cap) {
                throw new BizException("人工上浮不得超过建议量的 20%（最多 " + cap + " 册）");
            }
            if (qty < suggest && str(note).trim().length() < 5) {
                throw new BizException("下调建议量必须填写不少于 5 字的理由");
            }
        }
        biz.update("update replenish_plan set adopt_status=?, handler_id=?, manual_qty=?, manual_note=?, "
                        + "update_by=?, update_time=?, version=version+1 where id=?",
                status, UserContext.userId(), qty, note, UserContext.userId(), LocalDateTime.now(), id);
        biz.log("补货计算", "修改", str(p.get("planNo")), "补货建议" + status
                + (qty == null ? "" : "，人工修正为 " + qty + " 册，原因：" + str(note)));
    }

    /** 采纳率与人工修正率：验证规则是否有效（文档 8.5） */
    public Map<String, Object> replenishStat() {
        long total = biz.one("select count(*) from replenish_plan where deleted=0");
        long adopted = biz.one("select count(*) from replenish_plan where adopt_status='已采纳' and deleted=0");
        long fixed = biz.one("select count(*) from replenish_plan where manual_qty is not null and deleted=0");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", total);
        m.put("adopted", adopted);
        m.put("fixed", fixed);
        m.put("adoptRate", total == 0 ? 0.0 : Math.round(adopted * 1000.0 / total) / 10.0);
        m.put("manualRate", total == 0 ? 0.0 : Math.round(fixed * 1000.0 / total) / 10.0);
        m.put("levelDist", biz.list("select sales_level name, count(*) value from replenish_plan "
                + "where deleted=0 group by sales_level"));
        m.put("topSuggest", biz.list("select b.book_name name, p.suggest_qty value, p.sales_level level "
                + "from replenish_plan p join book b on b.id=p.book_id where p.deleted=0 "
                + "order by p.suggest_qty desc limit 8"));
        return m;
    }

    // ==================================================== 报表

    /** 销售报表：分类/出版社/图书三维度，剔除已退款订单，按签收时间归属 */
    public Map<String, Object> report(String dim, int days) {
        String group;
        String join = " join book b on b.id=o.book_id ";
        if ("publisher".equals(dim)) {
            group = "b.publisher";
        } else if ("book".equals(dim)) {
            group = "b.book_name";
        } else {
            group = "pc.category_name";
            join += " left join book_category c on c.id=b.category_id left join book_category pc on pc.id=c.parent_id ";
        }
        List<Map<String, Object>> list = biz.list("select " + group + " name, sum(o.item_count) qty, "
                + "round(sum(o.pay_amount),2) amount, round(avg(o.pay_amount),2) avgAmount from book_order o "
                + join + "where o.status not in ('待支付','已取消') and o.deleted=0 "
                + "and coalesce(o.sign_time,o.create_time) >= now() - interval ? day "
                + "and not exists(select 1 from book_return r where r.order_id=o.id and r.status='已退款') "
                + "group by " + group + " order by amount desc", days);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dim", dim == null ? "category" : dim);
        m.put("days", days);
        m.put("list", list);
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> r : list) {
            total = total.add(BizService.bd(r.get("amount")));
        }
        m.put("totalAmount", total.setScale(2, RoundingMode.HALF_UP));
        m.put("totalQty", list.stream().mapToLong(r -> ((Number) r.get("qty")).longValue()).sum());
        m.put("monthly", biz.list("select date_format(coalesce(o.sign_time,o.create_time),'%Y-%m') name, "
                + "round(sum(o.pay_amount),2) amount, count(*) orders from book_order o "
                + "where o.status not in ('待支付','已取消') and o.deleted=0 group by name "
                + "order by name desc limit 6"));
        return m;
    }

    // ==================================================== 门户

    public Map<String, Object> portalHome() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("newBooks", biz.list("select id, book_name, author, sale_price, list_price, cover_image, "
                + "stock_qty, status, total_sales from book where deleted=0 and status in ('在售','缺货') "
                + "order by publish_date desc, id desc limit 8"));
        m.put("hotBooks", biz.list("select id, book_name, author, sale_price, list_price, cover_image, "
                + "stock_qty, status, total_sales from book where deleted=0 and status in ('在售','缺货') "
                + "order by total_sales desc, id desc limit 8"));
        m.put("topics", biz.list("select c.id, c.category_name name, count(b.id) cnt from book_category c "
                + "left join book b on b.category_id=c.id and b.deleted=0 where c.parent_id<>0 and c.deleted=0 "
                + "group by c.id order by c.sort_no limit 6"));
        m.put("categories", categoryTree());
        m.put("notices", biz.list("select id, title, notice_type, publish_time from sys_notice "
                + "where status='已发布' order by publish_time desc limit 4"));
        m.put("stat", biz.find("select (select count(*) from book where deleted=0) books, "
                + "(select count(*) from book_order where deleted=0) orders, "
                + "(select count(*) from book_review where status='已展示' and deleted=0) reviews"));
        return m;
    }

    public Map<String, Object> portalBooks(int page, int size, String keyword, Long categoryId, String sort) {
        List<Object> args = new ArrayList<>();
        StringBuilder w = new StringBuilder("from book b left join book_category c on c.id=b.category_id "
                + "where b.deleted=0 and b.status in ('在售','缺货')");
        if (keyword != null && !keyword.isBlank()) {
            w.append(" and (b.book_name like ? or b.author like ? or b.publisher like ?)");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        if (categoryId != null) {
            w.append(" and (b.category_id=? or c.parent_id=?)");
            args.add(categoryId);
            args.add(categoryId);
        }
        String order = "sales".equals(sort) ? "b.total_sales desc"
                : ("new".equals(sort) ? "b.publish_date desc" : "b.create_time desc");
        return biz.page("b.*, c.category_name", w.toString(), args, order, page, size);
    }

    public Map<String, Object> myOrders(int page, int size, String status) {
        List<Object> args = new ArrayList<>();
        args.add(UserContext.userId());
        StringBuilder w = new StringBuilder("from book_order o left join book b on b.id=o.book_id "
                + "where o.deleted=0 and o.customer_id=?");
        if (status != null && !status.isBlank()) {
            w.append(" and o.status=?");
            args.add(status);
        }
        return biz.page("o.*, b.book_name, b.cover_image, b.author", w.toString(), args,
                "o.create_time desc, o.id desc", page, size);
    }

    /** 下拉选项：分类树、出版社、供应商、快递、图书、用户一次取回 */
    public Map<String, Object> options() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("categories", categoryTree());
        m.put("publishers", biz.list("select distinct publisher name from book where deleted=0 order by publisher"));
        m.put("suppliers", biz.list("select dict_label name from sys_dict_data where dict_type='supplier' order by sort_no"));
        m.put("express", biz.list("select dict_label name from sys_dict_data where dict_type='express_company' order by sort_no"));
        m.put("books", biz.list("select id, book_name, book_no, sale_price, stock_qty, cover_image from book "
                + "where deleted=0 and status<>'停售' order by book_name"));
        m.put("users", biz.list("select id, real_name, username from sys_user where deleted=0 and status=1 order by id"));
        m.put("orderStatus", biz.list("select dict_label name from sys_dict_data where dict_type='order_status' order by sort_no"));
        return m;
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private Long num(Object o) {
        if (o == null || "".equals(o)) {
            return null;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
