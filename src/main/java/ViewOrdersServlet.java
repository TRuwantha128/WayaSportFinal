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

@WebServlet("/ViewOrdersServlet")
public class ViewOrdersServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/wayasport";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "asdf4444";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession(false);

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

            // Query all statuses (approved, pending_return, returned, etc.)
            String sql = "SELECT order_id, item_name, quantity, order_date, status FROM user_orders WHERE user_index = ? ORDER BY order_date DESC";
            ps = con.prepareStatement(sql);
            ps.setString(1, userIndex);
            rs = ps.executeQuery();

            out.println("<!DOCTYPE html>");
            out.println("<html lang='en'>");
            out.println("<head>");
            out.println("    <title>Waya Sport - Ordered Items</title>");
            out.println("    <link rel='stylesheet' href='ItemSelect.css'>");
            out.println("    <style>");
            out.println("        table { width:80%; margin:20px auto; border-collapse:collapse; }");
            out.println("        th, td { border:1px solid #000; padding:12px; text-align:left; }");
            out.println("        th { background:#4169E1; color:white; }");
            out.println("        .action-btn { background:#4169E1; color:white; padding:8px 12px; text-decoration:none; border-radius:5px; font-size:14px; }");
            out.println("        .action-btn:hover { background:#0000FF; }");
            out.println("        .return-btn { background:red; color:white; }"); 
            out.println("        .return-btn:hover { background:#000; }");
            out.println("        .pending-row { background-color: #fff3cd; }");  
            out.println("        .returned-row { background-color: #d4edda; }");  
            out.println("    </style>");
            out.println("</head>");
            out.println("<body>");
            out.println("<div class='main'>");
            out.println("    <div class='navigationBar'>");
            out.println("        <div class='icon'><h1 class='logo'>Ordered Items</h1></div>");
            out.println("        <div class='menu'><ul><li><a href='Home.html'>Home</a></li><li><a href='About.html'>About</a></li><li><a href='Service.html'>Service</a></li><li><a href='Contact.html'>Contact</a></li></ul></div>");
            out.println("    </div>");

            out.println("    <div style='text-align:center; margin:20px;'>");
            out.println("        <a href='ItemListServlet' class='action-btn'>← Back to Order</a>");
            out.println("    </div>");

            out.println("    <h2 style='text-align:center;'>Your Orders (Index: " + userIndex + ")</h2>");
            if (rs.next()) {
                out.println("    <table>");
                out.println("        <tr>");
                out.println("            <th>Order Date</th>");
                out.println("            <th>Item Name</th>");
                out.println("            <th>Quantity</th>");
                out.println("            <th>Status</th>");
                out.println("            <th>Action</th>");  // NEW: Action column
                out.println("        </tr>");
                do {
                    int orderId = rs.getInt("order_id");
                    String itemName = rs.getString("item_name");
                    int qty = rs.getInt("quantity");
                    String date = rs.getTimestamp("order_date").toString();
                    String status = rs.getString("status");

                    // Status display
                    String statusDisplay = "";
                    String rowClass = "";
                    if ("pending_return".equals(status)) {
                        statusDisplay = "Pending Return Approval";
                        rowClass = "pending-row";
                    } else if ("approved".equals(status)) {
                        statusDisplay = "Active (Returnable)";
                    } else if ("returned".equals(status)) {
                        statusDisplay = "Returned";
                        rowClass = "returned-row";
                    } else {
                        statusDisplay = status;
                    }

                    out.println("        <tr class='" + rowClass + "'>");
                    out.println("            <td>" + date + "</td>");
                    out.println("            <td>" + itemName + "</td>");
                    out.println("            <td>" + qty + "</td>");
                    out.println("            <td>" + statusDisplay + "</td>");
                    out.println("            <td>");
                    if ("approved".equals(status)) {
                        out.println("                <a href='ReturnServlet?orderId=" + orderId + "' class='action-btn return-btn'>Return Item</a>");  // NEW: Return button for approved
                    } else if ("pending_return".equals(status)) {
                        out.println("                Awaiting Staff Approval");
                    } else {
                        out.println("                -");
                    }
                    out.println("            </td>");
                    out.println("        </tr>");
                } while (rs.next());
                out.println("    </table>");
            } else {
                out.println("    <p style='text-align:center;'>No orders yet. <a href='ItemListServlet'>Order something!</a></p>");
            }

            out.println("</div></body></html>");

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            out.println("<html><body><h2>❌ Error: " + e.getMessage() + "</h2><a href='ItemListServlet'>Back</a></body></html>");
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