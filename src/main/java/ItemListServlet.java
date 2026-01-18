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

@WebServlet("/ItemListServlet")
public class ItemListServlet extends HttpServlet {

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

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            String sql = "SELECT ItemId, ItemName, Quantity FROM itemissued ORDER BY ItemId";
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();

            out.println("<!DOCTYPE html>");
            out.println("<html lang='en'>");
            out.println("<head>");
            out.println("    <title>Waya Sport - Order Items</title>");
            out.println("    <link rel='stylesheet' href='ItemSelect.css'>");
            out.println("    <style>");
            out.println("        .button-group { display: flex; gap: 15px; justify-content: center; flex-wrap: wrap ;font-family:Arial }");  // UPDATED: Flex for two buttons side-by-side
            out.println("        .submit-btn { width: 180px; height: 55px; font-size: 18px; font-weight: bold; background-color: #4169E1; color: white; border: none; border-radius: 8px; cursor: pointer; text-decoration: none; display: inline-flex; align-items: center; justify-content: center; }");  // UPDATED: Style for both buttons
            out.println("        .submit-btn:hover { background-color: #0000FF; }");
            out.println("    </style>");
            out.println("    <script>");
            out.println("        function updateMaxQuantity() {");
            out.println("            const select = document.getElementById('item');");
            out.println("            const qtyInput = document.getElementById('quantity');");
            out.println("            const selectedOption = select.options[select.selectedIndex];");
            out.println("            const stock = selectedOption.dataset.stock || 0;");
            out.println("            qtyInput.max = stock;");
            out.println("            qtyInput.value = Math.min(qtyInput.value, stock);");
            out.println("            if (stock == 0) { qtyInput.value = ''; }");
            out.println("        }");
            out.println("    </script>");
            out.println("</head>");
            out.println("<body>");
            out.println("<div class='main'>");
            out.println("    <div class='navigationBar'>");
            out.println("        <div class='icon'>");
            out.println("            <h1 class='logo'>Order</h1>");
            out.println("        </div>");
            out.println("        <div class='menu'>");
            out.println("            <ul>");

            out.println("        </div>");
            out.println("    </div>");

            // REMOVED: Top "Ordered Items" button (now at bottom for simplicity)

            out.println("    <form action='OrderServlet' method='POST' class='item-form' onsubmit='return validateForm()'>");
            out.println("        <div class='form-row'>");
            out.println("            <label for='item'>Item (Stock Available)</label>");
            out.println("            <select name='item' id='item' required onchange='updateMaxQuantity()'>");
            out.println("                <option value=''>--- Select Item ---</option>");

            boolean hasItems = false;
            while (rs.next()) {
                int itemId = rs.getInt("ItemId");
                String itemName = rs.getString("ItemName");
                int stock = rs.getInt("Quantity");
                hasItems = true;

                out.print("                <option value='" + itemId + "' data-stock='" + stock + "'");
                if (stock == 0) {
                    out.print(" disabled");
                }
                out.println(">" + itemName + " (Stock: " + stock + ")</option>");
            }

            if (!hasItems) {
                out.println("                <option value=''>No items available</option>");
            }

            out.println("            </select>");
            out.println("        </div>");

            out.println("        <div class='form-row'>");
            out.println("            <label for='quantity'>Quantity</label>");
            out.println("            <input type='number' name='quantity' id='quantity' min='1' value='1' required>");
            out.println("        </div>");

            // UPDATED: Two buttons at bottom – Order (submit) + View Ordered Items (link)
            out.println("        <div class='form-row center'>");
            out.println("            <div class='button-group'>");
            out.println("                <button type='submit' class='submit-btn'> Order Item</button>");  // Button 1: Order (submit form)
            out.println("           <a href='ViewOrdersServlet' class='submit-btn'> View Ordered    Items</a>");  // Button 2: Optional link to history
            out.println("            </div>");
            out.println("        </div>");
            out.println("    </form>");

            out.println("</div>");  // Close main
            out.println("<script>");
            out.println("    function validateForm() {");
            out.println("        const select = document.getElementById('item');");
            out.println("        const qtyInput = document.getElementById('quantity');");
            out.println("        const stock = select.options[select.selectedIndex].dataset.stock || 0;");
            out.println("        if (parseInt(qtyInput.value) > stock) {");
            out.println("            alert('Cannot order more than available stock: ' + stock);");
            out.println("            return false;");
            out.println("        }");
            out.println("        return true;");
            out.println("    }");
            out.println("</script>");
            out.println("</body>");
            out.println("</html>");

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