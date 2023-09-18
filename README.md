# Football Updater


### Build
To build jar use 
    
    ./gradlew bootjar

To run with the executable jar

    java -jar footballupdater-0.0.1-SNAPSHOT.jar

Additional env params can be passed in with

    java -jar footballupdater-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --MYSQL_HOST=**** --SPRING_DATASOURCE_USERNAME=**** --SPRING_DATASOURCE_PASSWORD=**** --MYSQL_DATABASE_NAME=****


### Local MYSQL

Use to login to local mysql

    ./mysql -u root -p

### AWS EC2

One way to transfer jar to EC2

    scp -i .\somekey.pem .\build\libs\footballupdater-0.0.1-SNAPSHOT.jar ec2-user@[ec2 public dns]:[save path]

