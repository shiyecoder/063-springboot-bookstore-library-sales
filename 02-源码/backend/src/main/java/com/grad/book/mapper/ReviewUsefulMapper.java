package com.grad.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.book.entity.ReviewUseful;
import org.apache.ibatis.annotations.Mapper;

/** 书评有用记录 Mapper */
@Mapper
public interface ReviewUsefulMapper extends BaseMapper<ReviewUseful> {
}
