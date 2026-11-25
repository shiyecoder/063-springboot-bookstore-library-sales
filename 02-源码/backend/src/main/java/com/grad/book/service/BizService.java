package com.grad.book.service;

import com.grad.book.common.BizException;
import com.grad.book.common.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 业务支撑服务：分页取数、数据范围、编号生成、库存流水、订单状态机、补货五因子计算（创新点）。
 *
 * <p>约定：结果集列名统一由 {@link #rows} 从 snake_case 转为 camelCase，
 * 与系统管理接口保持一致，前端不必记两套命名。</p>
 */
@Service
@RequiredArgsConstructor
public class BizService {

    private final JdbcTemplate jdbc;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ============================================================ 通用取数
    public List<Map<String, Object>> rows(List<Map<String, Object>> list) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> src : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : src.entrySet()) {
                m.put(camel(e.getKey()), e.getValue());
            }
            out.add(m);
        }
        return out;
    }

    public Map<String, Object> row(Map<String, Object> src) {
        if (src == null) {
            return null;
        }
        return rows(List.of(src)).get(0);
    }

    public static String camel(String s) {
        if (s == null || s.indexOf('_') < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        boolean up = false;
        for (char c : s.toCharArray()) {
            if (c == '_') {
                up = true;
            } else if (up) {
                sb.append(Character.toUpperCase(c));
                up = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public long one(String sql, Object... args) {
        try {
            Long v = jdbc.queryForObject(sql, Long.class, args);
            return v == null ? 0L : v;
        } catch (Exception e) {
            // 统计失败不吞错：打到 stderr 便于定位「total=0 但表里有数据」这类问题
            System.err.println("[BizService.one] 统计失败：" + sql + " -> " + e.getMessage());
            return 0L;
        }
    }

    public BigDecimal dec(String sql, Object... args) {
        try {
            BigDecimal v = jdbc.queryForObject(sql, BigDecimal.class, args);
            return v == null ? BigDecimal.ZERO : v;
        } catch (Exception e) {
            System.err.println("[BizService.dec] 求和失败：" + sql + " -> " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /** 分页；page 从 1 开始，size 上限 100（文档 7.1） */
    public Map<String, Object> page(String select, String fromWhere, List<Object> args, int page, int size) {
        int p = Math.max(1, page);
        int s = Math.max(1, Math.min(100, size));
        String fwCount = fromWhere.trim();
        // 计数同样要防「from from」，并去掉 order by / limit，否则 count 会带上排序开销
        int ob = fwCount.toLowerCase().lastIndexOf(" order by ");
        if (ob > 0) {
            fwCount = fwCount.substring(0, ob);
        }
        int lm = fwCount.toLowerCase().lastIndexOf(" limit ");
        if (lm > 0) {
            fwCount = fwCount.substring(0, lm);
        }
        String countSql = "select count(*) "
                + (fwCount.toLowerCase().startsWith("from") ? fwCount : "from " + fwCount);
        long total = one(countSql, args.toArray());
        List<Object> a2 = new ArrayList<>(args);
        a2.add((p - 1) * s);   // MySQL 为 LIMIT offset, count
        a2.add(s);
        // 调用方已带 order by 时不再追加，避免 "order by ... order by ..." 语法错误
        String order = fromWhere.toLowerCase().contains("order by") ? "" : " order by create_time desc";
        // fromWhere 已带前导 "from" 时不再补，否则会拼出 "from from"
        String fw = fromWhere.trim();
        String head;
        if (fw.toLowerCase().startsWith("from")) {
            head = "select " + select + " " + fw;
        } else if (select.trim().toLowerCase().endsWith("from")) {
            head = "select " + select.trim() + " " + fw;
        } else {
            head = "select " + select + " from " + fw;
        }
        List<Map<String, Object>> list = rows(jdbc.queryForList(
                head + order + " limit ?, ?", a2.toArray()));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("records", list);
        m.put("total", total);
        m.put("page", p);
        m.put("size", s);
        m.put("pages", (total + s - 1) / s);
        return m;
    }

    /** 分页（显式指定排序，多表 join 时必须用这个重载避免 create_time 歧义） */
    public Map<String, Object> page(String select, String fromWhere, List<Object> args, String orderBy,
                                    int page, int size) {
        String withOrder = fromWhere + (orderBy == null || orderBy.isBlank() ? "" : " order by " + orderBy);
        return page(select, withOrder, args, page, size);
    }

    /** 系统管理页使用的分页：fromWhere 允许带前导 " from "，orderBy 由调用方给出 */
    public Map<String, Object> paged(String select, String fromWhere, List<Object> args, String orderBy,
                                     int page, int size) {
        int p = Math.max(1, page);
        int sz = Math.max(1, Math.min(100, size));
        String from = fromWhere.trim().toLowerCase().startsWith("from") ? fromWhere : " from " + fromWhere;
        long total = one("select count(*)" + from, args.toArray());
        List<Object> a2 = new ArrayList<>(args);
        a2.add((p - 1) * sz);
        a2.add(sz);
        String order = (orderBy == null || orderBy.isBlank()) ? "a.id" : orderBy;
        List<Map<String, Object>> list = rows(jdbc.queryForList(
                "select " + select + from + " order by " + order + " limit ?, ?", a2.toArray()));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("records", list);
        m.put("total", total);
        m.put("page", p);
        m.put("size", sz);
        m.put("pages", (total + sz - 1) / sz);
        return m;
    }

    /** 通用更新（系统管理使用） */
    public int update(String sql, Object... args) {
        return jdbc.update(sql, args);
    }

    /** 插入并返回自增主键 */
    public Long insert(String sql, Object... args) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            java.sql.PreparedStatement ps = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, kh);
        Number k = kh.getKey();
        return k == null ? null : k.longValue();
    }

    /** 单行查询（已转驼峰），不存在返回 null */
    public Map<String, Object> find(String sql, Object... args) {
        List<Map<String, Object>> l = jdbc.queryForList(sql, args);
        return l.isEmpty() ? null : row(l.get(0));
    }

    public List<Map<String, Object>> list(String sql, Object... args) {
        return rows(jdbc.queryForList(sql, args));
    }

    /**
     * 写操作日志。全系统只有 sys_oper_log 一张日志表（需求 6.4），
     * 早期版本同时写 sys_log 造成「日志页永远空白」，这里统一单表。
     */
    public void log(String module, String operType, String bizNo, String content) {
        jdbc.update("insert into sys_oper_log (module, oper_type, biz_no, content, oper_id, oper_name, "
                        + "oper_time, ip, create_time, update_time, deleted) values (?,?,?,?,?,?,?,?,?,?,0)",
                module, operType, bizNo, content, UserContext.userId(), UserContext.realName(),
                LocalDateTime.now(), "127.0.0.1", LocalDateTime.now(), LocalDateTime.now());
    }

    // ============================================================ 编号生成
    /** 业务编号：前缀 + yyMMdd + 指定位数序号，按当日已有数量递增 */
    public String no(String prefix, String table, String column, int digits) {
        String day = LocalDateTime.now().format(YMD).substring(2);
        long c = one("select count(*) from " + table + " where " + column + " like ?", prefix + day + "%");
        return prefix + day + String.format("%0" + digits + "d", c + 1);
    }

    // ============================================================ 数据范围
    /**
     * 订单数据范围：
     * 读者 SELF 只看本人下单；店员/库管/采购按本部门范围；运营与管理员看全部。
     */
    public String orderScope(List<Object> args) {
        // 订单是门店公共数据：只有读者需要收敛到「本人订单」，
        // 店员/库管/采购/运营都要看全店订单才能发货、验收与对账，不能按 customer_id 过滤。
        if ("READER".equals(UserContext.roleCode())) {
            args.add(UserContext.userId());
            return " and o.customer_id = ?";
        }
        return "";
    }

    // ============================================================ 库存
    /**
     * 写库存流水并同步 book.stock_qty。
     * 文档 5.2：变动后库存 = 变动前库存 + 变动数量，不一致拒绝提交；库存不得为负。
     */
    @Transactional
    public void changeStock(Long bookId, int delta, String type, String bizNo, String remark) {
        List<Map<String, Object>> l = jdbc.queryForList(
                "select stock_qty from book where id = ? and deleted = 0 for update", bookId);
        if (l.isEmpty()) {
            throw new BizException("图书不存在：" + bookId);
        }
        int before = ((Number) l.get(0).get("stock_qty")).intValue();
        int after = before + delta;
        if (after < 0) {
            throw new BizException("库存不足：当前可售 " + before + " 本，本次需扣减 " + (-delta) + " 本");
        }
        jdbc.update("update book set stock_qty = ?, update_time = now() where id = ?", after, bookId);
        writeFlow(bookId, delta, before, after, type, bizNo, remark);
        // 库存归零自动转缺货（文档 5.1）
        if (after == 0) {
            jdbc.update("update book set status = '缺货' where id = ? and status = '在售'", bookId);
        } else {
            jdbc.update("update book set status = '在售' where id = ? and status = '缺货'", bookId);
        }
    }

    public void writeFlow(Long bookId, int delta, int before, int after, String type, String bizNo, String remark) {
        if (after != before + delta) {
            throw new BizException("库存流水校验失败：变动前 " + before + " + 变动 " + delta + " ≠ 变动后 " + after);
        }
        jdbc.update("insert into stock_flow (flow_no, book_id, change_type, change_qty, before_qty, after_qty, "
                        + "biz_no, operator_id, remark, create_by, create_time, update_by, update_time, deleted, version) "
                        + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,0,0)",
                no("KC", "stock_flow", "flow_no", 6), bookId, type, delta, before, after, bizNo,
                UserContext.userId(), remark, UserContext.userId(), LocalDateTime.now(),
                UserContext.userId(), LocalDateTime.now());
    }

    // ============================================================ 订单状态机
    /** 文档第 4 节：允许的状态迁移，越级直接拒绝 */
    private static final Map<String, List<String>> NEXT = new LinkedHashMap<>();

    static {
        NEXT.put("待支付", List.of("待发货", "已取消"));
        NEXT.put("待发货", List.of("已发货", "已取消"));
        NEXT.put("已发货", List.of("已签收", "退换中"));
        NEXT.put("已签收", List.of("已完成", "退换中"));
        NEXT.put("退换中", List.of("已完成"));
        NEXT.put("已完成", List.of());
        NEXT.put("已取消", List.of());
    }

    public void requireNext(String from, String to) {
        List<String> allow = NEXT.getOrDefault(from, List.of());
        if (!allow.contains(to)) {
            throw new BizException("状态不允许从「" + from + "」流转到「" + to + "」，允许的目标：" +
                    (allow.isEmpty() ? "无（终态）" : String.join("/", allow)));
        }
    }

    public static String now() {
        return LocalDateTime.now().format(FULL);
    }

    /** 站内消息：状态流转后通知相关人 */
    public void message(Long receiverId, String type, String title, String content, String link, Long bizId) {
        if (receiverId == null) {
            return;
        }
        jdbc.update("insert into sys_message (receiver_id, title, content, msg_type, biz_type, biz_id, link_url, "
                        + "read_flag, create_time, update_time, deleted) values (?,?,?,?,?,?,?,0,?,?,0)",
                receiverId, title, content, type, "order", bizId, link,
                LocalDateTime.now(), LocalDateTime.now());
    }

    // ============================================================ 补货五因子（创新点）
    /** 当前启用规则；无记录时返回文档默认权重 */
    public Map<String, Object> currentRule() {
        Map<String, Object> r = find("select * from replenish_rule where status = 1 and deleted = 0 "
                + "order by effective_time desc, id desc limit 1");
        if (r != null) {
            return r;
        }
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("ruleVersion", "v1");
        d.put("wSales", new BigDecimal("35"));
        d.put("wTurnover", new BigDecimal("25"));
        d.put("wOnway", new BigDecimal("20"));
        d.put("wSeason", new BigDecimal("10"));
        d.put("wStockout", new BigDecimal("10"));
        d.put("hotLine", new BigDecimal("80"));
        d.put("normalLine", new BigDecimal("55"));
        d.put("safetyHot", 15);
        d.put("safetyNormal", 7);
        return d;
    }

    public static BigDecimal bd(Object o) {
        if (o == null) {
            return BigDecimal.ZERO;
        }
        if (o instanceof BigDecimal b) {
            return b;
        }
        return new BigDecimal(o.toString());
    }

    /** 季节与开学系数：按分类与月份（开学季 2/3/8/9 月偏高，寒暑假偏低） */
    public double seasonFactor(String categoryName, int month) {
        double base = 1.00;
        if (categoryName == null) {
            categoryName = "";
        }
        if (categoryName.contains("教辅") || categoryName.contains("考试")) {
            base = (month == 2 || month == 3 || month == 8 || month == 9) ? 1.40 : 1.05;
        } else if (categoryName.contains("童书") || categoryName.contains("少儿")) {
            base = (month == 1 || month == 2 || month == 7 || month == 8) ? 1.25 : 1.00;
        } else if (categoryName.contains("文学") || categoryName.contains("小说")) {
            base = (month == 6 || month == 7 || month == 12) ? 1.15 : 1.00;
        } else if (categoryName.contains("科技") || categoryName.contains("计算机")) {
            base = (month == 3 || month == 9 || month == 10) ? 1.20 : 0.95;
        }
        return base;
    }

    /**
     * 五因子评分（各因子 0-100 归一化分，加权后为补货优先级分）。
     * 归一化基准取「单店 30 天 60 件」为销量满分，固定基准避免随池子规模浮动。
     */
    public Map<String, Object> score(int sales30d, int stock, int onway, double turnover,
                                     double season, int shortage) {
        double sSales = Math.min(100.0, sales30d / 60.0 * 100.0);
        double sTurn;
        if (sales30d == 0) {
            sTurn = 20;                                  // 无销量按滞销处理（文档：周转记 999）
        } else if (turnover <= 7) {
            sTurn = 100;
        } else if (turnover <= 15) {
            sTurn = 90;
        } else if (turnover <= 30) {
            sTurn = 75;
        } else if (turnover <= 60) {
            sTurn = 55;
        } else if (turnover <= 120) {
            sTurn = 35;
        } else if (turnover <= 180) {
            sTurn = 20;
        } else {
            sTurn = 5;
        }
        double sOnway = onway == 0 ? 100 : onway <= 10 ? 80 : onway <= 30 ? 60 : 40;
        double sSeason = season >= 1.20 ? 100 : season >= 1.10 ? 90 : season >= 1.00 ? 75
                : season >= 0.95 ? 60 : season >= 0.90 ? 50 : 30;
        double sStock = shortage == 0 ? 100 : shortage == 1 ? 85 : shortage == 2 ? 70
                : shortage == 3 ? 55 : 40;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sSales", round1(sSales));
        m.put("sTurnover", round1(sTurn));
        m.put("sOnway", round1(sOnway));
        m.put("sSeason", round1(sSeason));
        m.put("sStockout", round1(sStock));
        return m;
    }

    public static BigDecimal round1(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP);
    }

    /** 按规则计算单本图书的补货建议（含留痕与解释） */
    public Map<String, Object> computePlan(Map<String, Object> book, Map<String, Object> rule) {
        Long bookId = ((Number) book.get("id")).longValue();
        int stock = book.get("stockQty") == null ? 0 : ((Number) book.get("stockQty")).intValue();
        int shortage = book.get("shortageCount") == null ? 0 : ((Number) book.get("shortageCount")).intValue();
        int sales30d = (int) one("select coalesce(sum(o.item_count),0) from book_order o "
                + "where o.book_id = ? and o.deleted = 0 and o.status in ('已签收','已完成') "
                + "and o.sign_time >= date_sub(now(), interval 30 day)", bookId);
        // 在途 = 已登记但未质检入库的数量（文档：采购单未入库）
        int onway = (int) one("select coalesce(sum(in_qty),0) from stock_in where book_id = ? "
                + "and status = '待质检' and deleted = 0", bookId);
        double avgDaily = sales30d / 30.0;
        double turnover = avgDaily <= 0 ? 999 : stock / avgDaily;
        String catName = (String) book.get("categoryName");
        double season = seasonFactor(catName, LocalDateTime.now().getMonthValue());
        Map<String, Object> sc = score(sales30d, stock, onway, turnover, season, shortage);

        BigDecimal wS = bd(rule.get("wSales"));
        BigDecimal wT = bd(rule.get("wTurnover"));
        BigDecimal wO = bd(rule.get("wOnway"));
        BigDecimal wE = bd(rule.get("wSeason"));
        BigDecimal wK = bd(rule.get("wStockout"));
        BigDecimal total = bd(sc.get("sSales")).multiply(wS)
                .add(bd(sc.get("sTurnover")).multiply(wT))
                .add(bd(sc.get("sOnway")).multiply(wO))
                .add(bd(sc.get("sSeason")).multiply(wE))
                .add(bd(sc.get("sStockout")).multiply(wK))
                .divide(new BigDecimal("100"), 1, RoundingMode.HALF_UP);

        BigDecimal hotLine = bd(rule.get("hotLine"));
        BigDecimal normalLine = bd(rule.get("normalLine"));
        String level;
        if (sales30d == 0 && stock == 0) {
            level = "断销";
        } else if (total.compareTo(hotLine) >= 0) {
            level = "畅销";
        } else if (total.compareTo(normalLine) >= 0) {
            level = "平销";
        } else {
            level = "滞销";
        }
        int safety = "畅销".equals(level) ? bd(rule.get("safetyHot")).intValue()
                : "平销".equals(level) ? bd(rule.get("safetyNormal")).intValue() : 0;
        double raw = avgDaily * (30 + safety) * season - stock - onway;
        int suggest = Math.max(0, (int) Math.round(raw));

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("bookId", bookId);
        m.put("sales30d", sales30d);
        m.put("stockQty", stock);
        m.put("onwayQty", onway);
        m.put("turnoverDays", BigDecimal.valueOf(Math.min(turnover, 999)).setScale(1, RoundingMode.HALF_UP));
        m.put("seasonFactor", BigDecimal.valueOf(season).setScale(2, RoundingMode.HALF_UP));
        m.put("avgDaily", BigDecimal.valueOf(avgDaily).setScale(2, RoundingMode.HALF_UP));
        m.put("suggestQty", suggest);
        m.put("salesLevel", level);
        m.put("originLevel", level);
        m.putAll(sc);
        m.put("totalScore", total);
        m.put("ruleVersion", rule.get("ruleVersion"));
        m.put("levelDesc", explain(sc, sales30d, onway, turnover, shortage, level, suggest));
        return m;
    }

    /** 结果解释：主要影响因素 + 未满足条件 + 可改进项（文档第 8 节第 3 条，不能只给标签） */
    private String explain(Map<String, Object> sc, int sales, int onway, double turnover,
                           int shortage, String level, int suggest) {
        List<String> weak = new ArrayList<>();
        if (bd(sc.get("sTurnover")).doubleValue() < 60) {
            weak.add("周转偏慢（" + String.format("%.1f", Math.min(turnover, 999)) + " 天）");
        }
        if (bd(sc.get("sStockout")).doubleValue() < 85) {
            weak.add("近 90 天缺货 " + shortage + " 次");
        }
        if (bd(sc.get("sSales")).doubleValue() < 40) {
            weak.add("近 30 天仅售 " + sales + " 本");
        }
        if (bd(sc.get("sOnway")).doubleValue() < 80) {
            weak.add("在途 " + onway + " 本待入库");
        }
        if (onway == 0 && suggest > 0) {
            weak.add("未见采购在途，可能存在未登记采购");
        }
        String head = "五因子加权得 " + sc.get("sSales") + "×35 + " + sc.get("sTurnover") + "×25 + "
                + sc.get("sOnway") + "×20 + " + sc.get("sSeason") + "×10 + " + sc.get("sStockout")
                + "×10，判定「" + level + "」，建议补 " + suggest + " 本";
        if ("滞销".equals(level) || "断销".equals(level)) {
            head += "；暂缓补货并列入促销，周转超 180 天转报损评估";
        } else if ("畅销".equals(level)) {
            head += "；需 3 个工作日内下单";
        } else {
            head += "；按建议值 80% 执行，两周后复评";
        }
        if (weak.isEmpty()) {
            return head + "。五项因子均达良好，无未满足条件。";
        }
        return head + "。主要短板：" + String.join("；", weak) + "。";
    }
}
