FROM java:8
MAINTAINER Ronald Holshausen <rholshausen@dius.com.au>

RUN useradd -m pact-jvm-server -G users
WORKDIR /home/pact-jvm-server
COPY build/2.11/install/pact-jvm-server_2.11 /home/pact-jvm-server
RUN chown -R pact-jvm-server:users /home/pact-jvm-server

CMD ["/home/pact-jvm-server/bin/pact-jvm-server_2.11", "8080", "-h", "0.0.0.0", "-d", "-u", "20010", "--debug"]
EXPOSE 8080 20000-20010
