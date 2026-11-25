package com.grad.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.book.entity.Book;
import org.apache.ibatis.annotations.Mapper;

/** 图书 Mapper */
@Mapper
public interface BookMapper extends BaseMapper<Book> {
}
