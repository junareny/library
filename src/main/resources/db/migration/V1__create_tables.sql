-- MySQL 5.7 不执行 CHECK 约束，因此使用触发器提供数据库层面的校验。
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) COLLATE utf8mb4_bin NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE books (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    isbn VARCHAR(20) NULL,
    total_copies INT NOT NULL,
    available_copies INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_books_isbn UNIQUE (isbn)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE loans (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    borrowed_at DATETIME(6) NOT NULL,
    due_at DATETIME(6) NOT NULL,
    returned_at DATETIME(6) NULL,
    active_book_id BIGINT GENERATED ALWAYS AS (CASE WHEN returned_at IS NULL THEN book_id ELSE NULL END) STORED,
    CONSTRAINT fk_loans_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_loans_book FOREIGN KEY (book_id) REFERENCES books(id),
    CONSTRAINT uk_loans_active_book UNIQUE (user_id, active_book_id),
    INDEX idx_loans_user_returned (user_id, returned_at),
    INDEX idx_loans_book (book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELIMITER $$
CREATE TRIGGER books_validate_insert BEFORE INSERT ON books FOR EACH ROW
BEGIN
    IF NEW.total_copies < 0 OR NEW.available_copies < 0 OR NEW.available_copies > NEW.total_copies THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid book inventory';
    END IF;
END$$
CREATE TRIGGER books_validate_update BEFORE UPDATE ON books FOR EACH ROW
BEGIN
    IF NEW.total_copies < 0 OR NEW.available_copies < 0 OR NEW.available_copies > NEW.total_copies THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid book inventory';
    END IF;
END$$
CREATE TRIGGER loans_validate_insert BEFORE INSERT ON loans FOR EACH ROW
BEGIN
    IF NEW.due_at <= NEW.borrowed_at OR (NEW.returned_at IS NOT NULL AND NEW.returned_at < NEW.borrowed_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid loan timestamps';
    END IF;
END$$
CREATE TRIGGER loans_validate_update BEFORE UPDATE ON loans FOR EACH ROW
BEGIN
    IF NEW.due_at <= NEW.borrowed_at OR (NEW.returned_at IS NOT NULL AND NEW.returned_at < NEW.borrowed_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid loan timestamps';
    END IF;
END$$
DELIMITER ;
