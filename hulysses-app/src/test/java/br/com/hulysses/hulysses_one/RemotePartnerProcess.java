package br.com.hulysses.hulysses_one;

import org.springframework.test.context.DynamicPropertyRegistry;
import java.io.File;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/** No service classes are on the principal classpath: integration is exclusively over HTTP. */
final class RemotePartnerProcess {
    private static Process process;
    private static String url;

    static synchronized void configure(DynamicPropertyRegistry registry) {
        if (process == null) start();
        registry.add("services.business-partner.url", () -> url);
    }

    private static void start() {
        try {
            Path service = Path.of("../business-partner-service/target").toAbsolutePath().normalize();
            Path dependencies = service.resolve("test-classpath.txt");
            if (!Files.exists(dependencies)) {
                throw new IllegalStateException("Build the service test fixture first: mvn -pl business-partner-service process-test-classes");
            }
            int port;
            try (ServerSocket socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
            url = "http://localhost:" + port;
            String classpath = service.resolve("classes") + File.pathSeparator + service.resolve("test-classes")
                    + File.pathSeparator + Files.readString(dependencies).strip();
            Path args = Path.of("target/partner-process.args").toAbsolutePath();
            Files.writeString(args, "-cp\n\"" + classpath.replace("\\", "\\\\") + "\"\n"
                    + "br.com.hulysses.business_partner_service.StandaloneTestService\n--server.port=" + port);
            Path java = Path.of(System.getProperty("java.home"), "bin", "java");
            ProcessBuilder builder = new ProcessBuilder(java.toString(), "@" + args);
            if (Boolean.getBoolean("hulysses.test.postgresql")) {
                Files.writeString(args, "-Dhulysses.test.postgresql=true\n" + Files.readString(args));
            }
            process = builder.redirectErrorStream(true).redirectOutput(Path.of("target/partner-process.log").toFile()).start();
            Runtime.getRuntime().addShutdownHook(new Thread(RemotePartnerProcess::stop));
            HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
            long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
            while (System.nanoTime() < deadline && process.isAlive()) {
                try {
                    if (http.send(HttpRequest.newBuilder(URI.create(url + "/business-partners"))
                            .timeout(Duration.ofSeconds(1)).GET().build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 200) return;
                } catch (java.io.IOException ignored) { }
                Thread.sleep(200);
            }
            stop();
            throw new IllegalStateException("Partner service failed to start. See target/partner-process.log");
        } catch (Exception exception) {
            throw new IllegalStateException("Could not start independent partner service for integration tests", exception);
        }
    }

    private static void stop() {
        if (process != null) {
            process.destroy();
            try {
                if (!process.waitFor(15, TimeUnit.SECONDS)) process.destroyForcibly();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
    }
}
