FROM sbtscala/scala-sbt:eclipse-temurin-alpine-21.0.5_11_1.10.5_3.5.2


WORKDIR /cat_tracker

# copy project files and folder to the current directory
COPY . .

# compile jar
RUN sbt assembly

# expose ports
EXPOSE 8081

# set command for the image
CMD ["scala", "target/scala-3.3.4/CatTracker-Backend-assembly-0.1.0-SNAPSHOT.jar", "--main-class", "be.unamur.cattracker.Main"]
