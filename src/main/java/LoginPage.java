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
import org.mindrot.jbcrypt.BCrypt;

@WebServlet("/LoginPage")
public class LoginPage extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/wayasport";
    private static final String DB_USER = "root";
    private static final String DB_PASS = System.getenv("DB_PASS") != null ? System.getenv("DB_PASS") : "asdf4444";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String index = request.getParameter("index") != null ? request.getParameter("index").trim() : "";
        String password = request.getParameter("password") != null ? request.getParameter("password").trim() : "";

        System.out.println("[LOGIN] === NEW ATTEMPT ===");
        System.out.println("[LOGIN] Raw index: '" + index + "' (length=" + index.length() + ")");
        System.out.println("[LOGIN] Password length: " + password.length() + " (first 3 chars: '" + (password.length() > 0 ? password.substring(0, Math.min(3, password.length())) + "***" : "EMPTY") + "')");

        // Validation
        if (index.isEmpty()) {
            System.out.println("[LOGIN] BLOCK: Empty index");
            response.sendRedirect(request.getContextPath() + "/Student.html?error=invalid_index");
            return;
        }
        if (!index.matches("\\d{6}")) {
            System.out.println("[LOGIN] BLOCK: Invalid format (not 6 digits): '" + index + "'");
            response.sendRedirect(request.getContextPath() + "/Student.html?error=invalid_index");
            return;
        }
        if (password.isEmpty()) {
            System.out.println("[LOGIN] BLOCK: Empty password");
            response.sendRedirect(request.getContextPath() + "/Student.html?error=empty_password");
            return;
        }

        System.out.println("[LOGIN] Passed validation. Querying DB for index: " + index);

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            String checkSql = "SELECT password_hash, full_name, is_verified FROM users WHERE index_num = ?";
            ps = con.prepareStatement(checkSql);
            ps.setString(1, index);
            rs = ps.executeQuery();

            if (rs.next()) {
                String hashedPassword = rs.getString("password_hash");
                String fullName = rs.getString("full_name");
                boolean isVerified = rs.getBoolean("is_verified");
                System.out.println("[LOGIN] User FOUND: " + fullName + ", verified=" + isVerified + ", hash starts with: " + hashedPassword.substring(0, 20) + "...");

                if (!isVerified) {
                    System.out.println("[LOGIN] BLOCK: Not verified");
                    response.sendRedirect(request.getContextPath() + "/Student.html?error=unverified");
                    return;
                }

                boolean pwMatch = BCrypt.checkpw(password, hashedPassword);
                System.out.println("[LOGIN] BCrypt check: " + pwMatch + " (plain len=" + password.length() + ")");

                if (pwMatch) {
                    HttpSession session = request.getSession();
                    session.setAttribute("userIndex", index);
                    session.setAttribute("userName", fullName);
                    System.out.println("[LOGIN] SUCCESS: Redirecting " + fullName + " to BorrowOrGive.html");
                    response.sendRedirect(request.getContextPath() + "/BorrowOrGive.html");
                    return;
                } else {
                    System.out.println("[LOGIN] BLOCK: Password mismatch");
                    response.sendRedirect(request.getContextPath() + "/Student.html?error=incorrect");
                    return;
                }
            } else {
                System.out.println("[LOGIN] BLOCK: No user with index " + index);
                response.sendRedirect(request.getContextPath() + "/Student.html?error=not_found");
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[LOGIN] ERROR: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/Student.html?error=server");
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (con != null) con.close();
            } catch (SQLException ignored) {}
            out.close();
        }
        System.out.println("[LOGIN] === END ===");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        System.out.println("[LOGIN] GET request - redirecting to BorrowOrGive");
        response.sendRedirect(request.getContextPath() + "/BorrowOrGive.html");
    }
}