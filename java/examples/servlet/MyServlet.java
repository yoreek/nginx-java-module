import org.nginx.servlet.*;
import org.nginx.Constants;
import javax.servlet.ServletOutputStream;
import java.io.IOException;

public class MyServlet extends BaseHttpServlet {
    public int service(ServletRequest request, ServletResponse response) throws IOException {
        BufferedWriter writer = response.getWriter();
        writer.println("It's work2!");
        response.finishResponse();
        return NGX_DONE;
    }
}
