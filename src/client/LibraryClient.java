package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class LibraryClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 12345);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner sc = new Scanner(System.in)) {

            String welcome = in.readLine();
            System.out.println(welcome);

            boolean running = true;
            int myUserId = -1;
            String myRole = "";

            while (running) {
                System.out.println("--- MENU ---");
                System.out.println("1. Đăng ký\n2. Đăng nhập\n3. Tìm sách\n4. Mượn sách\n5. Trả sách\n6. Admin: Thêm sách\n7. Admin: Xóa sách\n8. Admin: Danh sách mượn\n9. Thoát");
                System.out.print("Chọn: ");
                String c = sc.nextLine();
                switch (c) {
                    case "1":
                        System.out.print("Username: ");
                        String ru = sc.nextLine();
                        System.out.print("Password: ");
                        String rp = sc.nextLine();
                        out.println("REGISTER|" + ru + "|" + rp);
                        System.out.println(in.readLine());
                        break;
                    case "2":
                        System.out.print("Username: ");
                        String lu = sc.nextLine();
                        System.out.print("Password: ");
                        String lp = sc.nextLine();
                        out.println("LOGIN|" + lu + "|" + lp);
                        String resp = in.readLine();
                        System.out.println(resp);
                        if (resp.startsWith("LOGIN_SUCCESS")) {
                            String[] p = resp.split("\\|");
                            myUserId = Integer.parseInt(p[1]);
                            myRole = p[2];
                        }
                        break;
                    case "3":
                        System.out.print("Từ khóa tìm kiếm: ");
                        String kw = sc.nextLine();
                        out.println("SEARCH|" + kw);
                        System.out.println(in.readLine());
                        break;
                    case "4":
                        if (myUserId == -1) { System.out.println("Vui lòng đăng nhập trước."); break; }
                        System.out.print("BookId to borrow: ");
                        String bid = sc.nextLine();
                        out.println("BORROW|" + myUserId + "|" + bid);
                        System.out.println(in.readLine());
                        break;
                    case "5":
                        if (myUserId == -1) { System.out.println("Vui lòng đăng nhập trước."); break; }
                        System.out.print("BookId to return: ");
                        String rb = sc.nextLine();
                        out.println("RETURN|" + myUserId + "|" + rb);
                        System.out.println(in.readLine());
                        break;
                    case "6":
                        if (!"admin".equals(myRole)) { System.out.println("Không có quyền admin."); break; }
                        System.out.print("Title: ");
                        String t = sc.nextLine();
                        System.out.print("Author: ");
                        String a = sc.nextLine();
                        System.out.print("Quantity: ");
                        String q = sc.nextLine();
                        out.println("ADD_BOOK|" + t + "|" + a + "|" + q);
                        System.out.println(in.readLine());
                        break;
                    case "7":
                        if (!"admin".equals(myRole)) { System.out.println("Không có quyền admin."); break; }
                        System.out.print("BookId to delete: ");
                        String del = sc.nextLine();
                        out.println("DELETE_BOOK|" + del);
                        System.out.println(in.readLine());
                        break;
                    case "8":
                        if (!"admin".equals(myRole)) { System.out.println("Không có quyền admin."); break; }
                        out.println("LIST_BORROWS");
                        System.out.println(in.readLine());
                        break;
                    case "9":
                        out.println("EXIT");
                        running = false;
                        break;
                    default:
                        System.out.println("Lựa chọn không hợp lệ.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
