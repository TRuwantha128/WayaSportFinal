import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/StaffInventoryServlet")
public class StaffInventoryServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/wayasport";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "asdf4444";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        // Staff auth check (same as dashboard)
        HttpSession session = request.getSession(false);
        if (session == null || !"staff".equals(session.getAttribute("role"))) {
            response.sendRedirect("StaffLogin.html");
            return;
        }

        String message = request.getParameter("msg");

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            String sql = "SELECT ItemId, ItemName, Quantity, "
                       + "DATE_FORMAT(last_updated,'%Y-%m-%d %H:%i') AS last_upd, "
                       + "last_updated_by FROM itemissued ORDER BY ItemName";
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();

            // ---------------- HTML START ----------------
            out.println("<!DOCTYPE html><html lang='en'><head>");
            out.println("<title>Inventory Management - Waya Sport</title>");
            out.println("<link rel='stylesheet' href='ItemSelect.css'>");
            out.println("<style>");
            out.println("table {width:95%; margin:15px auto; border-collapse:collapse;}");
            out.println("th,td {border:1px solid #444; padding:10px; text-align:center;}");
            out.println("th {background:#ff7200; color:white;}");
            out.println(".action-btn {padding:6px 12px; margin:3px; border-radius:4px; color:white; text-decoration:none; font-size:13px;}");
            out.println(".plus {background:#16a34a;} .minus {background:#dc2626;} .damage {background:#7c3aed;}");
            out.println(".msg {padding:12px; margin:15px auto; width:90%; border-radius:6px; text-align:center;}");
            out.println(".success {background:#d1fae5; color:#065f46;} .error {background:#fee2e2; color:#991b1b;}");
            out.println("@media print { .no-print {display:none !important;} body {font-size:12px;} }");
            out.println("</style></head><body>");

            out.println("<div style='max-width:1400px; margin:auto; padding:20px;'>");
            out.println("<h1 style='text-align:center; color:#4169E1;'>Sport Items Inventory Management</h1>");

            if (message != null) {
                String cls = message.contains("uccess") ? "success" : "error";
                out.println("<div class='msg " + cls + "'>" + message + "</div>");
            }

            // Print & Add New Item buttons
            out.println("<div class='no-print' style='text-align:center; margin:20px 0;'>");
            out.println("<a href='javascript:window.print()' class='action-btn' style='background:#10b981;'>🖨️ Print Stock Report</a>");
            out.println("<a href='StaffAddItem.html' class='action-btn' style='background:#8b5cf6;'>➕ Add New Item</a>");
            out.println("</div>");

            out.println("<table>");
            out.println("<tr><th>ID</th><th>Item Name</th><th>Current Stock</th><th>Last Updated</th><th>Updated By</th><th class='no-print'>Actions</th></tr>");

            boolean hasItems = false;
            while (rs.next()) {
                hasItems = true;
                int id = rs.getInt("ItemId");
                String name = rs.getString("ItemName");
                int qty = rs.getInt("Quantity");
                String updated = rs.getString("last_upd");
                String by = rs.getString("last_updated_by");

                out.println("<tr>");
                out.println("<td>" + id + "</td>");
                out.println("<td>" + name + "</td>");
                out.println("<td style='font-weight:bold; font-size:1.2em;'>" + qty + "</td>");
                out.println("<td>" + (updated != null ? updated : "-") + "</td>");
                out.println("<td>" + by + "</td>");
                out.println("<td class='no-print'>");

                // Quick buttons
                out.println("<a href='StaffStockUpdateServlet?itemId=" + id + "&delta=-1&action=adjust' class='action-btn minus'>-1</a>");
                out.println("<a href='StaffStockUpdateServlet?itemId=" + id + "&delta=+5&action=adjust' class='action-btn plus'>+5</a>");
                out.println("<a href='StaffStockUpdateServlet?itemId=" + id + "&delta=+10&action=adjust' class='action-btn plus'>+10</a>");
                out.println("<a href='StaffStockUpdateServlet?itemId=" + id + "&action=damage' class='action-btn damage'>Damaged</a>");

                out.println("</td></tr>");
            }

            if (!hasItems) {
                out.println("<tr><td colspan='6' style='padding:30px;'>No items in inventory yet...</td></tr>");
            }

            out.println("</table>");
            out.println("</div></body></html>");

        } catch (Exception e) {
            e.printStackTrace();
            out.println("<h2 style='color:red; text-align:center;'>Error: " + e.getMessage() + "</h2>");
        } finally {
            try { if (rs != null) rs.close(); if (ps != null) ps.close(); if (con != null) con.close(); } catch (Exception ignored) {}
            out.close();
        }
    }
}