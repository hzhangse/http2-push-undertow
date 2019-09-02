package nl.jpoint;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLContext;

import org.xnio.ChannelListeners;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.ssl.XnioSsl;

import io.undertow.Handlers;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientExchange;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.client.UndertowClient;
import io.undertow.connector.ByteBufferPool;
import io.undertow.io.Receiver;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.Protocols;
import io.undertow.util.StringReadChannelListener;
import io.undertow.util.StringWriteChannelListener;

public class UnderTowHttpClient {
	private static final char[] STORE_PASSWORD = "storepwd".toCharArray();

	public static final AttachmentKey<String> RESPONSE_BODY = AttachmentKey.create(String.class);

	public static final int BUFFER_SIZE = 8192 * 3;
	public static final OptionMap DEFAULT_OPTIONS = OptionMap.builder().set(Options.WORKER_IO_THREADS, 8)
			.set(Options.TCP_NODELAY, true).set(Options.KEEP_ALIVE, true).set(Options.WORKER_NAME, "Client").getMap();
	public static final ByteBufferPool POOL = new DefaultByteBufferPool(true, BUFFER_SIZE, 1000, 10, 100);
	public static final ByteBufferPool SSL_BUFFER_POOL = new DefaultByteBufferPool(true, 17 * 1024);
	public static XnioWorker WORKER;
	public static XnioSsl SSL;
	public static ClientConnection connection;

	public static void main(String[] args) throws Exception {

		CompletableFuture<String> future = new CompletableFuture<>();

		try {

			SSLContext clientSslContext = Http2Server.createSSLContext(Http2Server.loadKeyStore("/kclient.keystore"),
					Http2Server.loadKeyStore("/tclient.keystore"));
			UndertowClient client = UndertowClient.getInstance();
			final Xnio xnio = Xnio.getInstance();
			WORKER = xnio.createWorker(null, DEFAULT_OPTIONS);
			SSL = new UndertowXnioSsl(WORKER.getXnio(), OptionMap.EMPTY, SSL_BUFFER_POOL, clientSslContext);
			connection = client.connect(new URI("https://localhost:8443"), WORKER, SSL, POOL,
					OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();

			
			
			connection.getIoThread().execute(new Runnable() {
				@Override
				public void run() {
					final ClientRequest request = new ClientRequest().setProtocol(Protocols.HTTP_2_0)
							.setMethod(Methods.GET).setPath("/hello-world/api/again");
					request.getRequestHeaders().put(Headers.HOST, "localhost");
					request.getRequestHeaders().put(Headers.ACCEPT, "*/*");
					request.getRequestHeaders().put(new HttpString("clientid"),"hzhangse");
					request.getRequestHeaders().put(new HttpString("batchsize"),"10");
					
					connection.sendRequest(request, new ClientCallback<ClientExchange>() {
						@Override
						public void completed(ClientExchange result) {
							result.setResponseListener(new ClientCallback<ClientExchange>() {
								@Override
								public void completed(ClientExchange result) {
									if (result.getResponse().getResponseCode() != 200) {
										future.completeExceptionally(new RuntimeException(
												"Received status code " + result.getResponse().getResponseCode()));
										return;
									}
									HeaderMap requestHeaders = result.getRequest().getRequestHeaders();
									for (HttpString header : requestHeaders.getHeaderNames()) {
										System.out.printf("\t%s: %s\n", header, requestHeaders.getFirst(header));
									}

									System.out.println("====");

									HeaderMap headerMap = result.getResponse().getResponseHeaders();
									for (HttpString header : headerMap.getHeaderNames()) {
										System.out.printf("\t%s: %s\n", header, headerMap.getFirst(header));
									}

									new StringReadChannelListener(result.getConnection().getBufferPool()) {

										@Override
										protected void stringDone(String string) {
											System.out.println("completed with: " + string);
											future.complete(string);
										}

										@Override
										protected void error(IOException e) {
											future.completeExceptionally(e);
										}
									}.setup(result.getResponseChannel());
								}

								@Override
								public void failed(IOException e) {
									future.completeExceptionally(e);
								}
							});

							try {
								result.getRequestChannel().shutdownWrites();
								if (!result.getRequestChannel().flush()) {
									result.getRequestChannel().getWriteSetter().set(
											ChannelListeners.<StreamSinkChannel>flushingChannelListener(null, null));
									result.getRequestChannel().resumeWrites();
								}
							} catch (IOException e) {
								e.printStackTrace();
								future.completeExceptionally(e);
							}
						}

						@Override
						public void failed(IOException e) {
							future.completeExceptionally(e);
						}
					});
				}

			});
		} catch (URISyntaxException | IOException e) {
			throw new RuntimeException("Failed to create request", e);
		}
		Thread.currentThread().join();
	
	}

	private static HttpHandler getHandler() throws Exception {
		SSLContext clientSslContext = Http2Server.createSSLContext(Http2Server.loadKeyStore("/kclient.keystore"),
				Http2Server.loadKeyStore("/tclient.keystore"));
		UndertowClient client = UndertowClient.getInstance();
		final Xnio xnio = Xnio.getInstance();
		WORKER = xnio.createWorker(null, DEFAULT_OPTIONS);
		SSL = new UndertowXnioSsl(WORKER.getXnio(), OptionMap.EMPTY, SSL_BUFFER_POOL, clientSslContext);
		connection = client.connect(new URI("https://localhost:8443"), WORKER, SSL, POOL,
				OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();

		return Handlers.routing().add(Methods.GET, "/get", exchange -> {
			// call server2 get endpoint with UndertowClient
			final CountDownLatch latch = new CountDownLatch(1);
			final AtomicReference<ClientResponse> reference = new AtomicReference<>();
			try {
				ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath("/get");
				connection.sendRequest(request, createClientCallback(reference, latch));
				latch.await();
				int statusCode = reference.get().getResponseCode();
				String body = reference.get().getAttachment(RESPONSE_BODY);
				if (statusCode == 200) {
					exchange.getResponseSender().send(body);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).add(Methods.POST, "/post",
				exchange -> exchange.getRequestReceiver().receiveFullString(new Receiver.FullStringCallback() {
					@Override
					public void handle(HttpServerExchange exchange, String message) {
						// call server2 post endpoint with UndertowClient
						exchange.getResponseSender().send(message + " World");
					}
				}));
	}

	public static ClientCallback<ClientExchange> createClientCallback(final AtomicReference<ClientResponse> reference,
			final CountDownLatch latch) {
		return new ClientCallback<ClientExchange>() {
			@Override
			public void completed(ClientExchange result) {
				result.setResponseListener(new ClientCallback<ClientExchange>() {
					@Override
					public void completed(final ClientExchange result) {
						reference.set(result.getResponse());
						new StringReadChannelListener(result.getConnection().getBufferPool()) {

							@Override
							protected void stringDone(String string) {
								result.getResponse().putAttachment(RESPONSE_BODY, string);
								latch.countDown();
							}

							@Override
							protected void error(IOException e) {
								e.printStackTrace();

								latch.countDown();
							}
						}.setup(result.getResponseChannel());
					}

					@Override
					public void failed(IOException e) {
						e.printStackTrace();

						latch.countDown();
					}
				});
				try {
					result.getRequestChannel().shutdownWrites();
					if (!result.getRequestChannel().flush()) {
						result.getRequestChannel().getWriteSetter()
								.set(ChannelListeners.<StreamSinkChannel>flushingChannelListener(null, null));
						result.getRequestChannel().resumeWrites();
					}
				} catch (IOException e) {
					e.printStackTrace();
					latch.countDown();
				}
			}

			@Override
			public void failed(IOException e) {
				e.printStackTrace();
				latch.countDown();
			}
		};
	}

	public static ClientCallback<ClientExchange> createClientCallback(final AtomicReference<ClientResponse> reference,
			final CountDownLatch latch, final String requestBody) {
		return new ClientCallback<ClientExchange>() {
			@Override
			public void completed(ClientExchange result) {
				new StringWriteChannelListener(requestBody).setup(result.getRequestChannel());
				result.setResponseListener(new ClientCallback<ClientExchange>() {
					@Override
					public void completed(ClientExchange result) {
						reference.set(result.getResponse());
						new StringReadChannelListener(POOL) {
							@Override
							protected void stringDone(String string) {
								result.getResponse().putAttachment(RESPONSE_BODY, string);
								latch.countDown();
							}

							@Override
							protected void error(IOException e) {
								e.printStackTrace();
								latch.countDown();
							}
						}.setup(result.getResponseChannel());
					}

					@Override
					public void failed(IOException e) {
						e.printStackTrace();
						latch.countDown();
					}
				});
			}

			@Override
			public void failed(IOException e) {
				e.printStackTrace();
				latch.countDown();
			}
		};
	}

}