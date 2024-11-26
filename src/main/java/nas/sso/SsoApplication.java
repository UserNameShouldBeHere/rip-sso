package nas.sso;

import java.io.IOException;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import nas.sso.grpc.GrpcSSOServer;
import nas.sso.repository.postgres.AuthRepositoryImpl;
import nas.sso.repository.redis.SessionRepositoryImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;

@SpringBootApplication
public class SsoApplication {
	private static final String postgresConnStr = "jdbc:postgresql://localhost:5432/auth?user=postgres&password=root1234";
	private static final String redisConnStr = "redis://localhost:48464";

	public static void main(String[] args) throws IOException, InterruptedException {
		Server server = ServerBuilder.forPort(4002).addService(
			new GrpcSSOServer(
				new AuthRepositoryImpl(postgresConnStr),
				new SessionRepositoryImpl(redisConnStr, 60*60*24)))
				.build();
		server.start();
		server.awaitTermination();
	}

}
