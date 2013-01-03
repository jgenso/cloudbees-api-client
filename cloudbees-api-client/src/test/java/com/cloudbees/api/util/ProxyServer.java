/*
 * Copyright 2010-2012, CloudBees Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudbees.api.util;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.security.B64Code;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.proxy.AsyncProxyServlet;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * @author Olivier Lamy
 */
public class ProxyServer {
    private Server proxyServer;

    /**
     * @param proxyServlet the wanted auth proxy servlet
     */
    public ProxyServer(AuthAsyncProxyServlet proxyServlet) {
        this("localhost", 0, proxyServlet);
    }

    /**
     * @param hostName     the server name
     * @param port         the server port
     * @param proxyServlet the wanted auth proxy servlet
     */
    public ProxyServer(String hostName, int port, AuthAsyncProxyServlet proxyServlet) {
        proxyServer = new Server();

        proxyServer.addConnector(getDefaultConnector(hostName, port));

        Context context = new Context(proxyServer, "/", 0);

        context.addServlet(new ServletHolder(proxyServlet), "/");

    }

    /**
     * @return the host name
     */
    public String getHostName() {
        Connector connector = proxyServer.getConnectors()[0];
        return connector.getHost();
    }

    /**
     * @return the host port
     */
    public int getPort() {
        Connector connector = proxyServer.getConnectors()[0];
        return (connector.getLocalPort() <= 0 ? connector.getPort() : connector.getLocalPort());
    }

    public void start() throws Exception {
        proxyServer.start();
    }

    public void stop() throws Exception {
        proxyServer.stop();
    }

    private Connector getDefaultConnector(String hostName, int port) {
        Connector connector = new SocketConnector();
        if (hostName != null) {
            connector.setHost(hostName);
        } else {
            try {
                connector.setHost(InetAddress.getLocalHost().getCanonicalHostName());
            } catch (UnknownHostException e) {
                // nop
            }
        }
        if (port > 0) {
            connector.setPort(port);
        }

        return connector;
    }

    public static class AuthAsyncProxyServlet
            extends AsyncProxyServlet {
        private Map<String, String> authentications;

        private long sleepTime = 0;

        public long requestsReceived = 0;

        /**
         * Constructor for non authentication servlet.
         */
        public AuthAsyncProxyServlet() {
            super();
        }

        public AuthAsyncProxyServlet(Map<String, String> authentications) {
            this();

            this.authentications = authentications;
        }

        public AuthAsyncProxyServlet(Map<String, String> authentications, long sleepTime) {
            this();

            this.authentications = authentications;
            this.sleepTime = sleepTime;
        }

        public void service(ServletRequest req, ServletResponse res)
                throws ServletException, IOException {
            final HttpServletRequest request = (HttpServletRequest) req;
            final HttpServletResponse response = (HttpServletResponse) res;
            requestsReceived++;
            if (this.authentications != null && !this.authentications.isEmpty()) {
                String proxyAuthorization = request.getHeader("Proxy-Authorization");
                if (proxyAuthorization != null && proxyAuthorization.startsWith("Basic ")) {
                    String proxyAuth = proxyAuthorization.substring(6);
                    String authorization = B64Code.decode(proxyAuth);
                    String[] authTokens = authorization.split(":");
                    String user = authTokens[0];
                    String password = authTokens[1];

                    if (this.authentications.get(user) == null) {
                        throw new IllegalArgumentException(user + " not found in the map!");
                    }

                    if (sleepTime > 0) {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            // nop
                        }
                    }
                    String authPass = this.authentications.get(user).toString();
                    if (password.equals(authPass)) {
                        // could throw exceptions...
                        super.service(req, res);
                        return;
                    }
                }

                // Proxy-Authenticate Basic realm="CCProxy Authorization"
                response.addHeader("Proxy-Authenticate", "Basic realm=\"Jetty Proxy Authorization\"");
                response.setStatus(HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED);
                return;
            }

            super.service(req, res);
        }
    }
}
