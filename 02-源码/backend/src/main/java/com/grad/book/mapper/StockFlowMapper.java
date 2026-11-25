package com.grad.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.book.entity.StockFlow;
import org.apache.ibatis.annotations.Mapper;

/** 库存变动流水 Mapper */
@Mapper
public interface StockFlowMapper extends BaseMapper<StockFlow> {
}
