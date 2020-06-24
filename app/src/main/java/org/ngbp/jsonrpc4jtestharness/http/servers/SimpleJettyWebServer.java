package org.ngbp.jsonrpc4jtestharness.http.servers;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SimpleJettyWebServer implements AutoCloseable {

    public SimpleJettyWebServer(String text) {
        SimpleJettyWebServer.text = text;
    }

    private Server server;
    private static String text;

    public void startup() throws Exception {
        server = new Server(8080);

        ServletContextHandler handler = new ServletContextHandler(server, "/example");
        handler.addServlet(JsonRpcTestServlet.class, "/");

        server.start();
    }

    @Override
    public void close() throws Exception {
        this.stop();
    }

    public void stop() throws Exception {
        server.stop();
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public static class JsonRpcTestServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {

            PrintWriter out = response.getWriter();
            out.println(text);
        }
    }
}
