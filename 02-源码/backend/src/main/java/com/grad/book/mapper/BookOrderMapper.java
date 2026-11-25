package com.grad.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.book.entity.BookOrder;
import org.apache.ibatis.annotations.Mapper;

/** 图书订单 Mapper */
@Mapper
public interface BookOrderMapper extends BaseMapper<BookOrder> {
}
