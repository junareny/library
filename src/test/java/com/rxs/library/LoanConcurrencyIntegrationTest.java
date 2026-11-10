package com.rxs.library;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rxs.library.loan.domain.vo.LoanVO;

/** 真实数据库并发借还集成测试。 */
class LoanConcurrencyIntegrationTest extends AbstractLibraryIntegrationTest {
    @Test
    @DisplayName("两名用户争抢最后一本")
    void twoUsersCompeteForLastCopy() throws Exception {
        long a = user("alice");
        long b = user("bobby");
        List<String> result = race(() -> borrowResult(a, 2L), () -> borrowResult(b, 2L));
        assertThat(result).containsExactlyInAnyOrder("OK", "OUT_OF_STOCK");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT available_copies FROM books WHERE id=2", Integer.class))
                .isZero();
        invariant();
    }

    @Test
    @DisplayName("同一用户并发争用第五本额度")
    void sameUserCompetesForFifthLoan() throws Exception {
        long uid = user("alice");
        for (long id : List.of(1L, 2L, 4L, 5L)) {
            service.borrow(uid, id);
        }
        assertThat(race(() -> borrowResult(uid, 6L), () -> borrowResult(uid, 7L)))
                .containsExactlyInAnyOrder("OK", "LOAN_LIMIT_EXCEEDED");
        assertThat(loans.countActive(uid)).isEqualTo(5);
        invariant();
    }

    @Test
    @DisplayName("同一用户不能并发重复借阅同一本")
    void sameUserCannotConcurrentlyBorrowSameBook() throws Exception {
        long uid = user("alice");
        assertThat(race(() -> borrowResult(uid, 1L), () -> borrowResult(uid, 1L)))
                .containsExactlyInAnyOrder("OK", "ALREADY_BORROWED");
        invariant();
    }

    @Test
    @DisplayName("并发归还仅恢复一次库存")
    void concurrentReturnsRestoreStockOnce() throws Exception {
        long uid = user("alice");
        LoanVO loan = service.borrow(uid, 1L);
        assertThat(race(() -> returnResult(uid, loan.id()), () -> returnResult(uid, loan.id())))
                .containsExactlyInAnyOrder("OK", "ALREADY_RETURNED");
        invariant();
    }

    @Test
    @DisplayName("满额度时并发借还保持一致")
    void sameUserBorrowAndReturnInterleaveSafely() throws Exception {
        long uid = user("alice");
        LoanVO original = service.borrow(uid, 1L);
        for (long id : List.of(2L, 4L, 5L, 6L)) {
            service.borrow(uid, id);
        }
        List<String> result =
                race(() -> returnResult(uid, original.id()), () -> borrowResult(uid, 7L));
        assertThat(result.getFirst()).isEqualTo("OK");
        assertThat(result.get(1)).isIn("OK", "LOAN_LIMIT_EXCEEDED");
        assertThat(loans.countActive(uid)).isBetween(4L, 5L);
        invariant();
    }
}
