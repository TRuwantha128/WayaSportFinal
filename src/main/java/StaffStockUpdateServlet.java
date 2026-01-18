import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/StaffStockUpdateServlet")
public class StaffStockUpdateServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/wayasport?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "asdf4444";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Staff authentication check
        HttpSession session = request.getSession(false);
        if (session == null || !"staff".equals(session.getAttribute("role"))) {
            response.sendRedirect("StaffLogin.html");
            return;
        }

        // 2. Get required parameters
        String itemIdStr = request.getParameter("itemId");
        String deltaStr = request.getParameter("delta");     // e.g. +5, -1, +10
        String action = request.getParameter("action");      // "adjust" or "damage"

        if (itemIdStr == null || itemIdStr.trim().isEmpty()) {
            redirectWithMessage(response, "Invalid item ID", false);
            return;
        }

        int itemId;
        try {
            itemId = Integer.parseInt(itemIdStr.trim());
        } catch (NumberFormatException e) {
            redirectWithMessage(response, "Invalid item ID format", false);
            return;
        }

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            String message;
            boolean success = false;

            if ("adjust".equals(action) && deltaStr != null && !deltaStr.trim().isEmpty()) {
                // ── QUANTITY ADJUSTMENT (+ or -) ───────────────────────────────
                int delta;
                try {
                    delta = Integer.parseInt(deltaStr.trim());
                } catch (NumberFormatException e) {
                    redirectWithMessage(response, "Invalid quantity change value", false);
                    return;
                }

                if (delta == 0) {
                    redirectWithMessage(response, "No change requested", true);
                    return;
                }

                String sql = 
                    "UPDATE itemissued " +
                    "SET Quantity = GREATEST(Quantity + ?, 0), " +  // prevent negative stock
                    "    last_updated = NOW(), " +
                    "    last_updated_by = 'staff' " +
                    "WHERE ItemId = ?";

                ps = conn.prepareStatement(sql);
                ps.setInt(1, delta);
                ps.setInt(2, itemId);

                int rowsAffected = ps.executeUpdate();

                if (rowsAffected > 0) {
                    message = "Stock updated successfully! " + 
                             (delta > 0 ? "+" : "") + delta + " item(s)";
                    success = true;
                } else {
                    message = "Item not found or could not be updated";
                }

            } else if ("damage".equals(action)) {
                // ── DAMAGE REPORT (decrease by 1 + mark) ───────────────────────
                String sql = 
                    "UPDATE itemissued " +
                    "SET Quantity = GREATEST(Quantity - 1, 0), " +
                    "    last_updated = NOW(), " +
                    "    last_updated_by = 'staff (damaged)' " +
                    "WHERE ItemId = ?";

                ps = conn.prepareStatement(sql);
                ps.setInt(1, itemId);

                int rowsAffected = ps.executeUpdate();

                if (rowsAffected > 0) {
                    message = "1 item marked as damaged (stock decreased by 1)";
                    success = true;
                } else {
                    message = "Item not found or already at 0 stock";
                }

            } else {
                message = "Invalid operation requested";
            }

            redirectWithMessage(response, message, success);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            redirectWithMessage(response, "Database driver not found", false);
        } catch (SQLException e) {
            e.printStackTrace();
            redirectWithMessage(response, "Database error: " + e.getMessage(), false);
        } finally {
            try {
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException ignored) {
            }
        }
    }

    /**
     * Helper method to redirect with success/error message
     */
    private void redirectWithMessage(HttpServletResponse response, String msg, boolean isSuccess)
            throws IOException {
        String encodedMsg = URLEncoder.encode(msg, StandardCharsets.UTF_8);
        String param = isSuccess ? "msg" : "error";
        response.sendRedirect("StaffInventoryServlet?" + param + "=" + encodedMsg);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Optional: If you later want to support POST (e.g. custom quantity form)
        doGet(request, response);
    }
}