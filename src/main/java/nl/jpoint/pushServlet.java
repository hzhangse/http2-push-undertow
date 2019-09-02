package nl.jpoint;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.protocol.http2.Http2ServerConnection;
import io.undertow.servlet.spec.HttpServletRequestImpl;
import io.undertow.util.Methods;

public class pushServlet extends HttpServlet {

	// Mapped to https://localhost:8443/hello-world/api/again
	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {

		HttpServletRequestImpl r = (HttpServletRequestImpl) req;

		HttpServerExchange exchange = r.getExchange();

		Http2ServerConnection serverConnection = (Http2ServerConnection) exchange.getConnection();

		class MyThread implements Runnable {
			HttpServletRequest req;

			MyThread(HttpServletRequest req) {
				this.req = req;
			}

			@Override
			public void run() {
				boolean pushed = true;

				while (pushed) {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					boolean result = serverConnection.pushResource("/hello-world/api/message", Methods.GET,
							exchange.getRequestHeaders());
					if (req.getHeader("ack") != null || !result) {
						pushed = false;
					}

				}
				System.out.println("exit thread after ack");
			}

		}

		Runnable t = new MyThread(req);
		new Thread(t).start();

		try {
			Thread.currentThread().join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}