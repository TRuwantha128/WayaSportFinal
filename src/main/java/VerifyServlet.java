import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/VerifyServlet")
public class VerifyServlet extends HttpServlet {

    // DB credentials (same as RegisterServlet)
    private static final String DB_URL = "jdbc:mysql://localhost:3306/wayasport";
    private static final String DB_USER = "root";
    private static final String DB_PASS = System.getenv("DB_PASS") != null ? System.getenv("DB_PASS") : "asdf4444";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String token = request.getParameter("token");
        if (token == null || token.isEmpty()) {
            out.println("<html><body><h2>❌ Invalid verification link.</h2><p><a href='Student.html'>Back to Login</a></p></body></html>");
            out.close();
            return;
        }

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            // Check token validity
            String checkSql = "SELECT email, is_verified, token_expires_at FROM users WHERE verification_token = ?";
            ps = con.prepareStatement(checkSql);
            ps.setString(1, token);
            rs = ps.executeQuery();

            if (rs.next()) {
                Timestamp expiresAt = rs.getTimestamp("token_expires_at");
                boolean isVerified = rs.getBoolean("is_verified");

                if (isVerified) {
                    out.println("<html><body><h2>✅ Account already verified!</h2><p><a href='Student.html'>Login Now</a></p></body></html>");
                } else if (expiresAt != null && expiresAt.after(new Timestamp(System.currentTimeMillis()))) {
                    // Valid: Update user
                    String updateSql = "UPDATE users SET is_verified = true, verified_at = NOW(), verification_token = NULL WHERE verification_token = ?";
                    ps = con.prepareStatement(updateSql);
                    ps.setString(1, token);
                    ps.executeUpdate();

                    out.println("<html><body><h2>✅ Email verified successfully!</h2><p>You can now log in.<br><a href='Student.html'>Go to Login</a></p></body></html>");
                } else {
                    out.println("<html><body><h2>❌ Verification link expired or invalid.</h2><p><a href='SignUp.html'>Register Again</a></p></body></html>");
                }
            } else {
                out.println("<html><body><h2>❌ Invalid verification link.</h2><p><a href='Student.html'>Back to Login</a></p></body></html>");
            }

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            out.println("<html><body><h2>❌ Server Error: " + e.getMessage() + "</h2><p><a href='Student.html'>Back to Login</a></p></body></html>");
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (con != null) con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            out.close();
        }
    }
}