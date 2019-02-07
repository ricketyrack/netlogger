FROM clojure:latest
RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
COPY project.clj /usr/src/app/
RUN lein deps
COPY . /usr/src/app
RUN mv "$(lein uberjar | grep kaufmann.jar | sed -e 's/Created //')" app-standalone.jar
CMD ["java", "-jar", "app-standalone.jar", "clojure.main", "-m", "kaufmann.server" ]
