import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/GiveServlet")
public class GiveServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/wayasport";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "asdf4444";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession(false);

        // Check login
        if (session == null || session.getAttribute("userIndex") == null) {
            response.sendRedirect("Student.html");
            return;
        }
        String userIndex = (String) session.getAttribute("userIndex");

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            // FIXED: Query only active orders for this user (status = 'approved' instead of 'ordered')
            String sql = "SELECT order_id, item_name, quantity, order_date FROM user_orders WHERE user_index = ? AND status = 'approved' ORDER BY order_date DESC";
            ps = con.prepareStatement(sql);
            ps.setString(1, userIndex);
            rs = ps.executeQuery();

            out.println("<!DOCTYPE html>");
            out.println("<html lang='en'>");
            out.println("<head>");
            out.println("    <title>Waya Sport - Return Items</title>");
            out.println("    <link rel='stylesheet' href='Give.css'>");  // Reuse your CSS
            out.println("    <style>");
            out.println("        table { width:80%; margin:20px auto; border-collapse:collapse; }");
            out.println("        th, td { border:1px solid #000; padding:12px; text-align:left; }");
            out.println("        th { background:#4169E1; color:white; }");
            out.println("        .return-btn { background:red; color:white; padding:8px 12px; text-decoration:none; border-radius:5px; font-size:14px; }");
            out.println("        .return-btn:hover { background:black; }");
            out.println("        .no-items { text-align:center; margin:40px; font-size:18px; }");
            out.println("    </style>");
            out.println("</head>");
            out.println("<body>");
            out.println("<div class='main'>");
            out.println("    <div class='navigationBar'>");
            out.println("        <div class='icon'><h1 class='logo'>Return Items</h1></div>");
        
            out.println("    </div>");

            out.println("    <div style='text-align:center; margin:20px;'>");
            out.println("        <a href='ItemListServlet' style='background:#ff7200; color:white; padding:10px 20px; text-decoration:none; border-radius:5px; font-size:18px;'>← Back to Lend</a>");
            out.println("        <a href='ViewOrdersServlet' style='background:#4a0da5; color:white; padding:10px 20px; text-decoration:none; border-radius:5px; font-size:18px; margin-left:10px;'>📋 View All Orders</a>");
            out.println("    </div>");

            out.println("    <h2 style='text-align:center;'>Your Borrowed Items Ready to Return (Index: " + userIndex + ")</h2>");
            if (rs.next()) {
                out.println("    <table>");
                out.println("        <tr><th>Order Date</th><th>Item Name</th><th>Quantity</th><th>Action</th></tr>");
                do {
                    int orderId = rs.getInt("order_id");
                    String itemName = rs.getString("item_name");
                    int qty = rs.getInt("quantity");
                    String date = rs.getTimestamp("order_date").toString();

                    out.println("        <tr>");
                    out.println("            <td>" + date + "</td>");
                    out.println("            <td>" + itemName + "</td>");
                    out.println("            <td>" + qty + "</td>");
                    out.println("            <td><a href='ReturnServlet?orderId=" + orderId + "' class='return-btn'>Return This Item</a></td>");  // Links to your ReturnServlet GET
                    out.println("        </tr>");
                } while (rs.next());
                out.println("    </table>");
                out.println("    <p style='text-align:center; margin:20px;'>* Returning deletes the record and adds stock back.</p>");
            } else {
                out.println("    <div class='no-items'>");
                out.println("        <p>No borrowed items to return yet. All good!</p>");
                out.println("        <a href='ItemListServlet' style='background:#ff7200; color:white; padding:10px 20px; text-decoration:none; border-radius:5px;'>➕ Borrow Something Now</a>");
                out.println("    </div>");
            }

            out.println("</div></body></html>");

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            out.println("<html><body><h2>❌ Database Error: " + e.getMessage() + "</h2><p><a href='BorrowOrGive.html'>Back</a></p></body></html>");
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (con != null) con.close();
            } catch (SQLException ignored) {}
            out.close();
        }
    }
}