FROM java:8
MAINTAINER Ronald Holshausen <rholshausen@dius.com.au>

RUN useradd -m pact-jvm-server -G users
WORKDIR /home/pact-jvm-server
ADD build/2.12/distributions/pact-jvm-server_2.12-*.tar /home/pact-jvm-server
RUN ln -s /home/pact-jvm-server/pact-jvm-server_2.12-*/bin/ /home/pact-jvm-server/bin
RUN ln -s /home/pact-jvm-server/pact-jvm-server_2.12-*/lib/ /home/pact-jvm-server/lib
RUN chown -R pact-jvm-server:users /home/pact-jvm-server

CMD ["/home/pact-jvm-server/bin/pact-jvm-server_2.12", "8080", "-h", "0.0.0.0", "-d", "-u", "20010", "--debug"]
EXPOSE 8080 20000-20010
