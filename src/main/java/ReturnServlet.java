import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/ReturnServlet")
public class ReturnServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Handle GET from View page (orderId for return request)
        response.setContentType("text/html;charset=UTF-8");
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userIndex") == null) {
            response.getWriter().println("<script>alert('❌ Please log in first'); window.location='Student.html';</script>");
            return;
        }
        String userIndex = (String) session.getAttribute("userIndex");

        String orderIdStr = request.getParameter("orderId");
        if (orderIdStr == null || orderIdStr.isEmpty()) {
            response.getWriter().println("<script>alert('❌ Invalid return request'); window.location='ViewOrdersServlet';</script>");
            return;
        }

        int orderId = Integer.parseInt(orderIdStr);
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/wayasport", "root", "asdf4444");

            // Check order is approved (not already returned/pending)
            String checkSql = "SELECT item_name, quantity FROM user_orders WHERE order_id = ? AND user_index = ? AND status = 'approved'";
            ps = con.prepareStatement(checkSql);
            ps.setInt(1, orderId);
            ps.setString(2, userIndex);
            rs = ps.executeQuery();

            if (rs.next()) {
                String itemName = rs.getString("item_name");
                int qty = rs.getInt("quantity");

                // NEW: Update to pending_return (no stock change yet)
                String updateSql = "UPDATE user_orders SET status = 'pending_return' WHERE order_id = ? AND user_index = ?";
                PreparedStatement psUpdate = con.prepareStatement(updateSql);
                psUpdate.setInt(1, orderId);
                psUpdate.setString(2, userIndex);
                int updateRows = psUpdate.executeUpdate();

                if (updateRows > 0) {
                    response.getWriter().println(
                        "<script>alert('✅ Return request for " + itemName + " (" + qty + " qty) submitted! Awaiting staff approval.'); window.location='ViewOrdersServlet';</script>"
                    );
                } else {
                    response.getWriter().println(
                        "<script>alert('❌ Return request failed.'); window.location='ViewOrdersServlet';</script>"
                    );
                }
            } else {
                response.getWriter().println(
                    "<script>alert('❌ Order not found or already returned.'); window.location='ViewOrdersServlet';</script>"
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().println(
                "<script>alert('DB ERROR: " + e.getMessage() + "'); window.location='ViewOrdersServlet';</script>"
            );
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (con != null) con.close();
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Existing POST logic for Give.html form (keep as-is or integrate pending_return if needed)
        // ... (your original code here)
    }
}