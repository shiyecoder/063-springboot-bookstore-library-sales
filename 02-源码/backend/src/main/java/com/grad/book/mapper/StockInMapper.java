package com.grad.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.book.entity.StockIn;
import org.apache.ibatis.annotations.Mapper;

/** 入库单 Mapper */
@Mapper
public interface StockInMapper extends BaseMapper<StockIn> {
}
