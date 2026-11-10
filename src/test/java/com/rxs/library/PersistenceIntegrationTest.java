package com.rxs.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.support.TransactionTemplate;

import com.rxs.library.loan.domain.entity.LoanTPO;
import com.rxs.library.loan.domain.vo.LoanVO;

/** MySQL 5.7 持久化与事务回滚集成测试。 */
class PersistenceIntegrationTest extends AbstractLibraryIntegrationTest {
    @Test
    @DisplayName("MySQL 5.7 触发器约束库存与借还时间")
    void mysql57TriggersRejectInvalidInventoryAndTime() {
        long uid = user("alice");
        assertThatThrownBy(() -> jdbc.update("UPDATE books SET available_copies=-1 WHERE id=1"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE books SET available_copies=total_copies+1 WHERE id=1"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        """
                                        INSERT INTO books(title,author,total_copies,available_copies,created_at)
                                        VALUES('invalid','a',-1,0,UTC_TIMESTAMP(6))
                                        """))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO loans(user_id,book_id,borrowed_at,due_at) VALUES(?,?,?,?)",
                                        uid,
                                        1,
                                        Timestamp.from(NOW),
                                        Timestamp.from(NOW)))
                .isInstanceOf(DataAccessException.class);
        LoanVO loan = service.borrow(uid, 1L);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE loans SET returned_at=? WHERE id=?",
                                        Timestamp.from(NOW.minusSeconds(1)),
                                        loan.id()))
                .isInstanceOf(DataAccessException.class);
        invariant();
    }

    @Test
    @DisplayName("借阅插入失败时回滚库存")
    void failedLoanInsertRollsBackInventory() {
        long uid = user("alice");
        jdbc.execute(
                """
                CREATE TRIGGER test_reject_loan BEFORE INSERT ON loans FOR EACH ROW
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Injected test failure'
                """);
        try {
            assertThatThrownBy(() -> service.borrow(uid, 1L)).isInstanceOf(RuntimeException.class);
            assertThat(loans.count()).isZero();
            invariant();
        } finally {
            jdbc.execute("DROP TRIGGER test_reject_loan");
        }
    }

    @Test
    @DisplayName("唯一约束冲突时回滚库存")
    void actualUniqueConflictRollsBackInventory() {
        long uid = user("alice");
        service.borrow(uid, 1L);
        TransactionTemplate tx = new TransactionTemplate(txManager);
        assertThatThrownBy(
                        () ->
                                tx.executeWithoutResult(
                                        status -> {
                                            assertThat(books.decrement(1L)).isEqualTo(1);
                                            loans.save(
                                                    new LoanTPO(
                                                            uid,
                                                            1L,
                                                            NOW,
                                                            NOW.plus(Duration.ofDays(30))));
                                        }))
                .isInstanceOf(DataAccessException.class);
        assertThat(loans.count()).isEqualTo(1);
        invariant();
    }

    @Test
    @DisplayName("库存恢复失败时回滚归还")
    void failedInventoryRestoreRollsBackReturn() {
        long uid = user("alice");
        LoanVO loan = service.borrow(uid, 1L);
        // 故意构造跨表库存不一致，但仍满足单行触发器约束，以验证归还失败后的事务回滚。
        jdbc.update("UPDATE books SET available_copies=total_copies WHERE id=1");
        assertThatThrownBy(() -> service.returnLoan(uid, loan.id()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT returned_at FROM loans WHERE id=?",
                                Timestamp.class,
                                loan.id()))
                .isNull();
        jdbc.update("UPDATE books SET available_copies=total_copies-1 WHERE id=1");
        invariant();
    }

    @Test
    @DisplayName("时间按 UTC 存储并保留微秒")
    void instantUsesUtcDatetimeAndPreservesMicroseconds() {
        Instant precise = NOW.plusNanos(123456000);
        clock.set(precise);
        long uid = user("utc_user");
        LoanVO loan = service.borrow(uid, 1L);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT DATE_FORMAT(borrowed_at, '%Y-%m-%d %H:%i:%s.%f') FROM loans WHERE id=?",
                                String.class, loan.id()))
                .isEqualTo("2026-11-09 04:00:00.123456");
        assertThat(loans.getById(loan.id()).getBorrowedAt()).isEqualTo(precise);
        assertThat(service.current(uid, 1, 20).getRecords().getFirst().dueAt())
                .isEqualTo(precise.plus(Duration.ofDays(30)));
        clock.set(precise.plusSeconds(60));
        service.returnLoan(uid, loan.id());
        assertThat(loans.getById(loan.id()).getReturnedAt()).isEqualTo(precise.plusSeconds(60));
        invariant();
    }

    @Test
    @DisplayName("MyBatis-Plus 主键回填与显式增删改查")
    void mybatisPlusCrudUsesGeneratedIdsAndExplicitUpdates() {
        long uid = user("crud_user");
        var stored = users.getById(uid);
        assertThat(stored.getId()).isPositive();
        assertThat(stored.toString()).doesNotContain(stored.getPasswordHash());
        assertThat(users.updateById(stored.setUsername("crud_updated"))).isTrue();
        assertThat(users.findByUsername("crud_user")).isNull();
        assertThat(users.findByUsername("crud_updated").getId()).isEqualTo(uid);
        assertThat(users.removeById(uid)).isTrue();
        assertThat(users.getById(uid)).isNull();
    }
}
