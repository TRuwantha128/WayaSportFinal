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

@WebServlet("/StaffApproveServlet")
public class StaffApproveServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/wayasport?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "asdf4444";
    private static final String DASHBOARD_URL = "StaffDashboardServlet";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        int pendingId = Integer.parseInt(request.getParameter("pendingId"));
        String action = request.getParameter("action");
        String reason = request.getParameter("reason") != null ? request.getParameter("reason") : "No reason provided";
        String type = request.getParameter("type") != null ? request.getParameter("type") : "order";

        System.out.println("[StaffApprove] DEBUG: START - pendingId=" + pendingId + ", action=" + action + ", type=" + type + ", reason=" + reason);

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String alertMsg = "Unknown error.";
        String userIndex = null;
        int itemId = 0;
        String itemName = null;
        int qty = 0;
        boolean success = false;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            con.setAutoCommit(false);  // NEW: Enable transactions for rollback safety
            System.out.println("[StaffApprove] DEBUG: DB Connected with transactions");

            // FIXED: Fetch based on type
            if ("order".equals(type)) {
                String fetchSql = "SELECT user_index, item_id, item_name, quantity FROM pending_orders WHERE pending_id = ?";
                ps = con.prepareStatement(fetchSql);
                ps.setInt(1, pendingId);
                rs = ps.executeQuery();
                System.out.println("[StaffApprove] DEBUG: Fetch query executed for ORDER");

                if (rs.next()) {
                    userIndex = rs.getString("user_index");
                    itemId = rs.getInt("item_id");
                    itemName = rs.getString("item_name");
                    qty = rs.getInt("quantity");
                    System.out.println("[StaffApprove] DEBUG: Fetched ORDER - User: " + userIndex + ", Item: " + itemName + ", Qty: " + qty + ", ItemId: " + itemId);
                }
            } else if ("return".equals(type)) {
                String fetchSql = "SELECT user_index, item_id, item_name, quantity FROM user_orders WHERE order_id = ?";
                ps = con.prepareStatement(fetchSql);
                ps.setInt(1, pendingId);
                rs = ps.executeQuery();
                System.out.println("[StaffApprove] DEBUG: Fetch query executed for RETURN");

                if (rs.next()) {
                    userIndex = rs.getString("user_index");
                    itemId = rs.getInt("item_id");
                    itemName = rs.getString("item_name");
                    qty = rs.getInt("quantity");
                    System.out.println("[StaffApprove] DEBUG: Fetched RETURN - User: " + userIndex + ", Item: " + itemName + ", Qty: " + qty + ", ItemId: " + itemId);
                }
            }

            if (userIndex == null) {
                alertMsg = "❌ Pending ID " + pendingId + " not found for type '" + type + "'.";
                System.out.println("[StaffApprove] ERROR: Not found - " + alertMsg);
                out.println("<script>alert('" + alertMsg + "'); window.location='" + DASHBOARD_URL + "';</script>");
                return;
            }

            if ("approve".equals(action)) {
                if ("order".equals(type)) {
                    // Order approval (unchanged, but with transaction)
                    String stockSql = "SELECT Quantity FROM itemissued WHERE ItemId = ?";
                    PreparedStatement psStockCheck = con.prepareStatement(stockSql);
                    psStockCheck.setInt(1, itemId);
                    ResultSet stockRs = psStockCheck.executeQuery();
                    if (stockRs.next()) {
                        int available = stockRs.getInt("Quantity");
                        System.out.println("[StaffApprove] DEBUG: Stock check - available=" + available + ", needed=" + qty);
                        if (available < qty) {
                            alertMsg = "❌ Insufficient stock (" + available + " < " + qty + ") for approval.";
                            throw new SQLException(alertMsg);  // NEW: Throw to trigger rollback
                        }
                        System.out.println("[StaffApprove] DEBUG: Stock OK");
                    } else {
                        alertMsg = "❌ Item stock not found for ItemId=" + itemId;
                        throw new SQLException(alertMsg);
                    }

                    // Insert to user_orders
                    String insertUserSql = "INSERT INTO user_orders (user_index, item_id, item_name, quantity, status) VALUES (?, ?, ?, ?, 'approved')";
                    PreparedStatement psInsert = con.prepareStatement(insertUserSql);
                    psInsert.setString(1, userIndex);
                    psInsert.setInt(2, itemId);
                    psInsert.setString(3, itemName);
                    psInsert.setInt(4, qty);
                    int insertRows = psInsert.executeUpdate();
                    System.out.println("[StaffApprove] DEBUG: Inserted to user_orders: " + insertRows + " rows");

                    if (insertRows > 0) {
                        // Deduct stock
                        String updateStock = "UPDATE itemissued SET Quantity = Quantity - ? WHERE ItemId = ?";
                        PreparedStatement psStock = con.prepareStatement(updateStock);
                        psStock.setInt(1, qty);
                        psStock.setInt(2, itemId);
                        int stockRows = psStock.executeUpdate();
                        System.out.println("[StaffApprove] DEBUG: Stock deducted: " + stockRows + " rows");

                        // Delete pending
                        String deletePending = "DELETE FROM pending_orders WHERE pending_id = ?";
                        PreparedStatement psDelete = con.prepareStatement(deletePending);
                        psDelete.setInt(1, pendingId);
                        int deleteRows = psDelete.executeUpdate();
                        System.out.println("[StaffApprove] DEBUG: Deleted pending: " + deleteRows + " rows");

                        con.commit();  // NEW: Commit transaction
                        success = true;
                        alertMsg = "✅ Order approved for " + userIndex + " (" + itemName + ", Qty: " + qty + ")! Stock updated.";
                    } else {
                        throw new SQLException("Insert failed (0 rows)");
                    }

                } else if ("return".equals(type)) {
                    // Return approval with granular error handling
                    try {
                        // Step 1: Add stock back
                        String updateStock = "UPDATE itemissued SET Quantity = Quantity + ? WHERE ItemId = ?";
                        PreparedStatement psStock = con.prepareStatement(updateStock);
                        psStock.setInt(1, qty);
                        psStock.setInt(2, itemId);
                        int stockRows = psStock.executeUpdate();
                        System.out.println("[StaffApprove] DEBUG: Stock added back: " + stockRows + " rows");
                        if (stockRows == 0) throw new SQLException("No stock updated (wrong ItemId?)");

                        // Step 2: Set status to 'returned'
                        String updateStatus = "UPDATE user_orders SET status = 'returned' WHERE order_id = ?";
                        PreparedStatement psStatus = con.prepareStatement(updateStatus);
                        psStatus.setInt(1, pendingId);
                        int statusRows = psStatus.executeUpdate();
                        System.out.println("[StaffApprove] DEBUG: Status set to returned: " + statusRows + " rows");
                        if (statusRows == 0) throw new SQLException("No status updated (wrong order_id?)");

                        // Step 3: DELETE row (with extra log)
                        String deleteOrder = "DELETE FROM user_orders WHERE order_id = ?";
                        PreparedStatement psDelete = con.prepareStatement(deleteOrder);
                        psDelete.setInt(1, pendingId);
                        int deleteRows = psDelete.executeUpdate();
                        System.out.println("[StaffApprove] DEBUG: Deleted user_orders row: " + deleteRows + " rows (SQL: " + deleteOrder.replace("?", String.valueOf(pendingId)) + ")");
                        if (deleteRows == 0) {
                            // NEW: Extra check - maybe row doesn't exist?
                            String verifySql = "SELECT COUNT(*) FROM user_orders WHERE order_id = ?";
                            PreparedStatement psVerify = con.prepareStatement(verifySql);
                            psVerify.setInt(1, pendingId);
                            ResultSet verifyRs = psVerify.executeQuery();
                            verifyRs.next();
                            int remaining = verifyRs.getInt(1);
                            throw new SQLException("DELETE failed (0 rows) - Row still exists? Count: " + remaining);
                        }

                        con.commit();  // NEW: Commit on full success
                        success = true;
                        alertMsg = "✅ Return approved for " + userIndex + " (" + itemName + ", Qty: " + qty + ")! Stock restored. Row deleted.";
                    } catch (SQLException stepE) {
                        con.rollback();  // NEW: Rollback on any step failure
                        System.out.println("[StaffApprove] RETURN STEP ERROR: " + stepE.getMessage());
                        alertMsg = "❌ Return approval failed at step (stock/status/delete): " + stepE.getMessage() + ". Check logs.";
                    }
                }
            } else if ("reject".equals(action)) {
                // Reject logic (with transaction for safety)
                if ("order".equals(type)) {
                    String updatePending = "UPDATE pending_orders SET status = 'rejected', staff_notes = ? WHERE pending_id = ?";
                    PreparedStatement psUpdate = con.prepareStatement(updatePending);
                    psUpdate.setString(1, reason);
                    psUpdate.setInt(2, pendingId);
                    int updateRows = psUpdate.executeUpdate();
                    con.commit();
                    alertMsg = "❌ Order rejected for " + userIndex + " (" + itemName + ") - Reason: " + reason;
                    System.out.println("[StaffApprove] DEBUG: Rejected order, rows updated: " + updateRows);
                } else if ("return".equals(type)) {
                    String updateStatus = "UPDATE user_orders SET status = 'approved' WHERE order_id = ?";
                    PreparedStatement psUpdate = con.prepareStatement(updateStatus);
                    psUpdate.setInt(1, pendingId);
                    int updateRows = psUpdate.executeUpdate();
                    con.commit();
                    if (updateRows > 0) {
                        alertMsg = "❌ Return rejected for " + userIndex + " (" + itemName + ") - Reason: " + reason + ". Status back to approved.";
                        System.out.println("[StaffApprove] DEBUG: Return rejected, status reverted: " + updateRows + " rows");
                    } else {
                        alertMsg = "❌ Failed to reject return (0 rows updated).";
                    }
                }
                success = true;  // Rejects always "succeed"
            }

            if (success) {
                System.out.println("[StaffApprove] APPROVE/REJECT SUCCESS: " + alertMsg);
            }

        } catch (Exception e) {
            if (con != null) {
                try { con.rollback(); System.out.println("[StaffApprove] ROLLBACK: Due to " + e.getMessage()); } catch (SQLException rbE) { System.out.println("[StaffApprove] Rollback error: " + rbE.getMessage()); }
            }
            e.printStackTrace();
            alertMsg = "❌ Critical error: " + e.getMessage();
            System.out.println("[StaffApprove] CRITICAL ERROR: " + alertMsg + " | Stack: " + e.toString());
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (con != null) {
                    con.setAutoCommit(true);  // NEW: Reset auto-commit
                    con.close();
                }
            } catch (Exception ignored) {
                System.out.println("[StaffApprove] Finally error: " + ignored.getMessage());
            }
        }

        // Always alert + JS redirect
        out.println("<script>alert('" + alertMsg.replace("'", "\\'") + "'); window.location='" + DASHBOARD_URL + "';</script>");
        out.flush();
        out.close();
    }
}