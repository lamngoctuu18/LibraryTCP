package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class UpdateDatabase {
    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db")) {
            Statement stmt = conn.createStatement();

            try {
                stmt.execute("ALTER TABLE books ADD COLUMN description TEXT DEFAULT ''");
                System.out.println("Added description column to books table.");
            } catch (Exception e) {
                if (e.getMessage().contains("duplicate column name")) {
                    System.out.println("Description column already exists.");
                } else {
                    System.out.println("Error adding description column: " + e.getMessage());
                }
            }

            String[] updates = {
                "UPDATE books SET description = 'Một tác phẩm kinh điển của văn học thế giới, khám phá những vấn đề sâu sắc về cuộc sống và con người.' WHERE title LIKE '%Dế mèn%'",
                "UPDATE books SET description = 'Cuốn sách khoa học công nghệ hấp dẫn, giúp độc giả hiểu rõ hơn về những tiến bộ mới nhất trong lĩnh vực này.' WHERE category = 'Khoa học – Công nghệ'",
                "UPDATE books SET description = 'Tài liệu quý giá trong lĩnh vực kinh tế và quản trị, phù hợp cho sinh viên và những người làm việc trong ngành.' WHERE category = 'Kinh tế – Quản trị'",
                "UPDATE books SET description = 'Cuốn sách phát triển kỹ năng sống và tâm lý, giúp bạn tự tin và thành công hơn trong cuộc sống.' WHERE category = 'Tâm lý – Kỹ năng sống'",
                "UPDATE books SET description = 'Giáo trình học thuật chất lượng cao, được biên soạn bởi các giảng viên có kinh nghiệm.' WHERE category = 'Giáo trình – Học thuật'"
            };

            for (String update : updates) {
                try {
                    int rowsUpdated = stmt.executeUpdate(update);
                    System.out.println("Updated " + rowsUpdated + " rows with description.");
                } catch (Exception e) {
                    System.out.println("Error updating description: " + e.getMessage());
                }
            }

            System.out.println("Database update completed successfully!");

        } catch (Exception e) {
            System.out.println("Database connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
