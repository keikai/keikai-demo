import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;


public class JettyLaunch {
	public static void main(String[] args) throws Exception {
		Server server = new Server(8080);

		ServletContextHandler servletContextHandler = new WebAppContext("src/main/webapp", "/java-client-demo");
		server.setHandler(servletContextHandler);
		server.start();
		server.join();
	}
}
