FROM eclipse-temurin:21-jre-jammy AS runner
WORKDIR runner
COPY **/target/app.jar runner/
CMD java -jar runner/app.jar 
