package com.rxs.library.book.domain.vo;

import com.rxs.library.book.domain.entity.BookTPO;

/** 公开书目响应，包含馆藏数量和当前可借数量。 */
public record BookVO(
        Long id, String title, String author, String isbn, int totalCopies, int availableCopies) {
    public static BookVO from(BookTPO b) {
        return new BookVO(
                b.getId(),
                b.getTitle(),
                b.getAuthor(),
                b.getIsbn(),
                b.getTotalCopies(),
                b.getAvailableCopies());
    }
}
