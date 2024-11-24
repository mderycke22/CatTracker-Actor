#
# Dockerfile for production environment
#

FROM sbtscala/scala-sbt:eclipse-temurin-alpine-21.0.5_11_1.10.5_3.5.2


# make the 'python-docker' the current working directory
WORKDIR /cat_tracker

# copy project files and folder to the current directory
COPY . .

# compile jar
RUN sbt assembly

# expose ports
EXPOSE 47474
EXPOSE 8081
EXPOSE 1883
EXPOSE 5432

# set command for the image
CMD ["scala", "target/scala-3.3.4/CatTracker-Backend-assembly-0.1.0-SNAPSHOT.jar", "--main-class", "be.unamur.cattracker.Main"]