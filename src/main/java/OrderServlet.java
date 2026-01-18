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

@WebServlet("/OrderServlet")
public class OrderServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        HttpSession session = request.getSession(false);

        System.out.println("=== [OrderServlet] START ===");
        System.out.println("[OrderServlet] DEBUG: Session exists? " + (session != null));

        // Check login
        if (session == null || session.getAttribute("userIndex") == null) {
            System.out.println("[OrderServlet] ERROR: No session/userIndex");
            response.getWriter().println(
                "<script>alert('❌ Please log in first'); window.location='Student.html';</script>"
            );
            return;
        }
        String userIndex = (String) session.getAttribute("userIndex");
        System.out.println("[OrderServlet] DEBUG: UserIndex = '" + userIndex + "'");

        // HTML form values
        int itemId = Integer.parseInt(request.getParameter("item"));
        int orderQty = Integer.parseInt(request.getParameter("quantity"));

        System.out.println("[OrderServlet] DEBUG: Ordering ItemID=" + itemId + ", Qty=" + orderQty);

        if (orderQty <= 0) {
            response.getWriter().println(
                "<script>alert('❌ Invalid order quantity (must be >0)'); window.location='ItemListServlet';</script>"
            );
            return;
        }

        Connection con = null;
        PreparedStatement ps = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/wayasport",
                "root",
                "asdf4444"
            );
            System.out.println("[OrderServlet] DEBUG: DB Connected successfully");

            // Check availability
            String checkSql = "SELECT ItemName, Quantity FROM itemissued WHERE ItemId=?";
            ps = con.prepareStatement(checkSql);
            ps.setInt(1, itemId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String itemName = rs.getString("ItemName").trim();
                int availableQty = rs.getInt("Quantity");
                System.out.println("[OrderServlet] DEBUG: Item '" + itemName + "' has stock " + availableQty);

                if (availableQty >= orderQty) {

                    // NEW: Submit to pending_orders (no stock deduct yet)
                    String pendingSql = "INSERT INTO pending_orders (user_index, item_id, item_name, quantity, status) VALUES (?, ?, ?, ?, 'pending')";
                    PreparedStatement psPending = con.prepareStatement(pendingSql);
                    psPending.setString(1, userIndex.trim());
                    psPending.setInt(2, itemId);
                    psPending.setString(3, itemName);
                    psPending.setInt(4, orderQty);
                    int rowsInserted = psPending.executeUpdate();
                    System.out.println("[OrderServlet] DEBUG: Pending order submitted, rows inserted: " + rowsInserted);

                    if (rowsInserted > 0) {
                        response.getWriter().println(
                            "<script>alert('✅ Order for " + itemName + " (Qty: " + orderQty + ") submitted! Awaiting staff approval.'); window.location='ItemListServlet';</script>"
                        );
                    } else {
                        response.getWriter().println(
                            "<script>alert('❌ Failed to submit order. Try again.'); window.location='ItemListServlet';</script>"
                        );
                    }

                } else {
                    System.out.println("[OrderServlet] ERROR: Low stock for '" + itemName + "'");
                    response.getWriter().println(
                        "<script>alert('❌ Only " + availableQty + " " + itemName + " available.'); window.location='ItemListServlet';</script>"
                    );
                }

            } else {
                System.out.println("[OrderServlet] ERROR: Item ID " + itemId + " not found");
                response.getWriter().println(
                    "<script>alert('❌ Item not found'); window.location='ItemListServlet';</script>"
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[OrderServlet] CRITICAL ERROR: " + e.getMessage());
            response.getWriter().println(
                "<script>alert('DB ERROR: " + e.getMessage() + "'); window.location='ItemListServlet';</script>"
            );
        } finally {
            try { 
                if (ps != null) ps.close(); 
                if (con != null) con.close(); 
            } catch (Exception ignored) {
                System.out.println("[OrderServlet] Finally block error: " + ignored.getMessage());
            }
        }
        System.out.println("=== [OrderServlet] END ===");
    }
}