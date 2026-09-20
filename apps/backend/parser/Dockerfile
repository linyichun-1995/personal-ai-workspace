FROM eclipse-temurin:25-jre@sha256:bb036ed6cfdc57e3da7c22634d15f1b840d2caf76183861c80e81ca4b5104abb
COPY classes /opt/parser/classes
COPY lib /opt/parser/lib
USER 65534:65534
WORKDIR /tmp
ENTRYPOINT ["java","-Xmx256m","-XX:MaxDirectMemorySize=32m","-Djava.awt.headless=true","-cp","/opt/parser/classes:/opt/parser/lib/*","com.example.workspace.parser.ParserMain"]
