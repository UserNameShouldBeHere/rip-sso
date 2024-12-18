package nas.sso.grpc;

import java.sql.SQLException;

import org.lognet.springboot.grpc.GRpcService;

import io.grpc.stub.StreamObserver;
import nas.sso.exception.InvalidSessionException;
import nas.sso.exception.PasswordHashException;
import nas.sso.model.UserSession;
import nas.sso.model.TokenPayload;
import nas.sso.repository.AuthRepository;
import nas.sso.repository.SessionRepository;
import sso.SSOGrpc.SSOImplBase;
import sso.*;

@GRpcService
public class GrpcSSOServer extends SSOImplBase {
    private AuthRepository authRepo;
    private SessionRepository sessionRepo;

    public GrpcSSOServer(AuthRepository authRepo, SessionRepository sessionRepo) {
        this.authRepo = authRepo;
        this.sessionRepo = sessionRepo;
    }

    @Override
    public void check(CheckRequest request, StreamObserver<CheckResponse> responseObserver) {
        try {
            TokenPayload tokenPayload = sessionRepo.check(request.getToken());

            responseObserver.onNext(CheckResponse
            .newBuilder()
            .setRespStatus(StatusResponse
                .newBuilder()
                .setStatus(0)
                .build())
            .setIsValidSession(tokenPayload.isTokenValid())
            .setPayload(
                sso.TokenPayload
                    .newBuilder()
                    .setUuid(tokenPayload.getUuid())
                    .setUsername(tokenPayload.getUsername())
                    .build()
            )
            .build());
        } catch (InvalidSessionException e) {
            responseObserver.onNext(CheckResponse
            .newBuilder()
            .setRespStatus(StatusResponse
                .newBuilder()
                .setStatus(3)
                .build())
            .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void hasUser(HasUserRequest request, StreamObserver<HasUserResponse> responseObserver) {
        boolean hasUser = false;
        try {
            hasUser = authRepo.hasUser(request.getUsername());
        } catch (Exception e) {
            System.err.println(e);

            responseObserver.onNext(HasUserResponse
            .newBuilder()
            .setRespStatus(StatusResponse
                .newBuilder()
                .setStatus(13)
                .build())
            .build());
            responseObserver.onCompleted();
        }

        responseObserver.onNext(HasUserResponse
            .newBuilder()
            .setRespStatus(StatusResponse
                .newBuilder()
                .setStatus(0)
                .build())
            .setHasUser(hasUser)
            .build());
        responseObserver.onCompleted();
    }

    @Override
    public void signIn(SignInRequest request, StreamObserver<SignInResponse> responseObserver) {
        try {
            boolean isValidPassword = authRepo.checkPassword(request.getUsername(), request.getPassword());

            String userUuid = authRepo.getUserUuid(request.getUsername());

            String token = sessionRepo.createSession(new UserSession(userUuid, request.getUsername(), 1));

            if (!isValidPassword) {
                responseObserver.onNext(SignInResponse
                .newBuilder()
                .setRespStatus(StatusResponse
                    .newBuilder()
                    .setStatus(3)
                    .build())
                .build());
            } else {
                responseObserver.onNext(SignInResponse
                .newBuilder()
                .setRespStatus(StatusResponse
                    .newBuilder()
                    .setStatus(0)
                    .build())
                .setToken(token)
                .build());
            }
        } catch (SQLException | PasswordHashException e) {
            System.err.println(e);

            responseObserver.onNext(SignInResponse
            .newBuilder()
            .setRespStatus(StatusResponse
                .newBuilder()
                .setStatus(3)
                .build())
            .build());
        }

        responseObserver.onCompleted();
    }

    @Override
    public void signUp(SignUpRequest request, StreamObserver<SignUpResponse> responseObserver) {
        if (request.getUsername().length() <= 3) {
            responseObserver.onNext(SignUpResponse
                .newBuilder()
                .setRespStatus(StatusResponse
                    .newBuilder()
                    .setStatus(3)
                    .build())
                .build());
            responseObserver.onCompleted();

            return;
        }

        boolean hasUser = false;
        try {
            hasUser = authRepo.hasUser(request.getUsername());
            if (hasUser) {
                responseObserver.onNext(SignUpResponse
                    .newBuilder()
                    .setRespStatus(StatusResponse
                        .newBuilder()
                        .setStatus(6)
                        .build())
                    .build());
            } else {
                String uuid = authRepo.createUser(request.getUsername(), request.getPassword());

                String token = sessionRepo.createSession(new UserSession(uuid, request.getUsername(), 1));

                responseObserver.onNext(SignUpResponse
                    .newBuilder()
                    .setRespStatus(StatusResponse
                        .newBuilder()
                        .setStatus(0)
                        .build())
                    .setToken(token)
                    .build());
            }
        } catch (SQLException | PasswordHashException e) {
            System.err.println(e);

            responseObserver.onNext(SignUpResponse
                .newBuilder()
                .setRespStatus(StatusResponse
                    .newBuilder()
                    .setStatus(13)
                    .build())
                .build());
        }
        
        responseObserver.onCompleted();
    }

    @Override
    public void updatePassword(UpdateRequest request, StreamObserver<UpdateResponse> responseObserver) {
        responseObserver.onNext(UpdateResponse
        .newBuilder()
        .setRespStatus(StatusResponse
            .newBuilder()
            .setStatus(12)
            .build())
        .build());
        responseObserver.onCompleted();
    }

    @Override
    public void updateUsername(UpdateRequest request, StreamObserver<UpdateResponse> responseObserver) {
        responseObserver.onNext(UpdateResponse
        .newBuilder()
        .setRespStatus(StatusResponse
            .newBuilder()
            .setStatus(12)
            .build())
        .build());
        responseObserver.onCompleted();
    }

    @Override
    public void logout(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        boolean state;
        try {
            state = this.sessionRepo.removeSession(request.getToken());
        } catch (InvalidSessionException e) {
            responseObserver.onNext(LogoutResponse
            .newBuilder()
            .setRespStatus(StatusResponse
                .newBuilder()
                .setStatus(16)
                .build())
            .build());
            responseObserver.onCompleted();

            return;
        }

        if (!state) {
            responseObserver.onNext(LogoutResponse
            .newBuilder()
            .setRespStatus(StatusResponse
                .newBuilder()
                .setStatus(16)
                .build())
            .build());
        } else {
            responseObserver.onNext(LogoutResponse
            .newBuilder()
            .setRespStatus(StatusResponse
                .newBuilder()
                .setStatus(0)
                .build())
            .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void logoutAll(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        boolean state;
        try {
            state = this.sessionRepo.removeAllSessions(request.getToken());
        } catch (InvalidSessionException e) {
            responseObserver.onNext(LogoutResponse
            .newBuilder()
            .setRespStatus(StatusResponse
                .newBuilder()
                .setStatus(16)
                .build())
            .build());
            responseObserver.onCompleted();

            return;
        }

        if (!state) {
            responseObserver.onNext(LogoutResponse
            .newBuilder()
            .setRespStatus(StatusResponse
                .newBuilder()
                .setStatus(16)
                .build())
            .build());
        } else {
            responseObserver.onNext(LogoutResponse
            .newBuilder()
            .setRespStatus(StatusResponse
                .newBuilder()
                .setStatus(0)
                .build())
            .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getUserUUIDByName(GetUserUUIDByNameRequest request, StreamObserver<GetUserUUIDByNameResponse> responseObserver) {
        try {
            String userUuid = this.authRepo.getUserUuid(request.getUsername());

            responseObserver.onNext(GetUserUUIDByNameResponse
            .newBuilder()
            .setRespStatus(StatusResponse
                .newBuilder()
                .setStatus(0)
                .build())
            .setUserUuid(userUuid)
            .build());
        } catch (SQLException e) {
            responseObserver.onNext(GetUserUUIDByNameResponse
            .newBuilder()
            .setRespStatus(StatusResponse
                .newBuilder()
                .setStatus(5)
                .build())
            .build());
        }
        responseObserver.onCompleted();
    }
}
