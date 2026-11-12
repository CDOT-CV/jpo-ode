FROM maven:3.8-eclipse-temurin-21-alpine AS builder

WORKDIR /home

# Copy the whole build context, so every build input (such as lombok.config) is included.
# .dockerignore leaves out build output, secrets and files the build doesn't use
COPY . .

# Build all the modules (the jpo-asn-pojos library is pulled from Maven Central)
RUN mvn package -DskipTests

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
