package nl.jpoint;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class pushedMessage extends HttpServlet {
	private static List<String> msgLst = new ArrayList<>();
	{
		for (int i = 0; i < 1000; i++) {
			msgLst.add("message--" + i);

		}
	}
	static AtomicInteger ack = new AtomicInteger(0);

	static Map<String, Integer> checkPoint = new ConcurrentHashMap<>();

	public void handleAck(final HttpServletRequest req) {
		String clientid = req.getHeader("clientid");
		if (req.getHeader("ack") != null) {
			int ack = Integer.valueOf(req.getHeader("ack"));
			checkPoint.put(clientid, ack);
		}
	}

	// Mapped to https://localhost:8443/hello-world/api/again
	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {

		PrintWriter writer = resp.getWriter();
		int batchsize = Integer.valueOf(req.getHeader("batchsize"));
		resp.addHeader("clientid", req.getHeader("clientid"));
		handleAck(req);
		
		String clientid =req.getHeader("clientid");

		if (checkPoint.get(clientid) == null) {
			checkPoint.put(clientid, 0);
		}			

		int idx = checkPoint.get(clientid);

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < batchsize; i++) {
			sb.append(msgLst.get(idx + i) + "\n");
		}
		
		int acked = idx+batchsize ;
		resp.addHeader("acked", String.valueOf(acked));
		checkPoint.put(clientid,acked );
		writer.write("This was server3 pushed @\n " + sb+"--" + Instant.now().toString() + "\n");
		writer.close();
		

	}

}