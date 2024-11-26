FROM openjdk:23

ADD target/sso-0.0.1-SNAPSHOT.jar sso-0.0.1-SNAPSHOT.jar

EXPOSE 4001

ENTRYPOINT ["java","-jar","sso-0.0.1-SNAPSHOT.jar"]
