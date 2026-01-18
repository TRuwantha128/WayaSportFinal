import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/LogoutServlet")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // Get current session (if exists)
        HttpSession session = request.getSession(false);
        
        // Invalidate/destroy the session if it exists
        if (session != null) {
            session.invalidate();
        }

        // Redirect to student login page with success message
        response.sendRedirect("Student.html?logout=success");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Just in case someone calls it with POST
        doGet(request, response);
    }
}