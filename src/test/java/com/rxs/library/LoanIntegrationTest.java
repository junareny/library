package com.rxs.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.rxs.library.exception.LibraryErrorCode;
import com.rxs.library.loan.domain.vo.LoanVO;

/** 借还业务规则集成测试。 */
class LoanIntegrationTest extends AbstractLibraryIntegrationTest {
    @Test
    @DisplayName("借还接口闭环与归属校验")
    void httpBorrowReturnAndOwnership() throws Exception {
        user("alice");
        user("bobby");
        String token = login("alice");
        MvcResult created =
                mvc.perform(
                                post("/ILoanBiz/borrowBook")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType("application/json")
                                        .content("{\"bookId\":1}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("body.status").value("BORROWED"))
                        .andExpect(jsonPath("body.borrowedAt").value("2026-11-09T04:00:00Z"))
                        .andExpect(jsonPath("body.dueAt").value("2026-12-09T04:00:00Z"))
                        .andReturn();
        long id =
                json.readTree(created.getResponse().getContentAsString())
                        .get("body")
                        .get("id")
                        .asLong();
        mvc.perform(
                        postJson("/ILoanBiz/returnBook", "{\"id\":" + id + "}")
                                .header("Authorization", "Bearer " + login("bobby")))
                .andExpect(status().isNotFound());
        mvc.perform(
                        postJson("/ILoanBiz/queryCurrentLoan/page", "{}")
                                .header("Authorization", "Bearer " + login("bobby")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("body.total").value(0));
        mvc.perform(
                        postJson("/ILoanBiz/returnBook", "{\"id\":" + id + "}")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(
                        postJson("/ILoanBiz/returnBook", "{\"id\":" + id + "}")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("code").value("ALREADY_RETURNED"));
        mvc.perform(
                        postJson("/ILoanBiz/queryCurrentLoan/page", "{}")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("body.total").value(0));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM loans", Long.class)).isEqualTo(1);
        invariant();
    }

    @Test
    @DisplayName("业务冲突与再次借阅历史")
    void businessConflictsAndReborrowHistory() {
        long uid = user("alice");
        LoanVO first = service.borrow(uid, 1L);
        assertCode(() -> service.borrow(uid, 1L), LibraryErrorCode.ALREADY_BORROWED);
        assertCode(() -> service.borrow(uid, 3L), LibraryErrorCode.OUT_OF_STOCK);
        assertCode(() -> service.borrow(uid, 99999L), LibraryErrorCode.RESOURCE_NOT_FOUND);
        service.returnLoan(uid, first.id());
        LoanVO second = service.borrow(uid, 1L);
        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(loans.countActive(uid)).isEqualTo(1);
        invariant();
    }

    @Test
    @DisplayName("逾期借阅仍占用五本额度")
    void fifthLoanLimitIncludesOverdueLoans() {
        long uid = user("alice");
        for (long id : List.of(1L, 2L, 4L, 5L, 6L)) {
            service.borrow(uid, id);
        }
        clock.set(NOW.plus(Duration.ofDays(31)));
        assertThat(service.current(uid, 1, 20).getRecords())
                .allMatch(l -> l.status().equals("OVERDUE"));
        assertCode(() -> service.borrow(uid, 7L), LibraryErrorCode.LOAN_LIMIT_EXCEEDED);
        invariant();
    }

    @Test
    @DisplayName("逾期边界与库存一致性")
    void overdueBoundaryUsesClockAndKeepsInventory() {
        long uid = user("alice");
        LoanVO loan = service.borrow(uid, 1L);
        clock.set(loan.dueAt());
        assertThat(service.current(uid, 1, 20).getRecords().getFirst().status())
                .isEqualTo("BORROWED");
        clock.set(loan.dueAt().plusNanos(1));
        assertThat(service.current(uid, 1, 20).getRecords().getFirst().status())
                .isEqualTo("OVERDUE");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT available_copies FROM books WHERE id=1", Integer.class))
                .isEqualTo(2);
        invariant();
    }

    @Test
    @DisplayName("同时间借阅分页排序稳定")
    void stableLoanPaginationAtSameTimestamp() {
        long uid = user("alice");
        LoanVO first = service.borrow(uid, 1L);
        LoanVO second = service.borrow(uid, 4L);
        assertThat(service.current(uid, 1, 1).getRecords().getFirst().id()).isEqualTo(second.id());
        assertThat(service.current(uid, 2, 1).getRecords().getFirst().id()).isEqualTo(first.id());
        invariant();
    }
}
