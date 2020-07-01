package org.ngbp.jsonrpc4jtestharness.core.ws;

import android.content.Context;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.ngbp.jsonrpc4jtestharness.http.servers.JsonRpcTestServlet;


public class SimplyJettyWSServer
{
    public static class EchoSocketHandler extends WebSocketHandler {
        @Override
        public void configure(WebSocketServletFactory factory)
        {
            factory.register(EchoSocket.class);
        }
    }

    private Server server;

    public SimplyJettyWSServer(Context context, int port) {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

//        Start websocket with servlet
        ServletContextHandler servletHandler = new ServletContextHandler(server, "/");
        JsonRpcTestServlet servlet = new JsonRpcTestServlet(context);
        ServletHolder holder = new ServletHolder(servlet);
        servletHandler.addServlet(holder, "/github");

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] {new EchoSocketHandler(), servletHandler});

        server.setHandler(handlers);
    }

    public void runWSServer() throws Exception {
        server.start();
        server.join();
    }
}
