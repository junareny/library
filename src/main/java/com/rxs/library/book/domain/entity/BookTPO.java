package com.rxs.library.book.domain.entity;

import java.time.Instant;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.experimental.Accessors;

/** 书目及多副本库存模型。 */
@Data
@Accessors(chain = true)
@TableName("books")
public class BookTPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String author;

    private String isbn;

    private int totalCopies;

    private int availableCopies;

    private Instant createdAt;

    public BookTPO() {}
}
