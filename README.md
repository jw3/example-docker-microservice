Microservice examples using Docker and Akka
-

Various examples of microservices using Akka HTTP.

The SBT build uses the sbt-docker plugin to build Docker images for the services.

```"se.marcuslonnberg" % "sbt-docker" % "1.2.0"```

##### Using the twitter examples
Copy the ```twitter4j.properties.template``` to ```twitter4j.properties``` and add your Twitter app authentication [(Docs)](http://twitter4j.org/en/configuration.html)