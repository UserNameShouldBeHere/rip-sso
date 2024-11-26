package nas.sso;

import java.io.IOException;
import java.util.Map;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import nas.sso.repository.postgres.AuthRepositoryImpl;
import nas.sso.repository.redis.SessionRepositoryImpl;
import nas.sso.grpc.GrpcSSOServer;
import io.grpc.Server;
import io.grpc.ServerBuilder;

@SpringBootApplication
public class SsoApplication {
	public static void main(String[] args) throws IOException, InterruptedException {
		Map<String, String> env = System.getenv();

		Server server = ServerBuilder.forPort(4001).addService(
			new GrpcSSOServer(
				new nas.sso.repository.postgres.AuthRepositoryImpl(
					env.get("POSTGRES_HOST"),
					env.get("POSTGRES_PORT"),
					env.get("POSTGRES_DB"),
					env.get("POSTGRES_USER"),
					env.get("POSTGRES_PASSWORD")
				),
				new nas.sso.repository.redis.SessionRepositoryImpl(
					env.get("REDIS_HOST"),
					env.get("REDIS_PORT"),
					60*60*24
				)))
				.build();
		server.start();
		server.awaitTermination();
	}

}
