FROM maven:3.8-eclipse-temurin-21-alpine AS builder
LABEL org.opencontainers.image.authors="583114@bah.com"

WORKDIR /home

COPY ./pom.xml ./
COPY ./jpo-ode-common/pom.xml ./jpo-ode-common/
COPY ./jpo-ode-common/src ./jpo-ode-common/src
COPY ./jpo-ode-plugins/pom.xml ./jpo-ode-plugins/
COPY ./jpo-ode-plugins/src ./jpo-ode-plugins/src
COPY ./jpo-ode-core/pom.xml ./jpo-ode-core/
COPY ./jpo-ode-core/src ./jpo-ode-core/src/
COPY ./jpo-ode-svcs/pom.xml ./jpo-ode-svcs/
COPY ./jpo-ode-svcs/src ./jpo-ode-svcs/src

# Build the project (the jpo-asn-pojos library is pulled from Maven Central)
RUN mvn -pl jpo-ode-common,jpo-ode-plugins,jpo-ode-core,jpo-ode-svcs -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /home

COPY --from=builder /home/jpo-ode-svcs/src/main/resources/application.yaml /home
COPY --from=builder /home/jpo-ode-svcs/src/main/resources/logback.xml /home
COPY --from=builder /home/jpo-ode-svcs/target/jpo-ode-svcs.jar /home
COPY ./scripts/startup_jpoode.sh /home

# Upgrade base image packages so cached base layers still get OS security fixes (e.g. brotli-libs, CVE-2026-111497)
RUN apk --no-cache upgrade \
    && apk --no-cache add openssh  \
    && apk --no-cache add openrc  \
    && rc-update add sshd \
    && apk add libstdc++

ENTRYPOINT ["sh", "/home/startup_jpoode.sh"]
