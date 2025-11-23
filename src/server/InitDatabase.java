package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.io.File;

public class InitDatabase {
    private static final String DB_PATH = "C:/data/library.db";

    public static void main(String[] args) {
        try {
            File dir = new File("C:/data");
            if (!dir.exists()) {
                dir.mkdirs();
                System.out.println("Đã tạo thư mục: " + dir.getAbsolutePath());
            }

            String url = "jdbc:sqlite:" + DB_PATH;
            Connection conn = DriverManager.getConnection(url);
            Statement stmt = conn.createStatement();

        stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "username TEXT UNIQUE," +
            "password TEXT," +
            "role TEXT," +
            "phone TEXT," +
            "email TEXT," +
            "avatar TEXT," +
            "created_at TEXT DEFAULT (datetime('now'))"
            + ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS books (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT," +
                    "author TEXT," +
                    "publisher TEXT," +
                    "year TEXT," +
                    "quantity INTEGER," +
                    "category TEXT," +
                    "favorite INTEGER DEFAULT 0" +
                    ")");

            try {
                stmt.execute("ALTER TABLE books ADD COLUMN cover_image TEXT DEFAULT ''");
            } catch (Exception e) {
            }

            try {
                stmt.execute("ALTER TABLE books ADD COLUMN description TEXT DEFAULT ''");
            } catch (Exception e) {
            }

            try {
                stmt.execute("ALTER TABLE books ADD COLUMN rating REAL DEFAULT 0.0");
            } catch (Exception e) {
            }

            stmt.execute("CREATE TABLE IF NOT EXISTS ratings (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER," +
                    "book_id INTEGER," +
                    "rating INTEGER," +
                    "review TEXT," +
                    "created_at TEXT DEFAULT (datetime('now'))," +
                    "FOREIGN KEY(user_id) REFERENCES users(id)," +
                    "FOREIGN KEY(book_id) REFERENCES books(id)," +
                    "UNIQUE(user_id, book_id)" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS favorites (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER," +
                    "book_id INTEGER," +
                    "added_date TEXT DEFAULT (datetime('now'))," +
                    "FOREIGN KEY(user_id) REFERENCES users(id)," +
                    "FOREIGN KEY(book_id) REFERENCES books(id)," +
                    "UNIQUE(user_id, book_id)" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS borrows (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER," +
                    "book_id INTEGER," +
                    "borrow_date TEXT," +
                    "return_date TEXT," +
                    "FOREIGN KEY(user_id) REFERENCES users(id)," +
                    "FOREIGN KEY(book_id) REFERENCES books(id))");

            // Create borrow_records table for enterprise features
            stmt.execute("CREATE TABLE IF NOT EXISTS borrow_records (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER," +
                    "book_id INTEGER," +
                    "borrow_date TEXT," +
                    "return_date TEXT," +
                    "status TEXT DEFAULT 'ACTIVE'," +
                    "rating INTEGER DEFAULT 0," +
                    "FOREIGN KEY(user_id) REFERENCES users(id)," +
                    "FOREIGN KEY(book_id) REFERENCES books(id))");

            // Sync data from borrows to borrow_records if exists
            try {
                stmt.execute("INSERT OR IGNORE INTO borrow_records (user_id, book_id, borrow_date, return_date) " +
                           "SELECT user_id, book_id, borrow_date, return_date FROM borrows");
            } catch (Exception e) {
                // Ignore if no data to sync
            }

            stmt.execute("CREATE TABLE IF NOT EXISTS activities (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER," +
                    "book_id INTEGER," +
                    "action TEXT," +
                    "action_time TEXT," +
                    "FOREIGN KEY(user_id) REFERENCES users(id)," +
                    "FOREIGN KEY(book_id) REFERENCES books(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS borrow_requests (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER NOT NULL," +
                    "book_id INTEGER NOT NULL," +
                    "request_date TEXT NOT NULL," +
                    "expected_return_date TEXT," +
                    "notes TEXT," +
                    "status TEXT DEFAULT 'PENDING'," +
                    "admin_notes TEXT," +
                    "approved_date TEXT," +
                    "FOREIGN KEY (user_id) REFERENCES users(id)," +
                    "FOREIGN KEY (book_id) REFERENCES books(id)" +
                    ")");

            try {
                stmt.execute("ALTER TABLE borrow_requests ADD COLUMN expected_return_date TEXT");
            } catch (Exception e) {
            }

            try {
                stmt.execute("ALTER TABLE borrow_requests ADD COLUMN notes TEXT");
            } catch (Exception e) {
            }

            stmt.execute("CREATE TABLE IF NOT EXISTS notifications (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER NOT NULL," +
                    "type TEXT NOT NULL," +
                    "title TEXT NOT NULL," +
                    "content TEXT NOT NULL," +
                    "created_at TEXT NOT NULL," +
                    "is_read INTEGER DEFAULT 0," +
                    "FOREIGN KEY (user_id) REFERENCES users(id)" +
                    ")");

            stmt.execute("DELETE FROM users WHERE username='admin'");
            stmt.execute("INSERT INTO users(username, password, role, phone, email) " +
                        "VALUES('admin', 'admin', 'admin', '', '')");

            // Add sample data for testing
            stmt.execute("INSERT OR IGNORE INTO books(title, author, publisher, year, quantity, category) VALUES " +
                        "('Java Programming', 'John Smith', 'Tech Publisher', '2023', 5, 'Programming'), " +
                        "('Data Structures', 'Jane Doe', 'CS Books', '2022', 3, 'Computer Science'), " +
                        "('Web Development', 'Bob Wilson', 'Web Press', '2023', 4, 'Programming'), " +
                        "('Machine Learning', 'Alice Brown', 'AI Publisher', '2024', 2, 'AI'), " +
                        "('Database Design', 'Mike Johnson', 'Data Books', '2023', 3, 'Database')");

            // Add sample borrow records for AI recommendations
            stmt.execute("INSERT OR IGNORE INTO borrow_records(user_id, book_id, borrow_date, status) VALUES " +
                        "(1, 1, '2024-01-15', 'RETURNED'), " +
                        "(1, 2, '2024-02-01', 'RETURNED'), " +
                        "(1, 3, '2024-02-15', 'ACTIVE')");

            conn.close();
            System.out.println("CSDL đã khởi tạo thành công tại: " + DB_PATH);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
