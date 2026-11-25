#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""页面组 D：系统管理 / 消息 / 个人中心 / 读者门户 / 移动端"""
from base import *


# ---------------------------------------------------------------- 22 用户管理
def p_user():
    data = [
        ('admin', '管理员', '系统管理', '138****0001', '全部', '启用', '2026-04-01 09:00'),
        ('operator', '沈知远', '运营管理员', '138****2210', '全部', '启用', '2026-04-02 10:12'),
        ('clerk01', '许立言', '书店店员', '139****3312', '本部门', '启用', '2026-04-08 14:20'),
        ('clerk02', '陶然', '书店店员', '139****3313', '本人', '启用', '2026-05-11 09:40'),
        ('keeper01', '周慧敏', '库管员', '137****5521', '本部门', '启用', '2026-04-09 08:30'),
        ('buyer01', '孟凡舟', '采购员', '136****6612', '本部门', '启用', '2026-04-15 11:05'),
        ('user', '林书禾', '读者', '138****6271', '本人', '启用', '2026-06-20 19:02'),
    ]
    rows = [[u, rn, '<span class="tag t-blue">' + role + '</span>', ph,
             '<span class="tag ' + ('t-green' if scope == '全部' else ('t-blue' if scope == '本部门' else 't-gray')) + '">' + scope + '</span>',
             stag(st), '<span class="mono" style="font-size:12px">' + tm + '</span>',
             '<a href="#">编辑</a> <a href="#" style="color:#5B6770">重置密码</a> '
             + ('<a href="#" style="color:#B02A37">停用</a>' if st == '启用' else '<a href="#">启用</a>')]
            for u, rn, role, ph, scope, st, tm in data]
    body = page_head('用户管理', '共 8,420 个账号（员工 12 / 读者 8,408）· 角色按菜单 + 按钮 + 数据范围三层授权',
                     '<button class="btn">导入账号</button><button class="btn primary">' + ic('i-plus', 14) + ' 新增用户</button>')
    body += toolbar(['input', 'sel:全部角色', 'sel:全部状态', 'sel:全部数据范围'])
    body += '<div class="pcard" style="padding:0">' + table(['账号', '姓名', '角色', '手机号', '数据范围', '状态', '创建时间', '操作'], rows) + '</div>'
    body += pager(8420, 1, 7)
    body += '<div style="margin-top:12px;display:grid;grid-template-columns:1fr 1fr;gap:14px">'
    body += '<div class="pcard"><h3>数据范围说明</h3>' + table(['范围', '可见数据', '典型角色'], [
        ['全部', '所有订单、图书与统计', '运营管理员、系统管理员'],
        ['本部门', '本门店 / 本仓库的单据', '书店店员、库管员、采购员'],
        ['本人', '本人名下的订单与售后', '读者'],
    ]) + '</div>'
    body += '<div class="pcard"><h3>账号安全</h3>' + kvrows([
        ('密码策略', 'BCrypt 存储 · 长度 ≥ 6 · 90 天强制更换'), ('登录失败锁定', '连续 5 次锁定 30 分钟'),
        ('本月新增读者', '286 个'), ('异常登录', '2 次（异地）'),
    ]) + '<div style="height:9px"></div>' + note('前端隐藏按钮不等于授权，所有写接口必须后端复核角色与数据范围。', 'warn') + '</div>'
    body += '</div>'
    return shell('sys', '用户管理', '<b>系统设置</b> / 用户管理', body)


# ---------------------------------------------------------------- 23 角色权限
def p_role():
    menus = [
        ('经营概览', ['运营工作台', '销售报表']),
        ('图书与分类', ['图书管理', '分类维护']),
        ('交易', ['订单管理', '发货作业', '物流跟踪', '售后处理台', '退换验收']),
        ('库存', ['入库登记', '入库质检', '库存盘点', '库存流水']),
        ('内容', ['书评审核']),
        ('补货', ['补货看板', '建议详情', '分级规则配置', '计算留痕']),
        ('系统', ['用户管理', '角色权限', '菜单与字典', '操作日志']),
        ('个人', ['站内消息', '个人中心']),
    ]
    body = page_head('角色权限', '角色决定菜单与按钮权限，数据范围决定可见数据边界；「菜单管理」在分配界面中对非管理员隐藏',
                     '<button class="btn">复制角色</button><button class="btn primary">' + ic('i-check', 14) + ' 保存授权</button>')
    body += '<div style="display:grid;grid-template-columns:280px 1fr;gap:14px;align-items:start">'
    body += '<div class="pcard"><h3>角色列表</h3>'
    roles = [('运营管理员', 'ops', 2), ('书店店员', 'clerk', 3), ('库管员', 'keeper', 2), ('采购员', 'buyer', 2),
             ('读者', 'reader', 8408), ('系统管理员', 'sys', 1)]
    for nm, code, cnt in roles:
        on = ' on' if nm == '书店店员' else ''
        body += ('<div class="qitem' + on + '"><div class="l"><span style="font-weight:600">' + nm
                 + '</span><span style="font-size:11.5px;color:#5B6770">' + code + '</span></div>'
                 '<span style="font-size:12px;color:#5B6770">' + str(cnt) + ' 人</span></div>')
    body += '<button class="btn ghost" style="width:100%;margin-top:4px">新增角色</button></div>'
    body += '<div style="display:flex;flex-direction:column;gap:14px">'
    body += '<div class="pcard"><h3>角色信息<span>书店店员</span></h3><div class="fgrid">' \
            '<div class="it"><span>角色名称</span><input value="书店店员"></div>' \
            '<div class="it"><span>角色编码</span><input value="CLERK"></div>' \
            '<div class="it"><span>数据范围</span><select style="width:100%"><option>本部门</option><option>本人</option><option>全部</option></select></div>' \
            '<div class="it"><span>状态</span><select style="width:100%"><option>启用</option><option>停用</option></select></div>' \
            '</div></div>'
    body += '<div class="pcard"><h3>菜单与按钮授权<span>勾选即授权，父级未勾选时子级自动置灰</span></h3>'
    body += '<div style="display:grid;grid-template-columns:repeat(2,1fr);gap:0 26px">'
    for grp, items in menus:
        body += '<div style="margin-bottom:10px"><div style="font-size:12.5px;font-weight:600;margin-bottom:5px">' \
                '<label style="display:flex;align-items:center;gap:7px"><input type="checkbox" checked style="width:14px;height:14px">' + grp + '</label></div>'
        for it in items:
            on = it in ('订单管理', '发货作业', '物流跟踪', '售后处理台', '退换验收', '书评审核', '站内消息', '个人中心')
            body += ('<div style="font-size:12.5px;color:#5B6770;padding-left:21px;line-height:22px">'
                     '<label style="display:flex;align-items:center;gap:7px"><input type="checkbox" '
                     + ('checked' if on else '') + ' style="width:13px;height:13px">' + it + '</label></div>')
        body += '</div>'
    body += '</div>'
    body += '<div style="border-top:1px dashed #E6EAEF;padding-top:10px;display:flex;gap:22px;font-size:12.5px;color:#5B6770">' \
            '<span>按钮权限：</span><label style="display:flex;gap:6px;align-items:center"><input type="checkbox" checked style="width:13px;height:13px">新增</label>' \
            '<label style="display:flex;gap:6px;align-items:center"><input type="checkbox" checked style="width:13px;height:13px">编辑</label>' \
            '<label style="display:flex;gap:6px;align-items:center"><input type="checkbox" style="width:13px;height:13px">删除</label>' \
            '<label style="display:flex;gap:6px;align-items:center"><input type="checkbox" checked style="width:13px;height:13px">导出</label>' \
            '<label style="display:flex;gap:6px;align-items:center"><input type="checkbox" checked style="width:13px;height:13px">审核</label></div>'
    body += '</div>'
    body += '<div class="pcard"><h3>授权差异<span>与「库管员」相比</span></h3>' + table(['差异项', '书店店员', '库管员'], [
        ['发货作业 / 物流跟踪', '已授权', '未授权'],
        ['入库登记 / 库存盘点', '未授权', '已授权'],
        ['售后处理台（审核）', '已授权', '仅退回验收'],
        ['订单删除', '未授权', '未授权'],
    ]) + '</div>'
    body += '</div></div>'
    return shell('sys', '角色权限', '<b>系统设置</b> / 角色权限', body)


# ---------------------------------------------------------------- 24 菜单与字典
def p_system():
    body = page_head('菜单与字典', '菜单三级结构：目录 / 菜单 / 按钮；字典项前后端统一取值，状态字段不硬编码',
                     '<button class="btn">刷新缓存</button><button class="btn primary">' + ic('i-plus', 14) + ' 新增菜单</button>')
    body += '<div class="tabs"><a class="on" href="#">菜单管理</a><a href="#">字典类型</a><a href="#log">操作日志</a><a href="#">系统参数</a></div>'
    body += '<div style="display:grid;grid-template-columns:1fr 420px;gap:14px;align-items:start">'
    body += '<div class="pcard" style="padding:0">' + table(['菜单名称', '类型', '路由', '权限标识', '排序', '可见', '操作'], [
        ['图书与分类', '<span class="tag t-blue">目录</span>', '/book', '—', '30', '是', '<a href="#">编辑</a>'],
        ['&nbsp;&nbsp;├ 图书管理', '<span class="tag t-gray">菜单</span>', '/book/list', 'book:list', '31', '是', '<a href="#">编辑</a>'],
        ['&nbsp;&nbsp;│　├ 新增', '<span class="tag t-gray">按钮</span>', '—', 'book:add', '311', '是', '<a href="#">编辑</a>'],
        ['&nbsp;&nbsp;│　└ 调价审批', '<span class="tag t-gray">按钮</span>', '—', 'book:price:audit', '312', '是', '<a href="#">编辑</a>'],
        ['&nbsp;&nbsp;└ 分类维护', '<span class="tag t-gray">菜单</span>', '/book/category', 'category:list', '32', '是', '<a href="#">编辑</a>'],
        ['交易', '<span class="tag t-blue">目录</span>', '/trade', '—', '40', '是', '<a href="#">编辑</a>'],
        ['&nbsp;&nbsp;├ 订单管理', '<span class="tag t-gray">菜单</span>', '/order', 'order:list', '41', '是', '<a href="#">编辑</a>'],
        ['&nbsp;&nbsp;├ 发货作业', '<span class="tag t-gray">菜单</span>', '/order/deliver', 'order:deliver', '42', '是', '<a href="#">编辑</a>'],
        ['&nbsp;&nbsp;└ 售后处理台', '<span class="tag t-gray">菜单</span>', '/return', 'return:list', '43', '是', '<a href="#">编辑</a>'],
        ['补货', '<span class="tag t-blue">目录</span>', '/replenish', '—', '60', '是', '<a href="#">编辑</a>'],
        ['&nbsp;&nbsp;├ 补货看板', '<span class="tag t-gray">菜单</span>', '/replenish/board', 'replenish:list', '61', '是', '<a href="#">编辑</a>'],
        ['&nbsp;&nbsp;└ 分级规则配置', '<span class="tag t-gray">菜单</span>', '/replenish/rule', 'rule:config', '62', '否', '<a href="#">编辑</a>'],
    ]) + '</div>'
    body += '<div style="display:flex;flex-direction:column;gap:14px">'
    body += '<div class="pcard"><h3>字典数据<span>订单状态 order_status</span></h3>'
    body += table(['标签', '值', '排序', '状态'], [
        ['待支付', 'WAIT_PAY', '1', '启用'], ['待发货', 'WAIT_SHIP', '2', '启用'],
        ['已发货', 'SHIPPED', '3', '启用'], ['已签收', 'SIGNED', '4', '启用'],
        ['已完成', 'FINISHED', '5', '启用'], ['退换中', 'RETURNING', '6', '启用'],
        ['已取消', 'CANCELLED', '7', '启用'],
    ]) + '</div>'
    body += '<div class="pcard"><h3>字典类型<span>共 24 个</span></h3>' + kvrows([
        ('order_status', '订单状态 · 7 项'), ('stock_change_type', '库存变动类型 · 6 项'),
        ('sales_level', '销量等级 · 4 项'), ('return_type', '退换类型 · 3 项'),
        ('quality_result', '质检结果 · 5 项'), ('publisher', '出版社 · 62 项'),
    ], 116) + '</div>'
    body += '</div></div>'
    body += '<div class="pcard" style="margin-top:13px" id="log"><h3>操作日志<span>关键写操作留痕 · 保留 180 天</span></h3>'
    body += table(['时间', '操作人', '模块', '操作类型', '业务单号', '内容', 'IP'], [
        ['2026-11-02 10:12', '周慧敏', '入库质检', '审核', 'RK2611010006', '质检合格 117 册，生成入库流水 KC261101000018', '10.12.3.41'],
        ['2026-11-02 09:48', '许立言', '发货作业', '更新', 'BO2611010030', '登记物流单号 SF1388204663012', '10.12.3.28'],
        ['2026-11-02 00:30', '系统任务', '补货计算', '计算', 'BH2611010001..12', '按规则 v2.0 生成 1,286 条建议', '127.0.0.1'],
        ['2026-11-01 16:20', '孟凡舟', '补货建议', '人工修正', 'BH2611010002', '117 → 140 册，原因：开学季团购预订', '10.12.3.55'],
        ['2026-11-01 11:05', '沈知远', '图书管理', '更新', 'BK2610310001', '售价 108.00 → 96.00（75.0% 定价）', '10.12.3.12'],
        ['2026-11-01 09:24', '周慧敏', '入库登记', '新增', 'RK2611010006', '三体（全三册）120 册，批次 0001-261101', '10.12.3.41'],
    ]) + pager(2841, 1, 6) + '</div>'
    return shell('sys', '菜单与字典', '<b>系统设置</b> / 菜单与字典', body)


# ---------------------------------------------------------------- 25 站内消息
def p_message():
    body = page_head('站内消息', '未读 4 条 · 待办、审核、超时提醒按角色分发', '<button class="btn">全部标记已读</button>')
    body += '<div style="display:grid;grid-template-columns:300px 1fr;gap:14px;align-items:start">'
    body += '<div class="pcard" style="padding:0">'
    body += '<div style="padding:11px 14px;border-bottom:1px solid #E2E5E8;font-size:13px;font-weight:600">消息分类</div>'
    for nm, cnt, on in [('全部消息', 28, True), ('待办提醒', 4, False), ('审核通知', 9, False), ('超时预警', 3, False), ('系统公告', 12, False)]:
        body += ('<div style="padding:9px 14px;border-bottom:1px solid #EEF1F4;display:flex;justify-content:space-between;font-size:13px;'
                 + ('background:#EEF2F8;' if on else '') + '"><span>' + nm + '</span>'
                 '<span class="tag ' + ('t-red' if cnt and nm == '待办提醒' else 't-gray') + '">' + str(cnt) + '</span></div>')
    body += '</div>'
    body += '<div style="display:flex;flex-direction:column;gap:10px">'
    msgs = [
        ('待办提醒', 't-amber', '有 46 笔订单待发货，其中 3 笔超 24 小时', '发货作业 · 2026-11-02 09:00', '未读', True),
        ('审核通知', 't-blue', '书评 SP2611010006 命中「联系方式」规则，等待人工复核', '书评审核 · 2026-11-02 10:02', '未读', True),
        ('超时预警', 't-red', 'BO2610300087 物流 54 小时无节点更新，请填写跟进结果', '物流跟踪 · 2026-11-02 08:00', '未读', True),
        ('待办提醒', 't-amber', '补货建议 12 项待处理，其中畅销 3 项需 3 个工作日内下单', '补货看板 · 2026-11-02 00:31', '未读', True),
        ('审核通知', 't-blue', '退换单 TH2611010004 等待审核（退货退款 ¥96.00）', '售后处理台 · 2026-11-01 09:12', '已读', False),
        ('系统公告', 't-gray', '11 月 5 日 02:00–04:00 数据库例行维护，期间下单接口只读', '系统公告 · 2026-11-01 08:00', '已读', False),
    ]
    for kind, c, title, sub, st, unread in msgs:
        body += ('<div class="pcard" style="padding:12px 16px;' + ('border-left:3px solid #C9A227' if unread else '') + '">'
                 '<div style="display:flex;justify-content:space-between;gap:12px;align-items:flex-start">'
                 '<div><div style="font-size:13.5px;font-weight:600;margin-bottom:3px">'
                 + ('<span style="display:inline-block;width:6px;height:6px;border-radius:50%;background:#B02A37;margin-right:7px;vertical-align:2px"></span>' if unread else '')
                 + title + '</div><div style="font-size:12px;color:#5B6770">' + sub + '</div></div>'
                 '<div style="display:flex;gap:8px;align-items:center;flex:0 0 auto">' + tag(kind, c)
                 + '<span class="tag t-gray">' + st + '</span><a href="#" style="font-size:12.5px">查看</a></div></div></div>')
    body += pager(28, 1, 6) + '</div></div>'
    return shell('clerk', '站内消息', '<b>个人</b> / 站内消息', body)


# ---------------------------------------------------------------- 26 个人中心
def p_profile():
    body = page_head('个人中心', '资料修改、密码修改与头像上传，头像上传后右上角同步显示', '')
    body += '<div style="display:grid;grid-template-columns:320px 1fr;gap:14px;align-items:start">'
    body += '<div class="pcard" style="text-align:center;padding:22px">'
    body += '<div style="width:84px;height:84px;border-radius:50%;background:#6B8CBE;color:#fff;display:grid;place-items:center;font-size:28px;margin:0 auto 12px">许</div>'
    body += '<div style="font-size:16px;font-weight:600">许立言</div>'
    body += '<div style="font-size:12.5px;color:#5B6770;margin-top:3px">书店店员 · 中山路门店</div>'
    body += '<div style="margin-top:12px"><button class="btn ghost">上传新头像</button></div>'
    body += '<div style="margin-top:16px;text-align:left">' + kvrows([
        ('账号', 'clerk01'), ('工号', 'EMP-2026-0031'), ('所属门店', '中山路门店'),
        ('入职时间', '2026-04-08'), ('本月处理', '1,286 笔订单'),
        ('待办 · 待发货', '46 笔（今日 18:00 前）'), ('待办 · 待审核退换', '5 单（24h 内）'),
    ], 88) + '</div></div>'
    body += '<div style="display:flex;flex-direction:column;gap:14px">'
    body += '<div class="pcard"><h3>基本资料</h3><div class="fgrid">' \
            '<div class="it"><span>姓名</span><input value="许立言"></div>' \
            '<div class="it"><span>手机号</span><input value="13933123312"></div>' \
            '<div class="it"><span>邮箱</span><input value="xuliyan@zhijian-books.cn"></div>' \
            '<div class="it"><span>所属门店</span><select style="width:100%"><option>中山路门店</option><option>新街口门店</option></select></div>' \
            '<div class="it full"><span>备注</span><textarea rows="2">负责中山路门店的拣货发货与售后初审。</textarea></div></div>'
    body += '<div style="display:flex;gap:8px;margin-top:8px"><button class="btn primary">保存资料</button><button class="btn">取消</button></div></div>'
    body += '<div class="pcard"><h3>修改密码</h3><div style="display:flex;flex-direction:column;gap:10px;max-width:420px">' \
            '<label class="f"><span>当前密码</span><input type="password" value="······" style="width:100%"></label>' \
            '<label class="f"><span>新密码</span><input type="password" value="" placeholder="至少 6 位，含字母与数字" style="width:100%"></label>' \
            '<label class="f"><span>确认密码</span><input type="password" value="" style="width:100%"></label></div>'
    body += '<button class="btn primary" style="margin-top:4px">更新密码</button>'
    body += '<div style="margin-top:10px">' + note('密码以 BCrypt 存储，修改后需重新登录；连续 5 次输错将锁定 30 分钟。', 'info') + '</div></div>'
    body += '</div></div>'
    return shell('clerk', '个人中心', '<b>个人</b> / 个人中心', body)


# ---------------------------------------------------------------- 门户公共头
def _pnav(active=''):
    links = [('书店首页', '27-shop.html'), ('全部分类', '27-shop.html#cat'), ('新书上架', '27-shop.html#new'),
             ('畅销榜', '27-shop.html#hot'), ('我的订单', '30-my-orders.html')]
    lk = ''.join('<a class="lk' + (' on' if n == active else '') + '" href="' + h + '">' + n + '</a>' for n, h in links)
    return ('<div class="pnav"><div class="brand"><span class="mk">书</span><span>纸间书店</span></div>'
            + lk + '<div class="spacer"></div>'
            '<div class="psearch">' + ic('i-search', 15) + '<span>搜书名、作者、ISBN</span></div>'
            '<a class="lk" href="29-cart.html" style="display:flex;align-items:center;gap:6px">' + ic('i-cart', 17) + '购物车<span class="tag t-red" style="margin-left:2px">3</span></a>'
            '<div style="display:flex;align-items:center;gap:8px"><span class="avatar" style="background:#6B8CBE">林</span>'
            '<span style="font-size:13px">林书禾</span></div></div>')


def _pfoot():
    return ('<div style="border-top:1px solid #E2E5E8;margin-top:22px;padding:16px 0 0;font-size:12px;color:#8B96A1;'
            'display:flex;justify-content:space-between"><span>纸间书店 · 网上书店与图书销售系统</span>'
            '<span>满 99 元免运费 · 7 天无理由退换 · 正版保障</span></div>')


# ---------------------------------------------------------------- 27 书店首页
def p_shop():
    def grid(items, h=118):
        out = ['<div class="bgrid">']
        for nm, au, sp, lp, star in items:
            out.append('<div class="bk">' + book_cover(nm, h)
                       + '<div class="nm" style="height:auto;max-height:38px">' + nm + '</div>'
                       + '<div class="au">' + au + '</div>'
                       + '<div class="pr"><b>¥' + sp + '</b><s>¥' + lp + '</s><span class="stars" style="margin-left:auto">' + star + '</span></div></div>')
        return ''.join(out) + '</div>'

    new_books = [('人类简史', '尤瓦尔·赫拉利', '51.00', '68.00', '★★★★★'),
                 ('白夜行', '东野圭吾', '37.20', '46.50', '★★★★☆'),
                 ('乡土中国', '费孝通', '22.40', '28.00', '★★★★★'),
                 ('设计中的设计', '原研哉', '46.40', '58.00', '★★★★☆'),
                 ('时间简史', '史蒂芬·霍金', '36.00', '45.00', '★★★★☆')]
    hot_books = [('解忧杂货店', '东野圭吾', '31.60', '39.50', '★★★★★'),
                 ('活着', '余华', '33.80', '45.00', '★★★★★'),
                 ('三体（全三册）', '刘慈欣', '96.00', '128.00', '★★★★★'),
                 ('百年孤独', '加西亚·马尔克斯', '41.00', '55.00', '★★★★☆'),
                 ('围城', '钱锺书', '29.50', '39.00', '★★★★★')]
    topic = [('万历十五年', '黄仁宇', '32.00', '42.00', '★★★★☆'),
             ('人类简史', '尤瓦尔·赫拉利', '51.00', '68.00', '★★★★★'),
             ('房思琪的初恋乐园', '林奕含', '38.00', '45.00', '★★★★☆'),
             ('数学之美（第3版）', '吴军', '55.00', '69.00', '★★★★★'),
             ('算法导论（第3版）', 'Thomas H. Cormen', '109.00', '128.00', '★★★★☆')]
    body = _pnav('书店首页')
    body += '<div class="pbody">'
    body += ('<div class="hero"><div><h2>开学季 · 计算机与人文书单</h2>'
             '<p>精选 128 种教材与通识读物，满 99 元免运费；会员下单再享 95 折，7 天无理由退换。</p></div>'
             '<div style="display:flex;gap:10px;flex:0 0 auto"><button class="btn primary">进入专题</button>'
             '<button class="btn">领取 20 元券</button></div></div>')
    cats = [('文学', '小说 · 散文 · 诗歌'), ('计算机', '系统架构 · 编程语言 · AI'), ('历史', '中国史 · 世界史'),
            ('社科', '社会学 · 心理学'), ('少儿', '绘本 · 科普'), ('经管', '管理 · 投资')]
    body += '<div id="cat" style="display:grid;grid-template-columns:repeat(6,1fr);gap:10px;margin:14px 0 4px">'
    for nm, sub in cats:
        body += ('<div style="background:#fff;border:1px solid #E2E5E8;border-radius:4px;padding:10px 12px">'
                 '<div style="font-size:13.5px;font-weight:600">' + nm + '</div>'
                 '<div style="font-size:11px;color:#8B96A1;margin-top:2px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">' + sub + '</div></div>')
    body += '</div>'
    body += '<div class="sec-t" id="new">新书上架<span>本月到货 86 种</span><a href="#" style="margin-left:auto;font-size:12px;font-weight:400">查看全部 ›</a></div>'
    body += grid(new_books, 104)
    body += '<div class="sec-t" id="hot">畅销榜<span>按近 30 天销量 · 每日更新</span><a href="#" style="margin-left:auto;font-size:12px;font-weight:400">完整榜单 ›</a></div>'
    body += grid(hot_books, 104)
    body += '<div class="sec-t">专题书单 · 看懂中国<span>编辑精选 12 种</span><a href="#" style="margin-left:auto;font-size:12px;font-weight:400">查看专题 ›</a></div>'
    body += grid(topic, 96)
    body += _pfoot()
    body += '</div>'
    head = '.bgrid{grid-template-columns:repeat(5,1fr)}.sec-t{margin:14px 0 9px}.bk .nm{font-size:12.5px}'
    return rawpage('书店首页', body, head, 'portal')


# ---------------------------------------------------------------- 28 图书详情（门户）
def p_book_detail():
    body = _pnav('')
    body += '<div class="pbody">'
    body += '<div style="font-size:12.5px;color:#8B96A1;margin-bottom:12px">书店首页 / 文学 / 小说 / 三体（全三册）</div>'
    body += '<div style="display:grid;grid-template-columns:330px 1fr 246px;gap:24px;align-items:start">'
    body += ('<div><div style="height:400px;background:#EEF2F8;border:1px solid #E4E9F1;border-radius:4px;display:grid;place-items:center;'
             'color:#8DA1BE;font-size:12px;position:relative"><span style="position:absolute;left:0;top:0;bottom:0;width:6px;background:rgba(58,90,140,.2)"></span>'
             '三体（全三册）封面 3:4</div>'
             '<div style="display:flex;gap:8px;margin-top:9px">'
             + ''.join('<div style="width:56px;height:56px;background:#EEF2F8;border:1px solid #E4E9F1;border-radius:3px;'
                       'display:grid;place-items:center;font-size:9px;color:#8DA1BE">' + t + '</div>'
                       for t in ['封面', '书脊', '内页', '晒图 1', '晒图 2']) + '</div></div>')
    body += ('<div><h1 style="font-size:22px;line-height:30px;font-family:\'Songti SC\',Georgia,serif">三体（全三册）</h1>'
             '<div style="color:#5B6770;font-size:13px;margin:6px 0 10px">刘慈欣 著 · 重庆出版社 · 2008-01-01 · 第 1 版</div>'
             '<div style="display:flex;align-items:baseline;gap:10px;margin-bottom:4px">'
             '<span class="stars" style="font-size:13px">★★★★★</span><span style="font-size:12.5px;color:#5B6770">4.9 分 · 428 条书评 · 2,186 人想读</span></div>'
             '<div style="background:#FBFAF7;border:1px solid #E6E2D8;border-radius:4px;padding:14px 16px;margin:12px 0">'
             '<div style="display:flex;align-items:baseline;gap:12px"><span style="font-size:13px;color:#5B6770">售价</span>'
             '<b style="font-size:28px;color:#B02A37;font-variant-numeric:tabular-nums">¥96.00</b>'
             '<s style="color:#9AA5B1;font-size:13px">定价 ¥128.00</s>'
             '<span class="tag t-red">7.5 折</span></div>'
             '<div style="font-size:12.5px;color:#5B6770;margin-top:6px">满 99 元免运费（当前差 ¥3.00）· 现货 259 册 · 48 小时内发货</div></div>'
             '<div style="display:flex;gap:10px;margin:14px 0"><select style="width:120px"><option>1 册</option><option>2 册</option><option>3 册</option></select>'
             '<button class="btn primary" style="padding:8px 26px">加入购物车</button>'
             '<button class="btn gold" style="padding:8px 26px">立即购买</button>'
             '<button class="btn">' + ic('i-star', 15) + ' 想读</button></div>'
             '<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:10px;font-size:12.5px;color:#5B6770">'
             '<div>正版保障 · 出版社直发</div><div>7 天无理由退换</div><div>满 99 免运费</div></div></div>')
    body += ('<div><div class="pcard"><h3 style="font-size:13.5px">配送与库存</h3>'
             '<div style="font-size:12.5px;color:#5B6770;line-height:1.9">江苏南京市<br>预计 11-04 送达<br>'
             '<span class="tag t-green">现货 259 册</span></div>'
             '<div style="border-top:1px solid #E2E5E8;margin:10px 0;padding-top:10px;font-size:12.5px">'
             '<div style="display:flex;justify-content:space-between"><span style="color:#5B6770">服务</span><span>7 天退换</span></div>'
             '<div style="display:flex;justify-content:space-between;margin-top:5px"><span style="color:#5B6770">运费</span><span>满 99 免运费</span></div>'
             '<div style="display:flex;justify-content:space-between;margin-top:5px"><span style="color:#5B6770">分类</span><span>文学 / 小说</span></div></div></div>'
             '<div class="pcard" style="margin-top:12px"><h3 style="font-size:13.5px">同类推荐</h3>'
             + ''.join('<div style="display:flex;gap:9px;padding:7px 0;border-bottom:1px dashed #E6EAEF">'
                       '<div style="width:34px;height:46px;background:#EEF2F8;border:1px solid #E4E9F1;border-radius:2px;flex:0 0 34px"></div>'
                       '<div style="min-width:0"><div style="font-size:12.5px;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">' + n + '</div>'
                       '<div style="font-size:11.5px;color:#B02A37">¥' + p + '</div></div></div>'
                       for n, p in [('球状闪电', '32.00'), ('超新星纪元', '28.00'), ('流浪地球', '35.00')]) + '</div></div>')
    body += '</div>'
    body += ('<div style="display:grid;grid-template-columns:1fr 320px;gap:14px;margin-top:16px;align-items:start">'
             '<div class="pcard"><h3>内容简介</h3>'
             '<div style="font-size:13px;line-height:1.9;color:#1F2933">文化大革命如火如荼进行的同时，军方探寻外星文明的绝秘计划「红岸工程」取得了突破性进展。'
             '在四光年外，三体文明正苦苦挣扎——三颗无规则运行的太阳主导下的百余次毁灭与重生，逼迫他们逃离母星。'
             '而就在此时，他们接收到了地球发来的信息……</div>'
             '<h3 style="margin-top:16px">目录摘要</h3>'
             '<div style="font-size:12.5px;color:#5B6770;line-height:2">第一章 疯狂年代　第二章 寂静的春天　第三章 红岸之一　'
             '第四章 三体、周文王、长夜　第五章 叶文洁　第六章 宇宙闪烁之一</div></div>'
             '<div class="pcard"><h3>书评 <span>428 条</span></h3>')
    reviews = [('林书禾', 5, '读完第三遍，仍然被「给岁月以文明」击中。装帧扎实，纸张偏米黄。'),
               ('陈砚青', 4, '想象力惊人，第一部节奏稍慢但值得。快递很快，书角有一点压痕。'),
               ('苏晚', 5, '国产科幻的巅峰，人物和物理设定都很扎实。')]
    for nm, sc, txt in reviews:
        body += ('<div style="padding:8px 0;border-bottom:1px dashed #E6EAEF">'
                 '<div style="display:flex;justify-content:space-between;font-size:12.5px">'
                 '<b style="font-weight:600">' + nm + '</b><span class="stars">' + '★' * sc + '☆' * (5 - sc) + '</span></div>'
                 '<div style="font-size:12px;color:#5B6770;line-height:1.7;margin-top:3px">' + txt + '</div></div>')
    body += '<button class="btn ghost" style="width:100%;margin-top:10px">查看全部 428 条书评</button></div>'
    body += '</div>'
    body += _pfoot()
    body += '</div>'
    return rawpage('图书详情', body, '', 'portal')


# ---------------------------------------------------------------- 29 购物车
def p_cart():
    body = _pnav('')
    body += '<div class="pbody">'
    body += '<div class="sec-t" style="margin-top:0">购物车<span>共 3 种 4 册</span></div>'
    body += '<div style="display:grid;grid-template-columns:1fr 316px;gap:14px;align-items:start">'
    body += '<div class="pcard" style="padding:0">'
    body += ('<div style="padding:10px 16px;border-bottom:1px solid #E2E5E8;display:flex;gap:10px;align-items:center;font-size:12.5px">'
             '<input type="checkbox" checked style="width:15px;height:15px"><span>全选</span>'
             '<span style="color:#5B6770;margin-left:8px">纸间书店 · 自营</span>'
             '<div style="flex:1"></div><span style="color:#5B6770">满 99 元免运费</span></div>')
    items = [('三体（全三册）', '刘慈欣 · 重庆出版社', '96.00', '128.00', 1, '现货', True),
             ('活着', '余华 · 作家出版社', '33.80', '45.00', 2, '现货', True),
             ('算法导论（第3版）', 'Thomas H. Cormen · 机械工业出版社', '109.00', '128.00', 1, '缺货', False)]
    for nm, sub, sp, lp, qty, st, on in items:
        body += ('<div style="display:flex;gap:12px;align-items:center;padding:13px 16px;border-bottom:1px solid #EEF1F4">'
                 '<input type="checkbox" ' + ('checked' if on else '') + ' style="width:15px;height:15px">'
                 '<div style="width:52px;height:70px;background:#EEF2F8;border:1px solid #E4E9F1;border-radius:2px;flex:0 0 52px;'
                 'display:grid;place-items:center;font-size:9px;color:#8DA1BE">封面</div>'
                 '<div style="flex:1;min-width:0"><div style="font-size:13.5px;font-weight:600">' + nm + '</div>'
                 '<div style="font-size:11.5px;color:#8B96A1;margin-top:2px">' + sub + '</div>'
                 '<div style="margin-top:5px">' + ('<span class="tag t-green">' + st + '</span>' if st == '现货' else '<span class="tag t-amber">缺货 · 可预约到货提醒</span>') + '</div></div>'
                 '<div style="width:96px;text-align:right"><b style="color:#B02A37;font-size:15px">¥' + sp + '</b>'
                 '<div style="font-size:11px;color:#9AA5B1"><s>¥' + lp + '</s></div></div>'
                 '<div style="display:flex;align-items:center;gap:0;border:1px solid #E2E5E8;border-radius:4px;overflow:hidden;height:28px">'
                 '<span style="padding:0 9px;color:#5B6770;cursor:pointer">−</span>'
                 '<span style="padding:0 10px;font-variant-numeric:tabular-nums">' + str(qty) + '</span>'
                 '<span style="padding:0 9px;color:#5B6770;cursor:pointer">＋</span></div>'
                 '<div style="width:82px;text-align:right;font-weight:600;font-variant-numeric:tabular-nums">¥'
                 + ('%.2f' % (float(sp) * qty)) + '</div>'
                 '<a href="#" style="font-size:12px;color:#8B96A1">删除</a></div>')
    body += ('<div style="padding:12px 16px;font-size:12.5px;color:#5B6770;display:flex;gap:16px">'
             '<span>合计图书金额 <b style="color:#1F2933">¥163.60</b></span>'
             '<span>运费 <b style="color:#2B7955">¥0.00</b>（已满 99）</span>'
             '<span>优惠 <b style="color:#B02A37">−¥20.00</b>（开学季券）</span></div>')
    body += '</div>'
    body += ('<div class="pcard"><h3>订单结算</h3>'
             '<div style="font-size:13px;line-height:2.1">'
             '<div style="display:flex;justify-content:space-between"><span style="color:#5B6770">已选 2 种 3 册</span><span>¥163.60</span></div>'
             '<div style="display:flex;justify-content:space-between"><span style="color:#5B6770">运费</span><span>¥0.00</span></div>'
             '<div style="display:flex;justify-content:space-between"><span style="color:#5B6770">优惠券</span><span style="color:#B02A37">−¥20.00</span></div>'
             '<div style="border-top:1px solid #E2E5E8;margin-top:6px;padding-top:6px;display:flex;justify-content:space-between">'
             '<span>应付金额</span><b style="font-size:20px;color:#B02A37">¥143.60</b></div></div>'
             '<div style="font-size:11.5px;color:#8B96A1;margin:8px 0 10px;line-height:1.7">优惠金额不得超过图书金额的 50%；提交订单后锁定库存 30 分钟。</div>'
             '<button class="btn primary" style="width:100%;height:40px">去结算</button>'
             '<div style="margin-top:10px;font-size:11.5px;color:#8B96A1;line-height:1.8">收货人：林书禾<br>江苏省南京市鼓楼区中山北路 128 号 3 幢 502<br>138****6271</div></div>')
    body += '</div>'
    body += _pfoot()
    body += '</div>'
    return rawpage('购物车', body, '', 'portal')


# ---------------------------------------------------------------- 30 我的订单
def p_my_orders():
    body = _pnav('我的订单')
    body += '<div class="pbody">'
    body += '<div class="sec-t" style="margin-top:0">我的订单<span>共 12 笔</span></div>'
    body += ('<div class="tabs"><a class="on" href="#">全部</a><a href="#">待支付<span class="c">1</span></a>'
             '<a href="#">待发货<span class="c">1</span></a><a href="#">待收货<span class="c">1</span></a>'
             '<a href="#">待评价<span class="c">2</span></a><a href="#">退换中<span class="c">1</span></a></div>')
    orders = [
        ('BO2611010031', '2026-11-01 08:52', '已签收', 't-green',
         [('三体（全三册）', 2, '96.00'), ('活着', 1, '33.80')], '¥186.00',
         ['申请退换（剩 5 天）', '去评价', '查看物流']),
        ('BO2610310094', '2026-10-31 16:38', '已完成', 't-green',
         [('人类简史', 1, '51.00'), ('万历十五年', 1, '32.00')], '¥89.00',
         ['再次购买', '查看物流']),
        ('BO2611010029', '2026-11-01 08:12', '待支付', 't-amber',
         [('百年孤独', 1, '41.00'), ('围城', 1, '29.50')], '¥76.50',
         ['去支付（剩 18 分钟）', '取消订单']),
        ('BO2610310095', '2026-10-31 19:02', '退换中', 't-amber',
         [('房思琪的初恋乐园', 1, '38.00')], '¥44.00',
         ['查看售后进度']),
    ]
    for no, tm, st, c, items, amt, acts in orders:
        body += ('<div class="pcard" style="padding:0;margin-bottom:11px">'
                 '<div style="padding:9px 16px;border-bottom:1px solid #EEF1F4;display:flex;gap:18px;font-size:12.5px;color:#5B6770">'
                 '<span>' + tm + '</span><span>订单号 <b style="color:#1F2933">' + no + '</b></span>'
                 '<div style="flex:1"></div><span class="tag ' + c + '">' + st + '</span></div>'
                 '<div style="display:flex;gap:14px;padding:13px 16px">')
        for nm, qty, price in items:
            body += ('<div style="display:flex;gap:9px;flex:1;min-width:0">'
                     '<div style="width:44px;height:60px;background:#EEF2F8;border:1px solid #E4E9F1;border-radius:2px;'
                     'flex:0 0 44px;display:grid;place-items:center;font-size:8px;color:#8DA1BE">封面</div>'
                     '<div style="min-width:0"><div style="font-size:12.5px;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">'
                     + nm + '</div><div style="font-size:11.5px;color:#8B96A1">×' + str(qty) + '</div>'
                     '<div style="font-size:12px;color:#B02A37">¥' + price + '</div></div></div>')
        body += ('<div style="width:112px;text-align:right;flex:0 0 112px"><div style="font-size:11.5px;color:#8B96A1">实付</div>'
                 '<b style="font-size:15px;color:#B02A37">' + amt + '</b></div>'
                 '<div style="width:150px;display:flex;flex-direction:column;gap:6px;align-items:flex-end;flex:0 0 150px">'
                 + ''.join('<button class="btn sm ' + ('primary' if i == 0 and st == '待支付' else '') + '">' + a + '</button>' for i, a in enumerate(acts))
                 + '</div></div></div>')
    body += '<div class="pager"><span>共 12 条 · 每页 4 条</span><span class="pg on">1</span><span class="pg">2</span><span class="pg">3</span><span class="pg">下一页</span></div>'
    body += _pfoot()
    body += '</div>'
    return rawpage('我的订单', body, '', 'portal')


# ---------------------------------------------------------------- 31 移动端
def p_mobile():
    def phone(title, inner, tab_on=0):
        tabs = ['首页', '分类', '购物车', '我的']
        bt = ''.join('<span class="' + ('on' if i == tab_on else '') + '">' + t + '</span>' for i, t in enumerate(tabs))
        return ('<div class="phone"><div class="st"><span>9:41</span><span>纸间书店</span><span>5G ▮</span></div>'
                '<div class="hd2">' + title + '</div>'
                '<div class="bd">' + inner + '</div>'
                '<div class="bt">' + bt + '</div></div>')

    left = ('<div style="display:flex;gap:8px;margin-bottom:10px"><div style="flex:1;height:32px;background:#fff;border:1px solid #E2E5E8;'
            'border-radius:16px;display:flex;align-items:center;gap:6px;padding:0 12px;font-size:12px;color:#8B96A1">'
            + ic('i-search', 14) + '搜书名、作者</div></div>'
            '<div style="display:flex;gap:9px;overflow:hidden;margin-bottom:10px">'
            + ''.join('<div style="width:44px;height:44px;border-radius:50%;background:#EEF2F8;display:grid;place-items:center;'
                      'font-size:11px;color:#3A5A8C;flex:0 0 44px">' + c + '</div>' for c in ['文学', '计算机', '历史', '社科', '少儿'])
            + '</div>'
            '<div class="mcard" style="background:#FBFAF7;border-color:#E6E2D8"><div class="t1"><span>开学季书单</span><span style="font-size:11px;color:#B02A37">满 99 免运费</span></div>'
            '<div class="t2">计算机与人文精选 128 种，会员再享 95 折</div></div>'
            '<div style="font-size:13px;font-weight:600;margin:11px 0 8px">畅销榜</div>'
            + ''.join('<div class="mcard"><div style="display:flex;gap:9px"><div style="width:40px;height:54px;background:#EEF2F8;border:1px solid #E4E9F1;'
                      'border-radius:2px;flex:0 0 40px;display:grid;place-items:center;font-size:8px;color:#8DA1BE">封面</div>'
                      '<div style="min-width:0;flex:1"><div class="t1" style="font-size:12.5px">' + n + '</div>'
                      '<div class="t2">' + a + '</div><div style="font-size:12.5px;color:#B02A37;font-weight:600;margin-top:2px">¥' + p + '</div></div></div></div>'
                      for n, a, p in [('解忧杂货店', '东野圭吾', '31.60'), ('活着', '余华', '33.80'), ('三体（全三册）', '刘慈欣', '96.00')]))

    right = ('<div class="mcard" style="border-color:#CFDAEB"><div class="t1"><span>BO2611010031</span><span class="tag t-green">已签收</span></div>'
             '<div class="t2">三体（全三册）×2　活着 ×1</div>'
             '<div style="font-size:12px;color:#B02A37;font-weight:600;margin-top:3px">实付 ¥186.00</div>'
             '<div style="display:flex;gap:7px;margin-top:9px"><button class="btn sm" style="flex:1">查看物流</button>'
             '<button class="btn sm primary" style="flex:1">去评价</button></div></div>'
             '<div class="mcard"><div class="t1"><span>物流进度</span><span style="font-size:11px;color:#2B7955">已签收</span></div>'
             '<div style="font-size:11.5px;color:#5B6770;line-height:1.9;margin-top:4px">'
             '11-01 14:26　已签收 · 本人签收<br>11-01 09:05　派送中 · 中山路营业点<br>11-01 09:02　已揽收 · 南京</div></div>'
             '<div class="mcard"><div class="t1"><span>可执行操作</span></div>'
             '<div style="display:flex;flex-direction:column;gap:7px;margin-top:6px">'
             '<button class="btn sm" style="width:100%;text-align:left">申请退换（剩 5 天）</button>'
             '<button class="btn sm" style="width:100%;text-align:left">写书评（晒图 ≤9 张）</button>'
             '<button class="btn sm" style="width:100%;text-align:left">再次购买</button></div></div>'
             '<div class="mcard"><div class="t1"><span>待办提醒</span></div>'
             '<div class="t2">BO2611010029 待支付，剩 18 分钟自动取消</div></div>')

    body = ('<div style="padding:24px 24px 10px"><h1 style="font-size:18px;margin:0 0 4px">移动端适配（&lt; 768px）</h1>'
            '<div style="font-size:12.5px;color:#5B6770">侧栏改抽屉、表格转信息卡片、主按钮固定安全区；优先支持查询、审批、现场录入、拍照上传与消息查看</div></div>'
            '<div style="display:flex;gap:34px;justify-content:center;padding:6px 24px 22px">'
            + phone('书店首页', left, 0) + phone('订单详情', right, 3) + '</div>')
    head = 'body{background:#EFF1F3}'
    return rawpage('移动端', body, head)
