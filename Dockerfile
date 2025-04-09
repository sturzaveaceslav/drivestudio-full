# Imagine de bază cu Java 17
FROM eclipse-temurin:17-jdk-alpine

# Setăm directorul de lucru în container
WORKDIR /app

# Copiem fișierul JAR în container
COPY target/drivestudio-1.0.0.jar app.jar

# Portul pe care rulează aplicația
EXPOSE 8080

# Comanda de pornire a aplicației
ENTRYPOINT ["java", "-jar", "app.jar"]
