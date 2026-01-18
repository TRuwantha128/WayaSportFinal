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
import java.text.SimpleDateFormat;
import java.util.Date;

@WebServlet("/StaffDashboardServlet")
public class StaffDashboardServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/wayasport?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "asdf4444";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || !"staff".equals(session.getAttribute("role"))) {
            response.sendRedirect("StaffLogin.html");
            return;
        }

        String msg = request.getParameter("msg");
        String error = request.getParameter("error");

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        ResultSet activeRs = null;
        ResultSet stockRs = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            // ── 1. Pending Approvals ──────────────────────────────────────────
            String pendingSql = "SELECT pending_id, user_index, item_name, quantity, order_date, 'order' AS type " +
                               "FROM pending_orders WHERE status = 'pending' " +
                               "UNION " +
                               "SELECT order_id AS pending_id, user_index, item_name, quantity, order_date, 'return' AS type " +
                               "FROM user_orders WHERE status = 'pending_return' " +
                               "ORDER BY order_date DESC";
            ps = con.prepareStatement(pendingSql);
            rs = ps.executeQuery();

            // ── 2. Active Borrows ──────────────────────────────────────────────
            String activeSql = "SELECT u.index_num AS user_index, u.full_name AS user_name, " +
                              "uo.item_name, uo.quantity, uo.order_date " +
                              "FROM user_orders uo " +
                              "JOIN users u ON u.index_num = uo.user_index " +
                              "WHERE uo.status = 'approved' " +
                              "ORDER BY uo.order_date ASC";
            PreparedStatement activePs = con.prepareStatement(activeSql);
            activeRs = activePs.executeQuery();

            // ── 3. Current Stock / Inventory ───────────────────────────────────
            String stockSql = "SELECT ItemId, ItemName, Quantity, " +
                             "DATE_FORMAT(last_updated, '%Y-%m-%d %H:%i') AS last_updated_fmt " +
                             "FROM itemissued ORDER BY ItemName";
            PreparedStatement stockPs = con.prepareStatement(stockSql);
            stockRs = stockPs.executeQuery();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String reportTimestamp = sdf.format(new Date());

            // ── HTML OUTPUT ────────────────────────────────────────────────────
            out.println("<!DOCTYPE html>");
            out.println("<html lang='en'>");
            out.println("<head>");
            out.println(" <title>Staff Dashboard - Waya Sport</title>");
            out.println(" <link rel='stylesheet' href='ItemSelect.css'>");
            out.println(" <link rel='preconnect' href='https://fonts.googleapis.com'>");
            out.println(" <link rel='preconnect' href='https://fonts.gstatic.com' crossorigin>");
            out.println(" <link href='https://fonts.googleapis.com/css2?family=Inter:wght@700;800&display=swap' rel='stylesheet'>");
            out.println(" <style>");
            // Override the external CSS background for dashboard
            out.println("  body { ");
            out.println("    font-family: Arial, sans-serif; ");
            out.println("    margin: 0; ");
            out.println("    padding: 0;");
            out.println("    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;");
            out.println("    min-height: 100vh;");
            out.println("  }");
            out.println("  .main {");
            out.println("    width: 100%;");
            out.println("    background: none !important;"); // Override ItemSelect.css
            out.println("    min-height: 100vh;");
            out.println("    padding-bottom: 30px;");
            out.println("  }");
            out.println("  .dashboard-title {");
            out.println("    font-family: 'Inter', sans-serif;");
            out.println("    font-size: 2.6rem;");
            out.println("    font-weight: 800;");
            out.println("    background: linear-gradient(90deg, #ffffff, #f0f0f0);");
            out.println("    -webkit-background-clip: text;");
            out.println("    -webkit-text-fill-color: transparent;");
            out.println("    letter-spacing: -1px;");
            out.println("    text-shadow: 0 2px 12px rgba(0,0,0,0.3);");
            out.println("    text-align: center;");
            out.println("    margin: 0;");
            out.println("    padding: 1.5rem 0;");
            out.println("  }");
            out.println("  .section { ");
            out.println("    margin: 1.2rem auto; ");
            out.println("    width: 95%; ");
            out.println("    max-width: 1400px; ");
            out.println("    border: 1px solid rgba(255,255,255,0.2); ");
            out.println("    border-radius: 12px; ");
            out.println("    padding: 12px; ");
            out.println("    background: rgba(255, 255, 255, 0.95); ");
            out.println("    box-shadow: 0 8px 16px rgba(0,0,0,0.15);");
            out.println("  }");
            out.println("  .section h2 { ");
            out.println("    background: linear-gradient(135deg, #4169ff 0%, #5b7bff 100%); ");
            out.println("    color: white; ");
            out.println("    padding: 12px; ");
            out.println("    margin: -12px -12px 8px -12px; ");
            out.println("    border-radius: 10px 10px 0 0; ");
            out.println("    cursor: pointer; ");
            out.println("    font-size: 1.2rem;");
            out.println("    box-shadow: 0 2px 4px rgba(0,0,0,0.1);");
            out.println("  }");
            out.println("  .section-content { display: none; } ");
            out.println("  .section-content.active { display: block; }");
            out.println("  table { ");
            out.println("    width: 100%; ");
            out.println("    border-collapse: collapse; ");
            out.println("    margin-top: 10px; ");
            out.println("    font-size: 0.95em; ");
            out.println("    background: white; ");
            out.println("    border-radius: 8px;");
            out.println("    overflow: hidden;");
            out.println("  }");
            out.println("  th, td { border: 1px solid #ddd; padding: 10px; text-align: center; }");
            out.println("  th { ");
            out.println("    background: linear-gradient(135deg, #ff7200 0%, #ff8c2e 100%); ");
            out.println("    color: white; ");
            out.println("    font-weight: 600;");
            out.println("  }");
            out.println("  tr:hover { background-color: #f8f9fa; }");
            out.println("  .approve-btn { background: #16a34a; color: white; padding: 6px 12px; margin: 3px; border-radius: 4px; text-decoration: none; display: inline-block; transition: 0.3s; }");
            out.println("  .approve-btn:hover { background: #15803d; transform: translateY(-1px); box-shadow: 0 2px 4px rgba(0,0,0,0.2); }");
            out.println("  .reject-btn { background: #dc2626; color: white; padding: 6px 12px; margin: 3px; border-radius: 4px; text-decoration: none; display: inline-block; transition: 0.3s; }");
            out.println("  .reject-btn:hover { background: #b91c1c; transform: translateY(-1px); box-shadow: 0 2px 4px rgba(0,0,0,0.2); }");
            out.println("  .stock-plus { background: #16a34a; color: white; padding: 5px 10px; margin: 2px; border-radius: 4px; text-decoration: none; font-size: 0.9em; display: inline-block; transition: 0.3s; }");
            out.println("  .stock-plus:hover { background: #15803d; transform: translateY(-1px); }");
            out.println("  .stock-minus { background: #dc2626; color: white; padding: 5px 10px; margin: 2px; border-radius: 4px; text-decoration: none; font-size: 0.9em; display: inline-block; transition: 0.3s; }");
            out.println("  .stock-minus:hover { background: #b91c1c; transform: translateY(-1px); }");
            out.println("  .add-item-btn { background: #7c3aed; color: white; padding: 10px 18px; border-radius: 6px; text-decoration: none; margin: 10px; display: inline-block; transition: 0.3s; }");
            out.println("  .add-item-btn:hover { background: #6d28d9; transform: translateY(-2px); box-shadow: 0 4px 8px rgba(0,0,0,0.2); }");
            out.println("  .print-btn { background: #10b981; color: white; padding: 10px 18px; border-radius: 6px; text-decoration: none; cursor: pointer; display: inline-block; transition: 0.3s; }");
            out.println("  .print-btn:hover { background: #059669; transform: translateY(-2px); box-shadow: 0 4px 8px rgba(0,0,0,0.2); }");
            out.println("  .msg { padding: 12px; margin: 15px auto; width: 90%; max-width: 800px; border-radius: 8px; text-align: center; font-weight: bold; }");
            out.println("  .success { background: #d1fae5; color: #065f46; border-left: 4px solid #10b981; }");
            out.println("  .error { background: #fee2e2; color: #991b1b; border-left: 4px solid #ef4444; }");
            out.println("  .logout-section { text-align: center; margin: 30px 0; }");
            out.println("  .logout-btn { background: #dc2626; color: white; padding: 12px 30px; border-radius: 8px; text-decoration: none; display: inline-block; font-weight: 600; transition: 0.3s; }");
            out.println("  .logout-btn:hover { background: #b91c1c; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(220, 38, 38, 0.4); }");
            out.println("  @media print { ");
            out.println("    .no-print { display: none !important; } ");
            out.println("    body { margin: 0; font-size: 11px; background: white !important; } ");
            out.println("    .main { background: white !important; }");
            out.println("  }");
            out.println(" </style>");
            out.println(" <script>");
            out.println(" function toggleSection(id) {");
            out.println("  document.getElementById(id).classList.toggle('active');");
            out.println(" }");

            out.println(" function printCurrentStock() {");
            out.println("  var printWin = window.open('', '_blank');");
            out.println("  var printContent = '<html><head><title>Waya Sport - Current Stock Report - ' + new Date().toLocaleString() + '</title>';");
            out.println("  printContent += '<style>body{font-family:Arial,sans-serif;margin:20px;}table{width:100%;border-collapse:collapse;}th,td{border:1px solid #000;padding:10px;text-align:center;}th{background:#ff7200;color:white;}</style>';");
            out.println("  printContent += '<h2 style=\"text-align:center;\">Waya Sport - Current Inventory Stock Report</h2>';");
            out.println("  printContent += '<p style=\"text-align:center;font-style:italic;\">Generated on " + reportTimestamp + "</p>';");
            out.println("  var table = document.querySelector('#inventoryContent table');");
            out.println("  var cleanTable = '<table><tr><th>ID</th><th>Item Name</th><th>Current Stock</th><th>Last Updated</th></tr>';");
            out.println("  var rows = table.querySelectorAll('tr');");
            out.println("  for (var i = 1; i < rows.length; i++) {");
            out.println("    var cells = rows[i].querySelectorAll('td');");
            out.println("    if (cells.length >= 4) {");
            out.println("      cleanTable += '<tr>';");
            out.println("      cleanTable += '<td>' + cells[0].innerText + '</td>';");
            out.println("      cleanTable += '<td>' + cells[1].innerText + '</td>';");
            out.println("      cleanTable += '<td>' + cells[2].innerText + '</td>';");
            out.println("      cleanTable += '<td>' + cells[3].innerText + '</td>';");
            out.println("      cleanTable += '</tr>';");
            out.println("    }");
            out.println("  }");
            out.println("  cleanTable += '</table>';");
            out.println("  printContent += cleanTable;");
            out.println("  printContent += '</body></html>';");
            out.println("  printWin.document.write(printContent);");
            out.println("  printWin.document.close();");
            out.println("  printWin.print();");
            out.println(" }");

            out.println(" function printActiveBorrows() {");
            out.println("  var win = window.open('', '_blank');");
            out.println("  var html = '<html><head><title>Active Borrows Report - ' + new Date().toLocaleString() + '</title>';");
            out.println("  html += '<style>table{width:100%;border-collapse:collapse;}th,td{border:1px solid #000;padding:8px;text-align:left;}th{background:#ff7200;color:white;}</style>';");
            out.println("  html += '<h2 style=\"text-align:center;\">Waya Sport - Active Borrows (' + '" + reportTimestamp + "' + ')</h2>';");
            out.println("  html += document.getElementById('activeTable').outerHTML;");
            out.println("  html += '</body></html>';");
            out.println("  win.document.write(html); win.document.close(); win.print();");
            out.println(" }");
            out.println(" </script>");
            out.println("</head>");
            out.println("<body>");
            out.println("<div class='main'>");

            out.println(" <h1 class='dashboard-title'>Staff Dashboard</h1>");

            if (msg != null && !msg.trim().isEmpty()) {
                out.println("<div class='msg success'>" + msg + "</div>");
            }
            if (error != null && !error.trim().isEmpty()) {
                out.println("<div class='msg error'>" + error + "</div>");
            }

            // ── Pending Approvals ──────────────────────────────────────────────
            out.println(" <div class='section'>");
            out.println(" <h2 onclick=\"toggleSection('pendingContent')\">📋 Pending Approvals</h2>");
            out.println(" <div id='pendingContent' class='section-content active'>");
            if (rs.next()) {
                out.println(" <table>");
                out.println(" <tr><th>User Index</th><th>Item</th><th>Qty</th><th>Date</th><th>Type</th><th>Actions</th></tr>");
                do {
                    int pid = rs.getInt("pending_id");
                    String uid = rs.getString("user_index").trim();
                    String iname = rs.getString("item_name");
                    int qty = rs.getInt("quantity");
                    String date = rs.getTimestamp("order_date").toString();
                    String type = rs.getString("type");
                    out.println(" <tr>");
                    out.println(" <td>" + uid + "</td>");
                    out.println(" <td>" + iname + "</td>");
                    out.println(" <td>" + qty + "</td>");
                    out.println(" <td>" + date + "</td>");
                    out.println(" <td>" + (type.equals("order") ? "New Order" : "Return Request") + "</td>");
                    out.println(" <td>");
                    out.println(" <a href='StaffApproveServlet?pendingId=" + pid + "&action=approve&type=" + type + "' class='approve-btn'>Approve</a>");
                    out.println(" <a href='StaffApproveServlet?pendingId=" + pid + "&action=reject&reason=Not+approved&type=" + type + "' class='reject-btn'>Reject</a>");
                    out.println(" </td>");
                    out.println(" </tr>");
                } while (rs.next());
                out.println(" </table>");
            } else {
                out.println(" <p style='text-align:center;padding:20px;'>No pending approvals.</p>");
            }
            out.println(" </div>");
            out.println(" </div>");

            // ── Active Borrows ──────────────────────────────────────────────────
            out.println(" <div class='section'>");
            out.println(" <h2 onclick=\"toggleSection('activeContent')\">📖 Active Borrows</h2>");
            out.println(" <div id='activeContent' class='section-content'>");
            out.println(" <div style='text-align:center; margin:15px 0;' class='no-print'>");
            out.println(" <a href='javascript:printActiveBorrows()' class='print-btn'>🖨️ Print Active Borrows Report</a>");
            out.println(" </div>");
            if (activeRs.next()) {
                out.println(" <table id='activeTable'>");
                out.println(" <tr><th>User Index</th><th>User Name</th><th>Item</th><th>Qty</th><th>Lend Date</th></tr>");
                do {
                    out.println(" <tr>");
                    out.println(" <td>" + activeRs.getString("user_index").trim() + "</td>");
                    out.println(" <td>" + (activeRs.getString("user_name") != null ? activeRs.getString("user_name") : "Unknown") + "</td>");
                    out.println(" <td>" + activeRs.getString("item_name") + "</td>");
                    out.println(" <td>" + activeRs.getInt("quantity") + "</td>");
                    out.println(" <td>" + activeRs.getTimestamp("order_date") + "</td>");
                    out.println(" </tr>");
                } while (activeRs.next());
                out.println(" </table>");
            } else {
                out.println(" <p style='text-align:center;padding:20px;'>No active borrows.</p>");
            }
            out.println(" </div>");
            out.println(" </div>");

            // ── INVENTORY MANAGEMENT SECTION ────────────────────────────────────
            out.println(" <div class='section'>");
            out.println(" <h2 onclick=\"toggleSection('inventoryContent')\">🏟️ Current Inventory (Stock Management)</h2>");
            out.println(" <div id='inventoryContent' class='section-content'>");
            out.println(" <div style='text-align:center; margin:15px 0;' class='no-print'>");
            out.println(" <a href='StaffAddItem.html' class='add-item-btn'>➕ Add New Sport Item</a>");
            out.println(" <a href='javascript:printCurrentStock()' class='print-btn'>🖨️ Print Current Stock</a>");
            out.println(" </div>");
            if (stockRs.next()) {
                out.println(" <table>");
                out.println(" <tr>");
                out.println(" <th>ID</th>");
                out.println(" <th>Item Name</th>");
                out.println(" <th>Current Stock</th>");
                out.println(" <th>Last Updated</th>");
                out.println(" <th class='no-print'>Quick Actions</th>");
                out.println(" </tr>");
                do {
                    int id = stockRs.getInt("ItemId");
                    String name = stockRs.getString("ItemName");
                    int qty = stockRs.getInt("Quantity");
                    String updated = stockRs.getString("last_updated_fmt");

                    out.println(" <tr>");
                    out.println(" <td>" + id + "</td>");
                    out.println(" <td style='text-align:left;'>" + name + "</td>");
                    out.println(" <td style='font-weight:bold; font-size:1.2em;'>" + qty + "</td>");
                    out.println(" <td>" + (updated != null ? updated : "-") + "</td>");
                    out.println(" <td class='no-print'>");
                    out.println(" <a href='StaffStockUpdateServlet?itemId=" + id + "&delta=+1&action=adjust' class='stock-plus'>+1</a>");
                    out.println(" <a href='StaffStockUpdateServlet?itemId=" + id + "&delta=+5&action=adjust' class='stock-plus'>+5</a>");
                    out.println(" <a href='StaffStockUpdateServlet?itemId=" + id + "&delta=-1&action=adjust' class='stock-minus'>-1</a>");
                    out.println(" <a href='StaffStockUpdateServlet?itemId=" + id + "&action=damage' class='stock-minus'>Damaged -1</a>");
                    out.println(" </td>");
                    out.println(" </tr>");
                } while (stockRs.next());
                out.println(" </table>");
            } else {
                out.println(" <p style='text-align:center;padding:30px;'>No items in inventory yet.</p>");
            }
            out.println(" </div>");
            out.println(" </div>");

            out.println(" <div class='logout-section'>");
            out.println(" <a href='StaffLogin.html?logout=true' class='logout-btn'>Logout</a>");
            out.println(" </div>");

            out.println("</div></body></html>");

        } catch (Exception e) {
            e.printStackTrace();
            out.println("<html><body><h2 style='color:red;text-align:center;'>Error: " + e.getMessage() + "</h2>");
            out.println("<p><a href='StaffLogin.html'>Back to login</a></p></body></html>");
        } finally {
            try {
                if (stockRs != null) stockRs.close();
                if (activeRs != null) activeRs.close();
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (con != null) con.close();
            } catch (SQLException ignored) {}
            out.close();
        }
    }
}