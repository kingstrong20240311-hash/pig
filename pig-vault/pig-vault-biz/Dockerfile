FROM eclipse-temurin:21-jre

WORKDIR /pig-vault

ARG JAR_FILE=target/pig-vault-biz.jar

COPY ${JAR_FILE} app.jar

EXPOSE 5003

ENV TZ=Asia/Shanghai JAVA_OPTS="-Xms128m -Xmx256m -Djava.security.egd=file:/dev/./urandom"

CMD sleep 60; java $JAVA_OPTS -jar app.jar
