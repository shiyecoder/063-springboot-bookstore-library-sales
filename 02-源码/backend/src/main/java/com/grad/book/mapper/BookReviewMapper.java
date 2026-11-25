package com.grad.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.book.entity.BookReview;
import org.apache.ibatis.annotations.Mapper;

/** 图书书评 Mapper */
@Mapper
public interface BookReviewMapper extends BaseMapper<BookReview> {
}
