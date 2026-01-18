import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/StaffLoginServlet")
public class StaffLoginServlet extends HttpServlet {

    private static final String STAFF_INDEX = "STAFF001";
    private static final String STAFF_PASS = "staffpass";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String index = request.getParameter("index");
        String pass = request.getParameter("password");

        if (STAFF_INDEX.equals(index) && STAFF_PASS.equals(pass)) {
            HttpSession session = request.getSession(true);
            session.setAttribute("role", "staff");
            response.sendRedirect("StaffDashboardServlet");
        } else {
            response.sendRedirect("StaffLogin.html?error=invalid");
        }
    }
}