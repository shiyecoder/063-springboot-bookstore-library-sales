#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 NEW-063 全部设计稿 HTML"""
import os
from base import BASE
import pages_a as A
import pages_b as B
import pages_c as C
import pages_d as D

PAGES = [
    ('01-login', A.p_login),
    ('02-dash', A.p_dash),
    ('03-book', A.p_book),
    ('04-book-new', A.p_book_new),
    ('05-category', A.p_category),
    ('06-stock-in', A.p_stock_in),
    ('07-inspect', A.p_inspect),
    ('08-stocktake', B.p_stocktake),
    ('09-flow', B.p_flow),
    ('10-order', B.p_order),
    ('11-order-detail', B.p_order_detail),
    ('12-deliver', B.p_deliver),
    ('13-logistics', B.p_logistics),
    ('14-return', B.p_return),
    ('15-return-detail', B.p_return_detail),
    ('16-review-audit', B.p_review),
    ('17-replenish', C.p_replenish),
    ('18-replenish-detail', C.p_replenish_detail),
    ('19-rule', C.p_rule),
    ('20-trace', C.p_trace),
    ('21-report', C.p_report),
    ('22-user', D.p_user),
    ('23-role', D.p_role),
    ('24-system', D.p_system),
    ('25-message', D.p_message),
    ('26-profile', D.p_profile),
    ('27-shop', D.p_shop),
    ('28-book-detail', D.p_book_detail),
    ('29-cart', D.p_cart),
    ('30-my-orders', D.p_my_orders),
    ('31-mobile', D.p_mobile),
]

if __name__ == '__main__':
    for name, fn in PAGES:
        html = fn()
        path = os.path.join(BASE, name + '.html')
        with open(path, 'w', encoding='utf-8') as f:
            f.write(html)
        print(name, len(html))
    print('total', len(PAGES))
