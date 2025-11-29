FROM amazoncorretto:8-al2023-jdk as deps
WORKDIR /build
RUN yum install -y tar
COPY --chmod=0755 mvnw mvnw
COPY .mvn .mvn
COPY pom.xml pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:go-offline -DskipTests



FROM deps as package
WORKDIR /build
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw package -DskipTests



FROM amazoncorretto:8-al2023-jdk as final
COPY --from=package build/target/sdfs-app.jar sdfs-app.jar
