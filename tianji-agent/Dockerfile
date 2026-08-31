FROM eclipse-temurin:17-jre

ENV TZ=Asia/Shanghai
WORKDIR /app
COPY target/tianji-agent-*.jar /app/app.jar

EXPOSE 8094
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
