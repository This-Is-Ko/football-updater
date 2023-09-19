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

### AWS RDS

.

### AWS EC2

One way to transfer jar to EC2

    scp -i .\[key.pem] .\build\libs\footballupdater-0.0.1-SNAPSHOT.jar ec2-user@[ec2 public dns]:[save path]

Install Java 20 onto instance

    ssh -i .\[key.pem] ec2-user@[instance ip]
    sudo rpm --import https://yum.corretto.aws/corretto.key
    sudo curl -L -o /etc/yum.repos.d/corretto.repo https://yum.corretto.aws/corretto.repo
    sudo yum install -y java-20-amazon-corretto-devel

