FROM maven:3.6-jdk-11 as builder
# Build application jar
WORKDIR /root/
COPY ./transport/ ./
RUN mvn package -Dmaven.test.skip=true

FROM openjdk:11
# Start application container
RUN apt-get update \
    && apt-get install -y tree

# Load jar to image
WORKDIR /root/
COPY --from=builder /root/target/transport-*.jar transport.jar

# Copy run script
COPY ./dafni-run.sh dafni-run.sh

# Run application
ENV ARGS="-h"
CMD bash /root/dafni-run.sh
