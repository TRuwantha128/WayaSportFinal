import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

@WebServlet("/RegisterServlet")
public class RegisterServlet extends HttpServlet {

    // DB creds (secure in prod!)
    private static final String DB_URL = "jdbc:mysql://localhost:3306/wayasport";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "asdf4444";  // Use env vars!

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        String index = request.getParameter("Index").trim();
        String email = request.getParameter("email").trim().toLowerCase();
        String fullName = request.getParameter("fullName").trim();
        String password = request.getParameter("password").trim();
        String confirmPassword = request.getParameter("confirmPassword").trim();

        // Validation
        if (!index.matches("\\d{6}")) {
            response.getWriter().println("<script>alert('❌ Index must be 6 digits'); window.location='SignUp.html';</script>");
            return;
        }
        if (!email.contains("@") || !fullName.matches("^[a-zA-Z\\s]+$")) {
            response.getWriter().println("<script>alert('❌ Invalid email or name'); window.location='SignUp.html';</script>");
            return;
        }
        if (!password.equals(confirmPassword) || password.length() < 6) {
            response.getWriter().println("<script>alert('❌ Passwords don't match or too short'); window.location='SignUp.html';</script>");
            return;
        }

        // Check if user exists
        Connection con = null;
        PreparedStatement ps = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            ps = con.prepareStatement("SELECT index_num FROM users WHERE index_num = ? OR email = ?");
            ps.setString(1, index);
            ps.setString(2, email);
            if (ps.executeQuery().next()) {
                response.getWriter().println("<script>alert('❌ User already exists'); window.location='SignUp.html';</script>");
                return;
            }

            // Hash password
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            // Insert user (auto-verified, no token needed)
            ps = con.prepareStatement("INSERT INTO users (index_num, email, full_name, password_hash, is_verified) VALUES (?, ?, ?, ?, true)");
            ps.setString(1, index);
            ps.setString(2, email);
            ps.setString(3, fullName);
            ps.setString(4, hashedPassword);
            ps.executeUpdate();

            response.getWriter().println("<script>alert('✅ Registered successfully! You can log in now.'); window.location='Student.html';</script>");

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().println("<script>alert('❌ Registration failed: " + e.getMessage() + "'); window.location='SignUp.html';</script>");
        } finally {
            try { if (ps != null) ps.close(); if (con != null) con.close(); } catch (Exception ignored) {}
        }
    }
}