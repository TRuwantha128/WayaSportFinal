import java.io.IOException;
import java.io.PrintWriter;
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

@WebServlet("/StaffAddItemServlet")
public class StaffAddItemServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/wayasport?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "asdf4444";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Security: Only staff can add items
        HttpSession session = request.getSession(false);
        if (session == null || !"staff".equals(session.getAttribute("role"))) {
            response.sendRedirect("StaffLogin.html");
            return;
        }

        // 2. Get form data
        String itemName = request.getParameter("itemName");
        String quantityStr = request.getParameter("quantity");

        if (itemName == null || itemName.trim().isEmpty()) {
            redirectWithMessage(response, "Item name is required!", false);
            return;
        }

        int quantity = 0;
        try {
            quantity = Integer.parseInt(quantityStr.trim());
            if (quantity < 0) {
                redirectWithMessage(response, "Quantity cannot be negative!", false);
                return;
            }
        } catch (NumberFormatException e) {
            redirectWithMessage(response, "Invalid quantity value!", false);
            return;
        }

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            // 3. Insert new item
            String sql = 
                "INSERT INTO itemissued (ItemName, Quantity, last_updated, last_updated_by) " +
                "VALUES (?, ?, NOW(), 'staff')";

            ps = conn.prepareStatement(sql);
            ps.setString(1, itemName.trim());
            ps.setInt(2, quantity);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                String successMsg = "New item added successfully: " + 
                                  itemName.trim() + " (Quantity: " + quantity + ")";
                redirectWithMessage(response, successMsg, true);
            } else {
                redirectWithMessage(response, "Failed to add item - no rows affected", false);
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            redirectWithMessage(response, "Database driver not found", false);
        } catch (SQLException e) {
            e.printStackTrace();
            String errorMsg = "Database error: " + e.getMessage();
            if (errorMsg.contains("Duplicate")) {
                errorMsg = "Item with this name already exists!";
            }
            redirectWithMessage(response, errorMsg, false);
        } finally {
            try {
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException ignored) {}
        }
    }

    /**
     * Redirect with success or error message
     */
    private void redirectWithMessage(HttpServletResponse response, String message, boolean isSuccess)
            throws IOException {
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        String param = isSuccess ? "msg" : "error";
        response.sendRedirect(
            response.encodeRedirectURL(
                "StaffDashboardServlet?" + param + "=" + encoded
            )
        );
    }

    // Optional: Allow GET for testing (shows message)
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<h2>Staff Add Item Servlet</h2>");
        out.println("<p>Use POST method from the form to add new items.</p>");
        out.println("<p><a href='StaffDashboardServlet'>Back to Dashboard</a></p>");
        out.close();
    }
}