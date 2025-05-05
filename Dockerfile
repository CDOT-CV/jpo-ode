FROM maven:3.8-eclipse-temurin-21-alpine AS builder
LABEL org.opencontainers.image.authors="583114@bah.com"

WORKDIR /home

# Copy ASN.1 POJO projects first
COPY ./jpo-asn-pojos/pom.xml ./jpo-asn-pojos/
COPY ./jpo-asn-pojos/jpo-asn-j2735-2024 ./jpo-asn-pojos/jpo-asn-j2735-2024
COPY ./jpo-asn-pojos/jpo-asn-runtime ./jpo-asn-pojos/jpo-asn-runtime

# Build and install ASN.1 POJOs first
RUN cd jpo-asn-pojos && mvn clean install -DskipTests

# Copy the rest of the project files
COPY ./pom.xml ./
COPY ./jpo-ode-common/pom.xml ./jpo-ode-common/
COPY ./jpo-ode-common/src ./jpo-ode-common/src
COPY ./jpo-ode-plugins/pom.xml ./jpo-ode-plugins/
COPY ./jpo-ode-plugins/src ./jpo-ode-plugins/src
COPY ./jpo-ode-core/pom.xml ./jpo-ode-core/
COPY ./jpo-ode-core/src ./jpo-ode-core/src/
COPY ./jpo-ode-svcs/pom.xml ./jpo-ode-svcs/
COPY ./jpo-ode-svcs/src ./jpo-ode-svcs/src

# Build the main project
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /home

COPY --from=builder /home/jpo-ode-svcs/src/main/resources/application.yaml /home
COPY --from=builder /home/jpo-ode-svcs/src/main/resources/logback.xml /home
COPY --from=builder /home/jpo-ode-svcs/target/jpo-ode-svcs.jar /home
COPY ./scripts/startup_jpoode.sh /home

RUN apk --no-cache add openssh  \
    && apk --no-cache add openrc  \
    && rc-update add sshd

ENTRYPOINT ["sh", "/home/startup_jpoode.sh"]
