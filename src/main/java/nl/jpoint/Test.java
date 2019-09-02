//package nl.jpoint;
//
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.net.http.HttpResponse.BodyHandler;
//import java.net.http.HttpResponse.BodySubscribers;
//import java.net.http.HttpResponse.PushPromiseHandler;
//import java.nio.charset.Charset;
//import java.util.Objects;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ConcurrentMap;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.Executor;
//import java.util.concurrent.Executors;
//import java.util.function.Function;
//
//public class Test {
//
//	public void testAsyncGet(String path) throws ExecutionException, InterruptedException {
//		HttpClient client = HttpClient.newHttpClient();
//
//		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(path)).version(HttpClient.Version.HTTP_2).build();
//
//		client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete(Test::processResponse);
//		try {
//			// Let the current thread sleep for 5 seconds,
//			// so the async response processing is complete
//			Thread.sleep(5000);
//		} catch (InterruptedException ex) {
//			ex.printStackTrace();
//		}
//	}
//
//	
//
//	
//
//	private static void processResponse(HttpResponse<String> response, Throwable t) {
//		if (t == null) {
//			System.out.println("Response Status Code: " + response.statusCode());
//			System.out.println("Response Body: " + response.body());
//		} else {
//			System.out
//					.println("An exception occurred while " + "processing the HTTP request. Error: " + t.getMessage());
//		}
//	}
//
//	public static void main(String[] args) throws Exception {
//
//		Test t = new Test();
//
//		//t.testAsyncGet1("https://localhost:8443/hello-world/api/hello");
//		 t.testAsyncGet("https://localhost:8443/hello-world/api/again");
//	}
//
//}
