package com.grad.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.book.entity.BookCategory;
import org.apache.ibatis.annotations.Mapper;

/** 图书分类 Mapper */
@Mapper
public interface BookCategoryMapper extends BaseMapper<BookCategory> {
}
