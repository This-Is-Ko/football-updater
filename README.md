# Football Updater

Check for player match performance data from their latest game and generate images which contain their statistics for the selected game.

These are uploaded to a S3 bucket and an email is sent to the configured email containing a generated post caption, S3 file links and Google Image search links for recent photos for the player.

Hosted on an EC2 instance, players/teams to check are stored in RDS MySQL database and configured with an external cron job to run at scheduled times.

Included a dashboard to track which posts have been posted with the same content in additional to the email notifications.

From this dashboard, posts can be marked as posted, or deleted, and generate different post types for standout statistics.

Posts page: 
    ![image](https://github.com/This-Is-Ko/football-updater/assets/52279273/598b68d1-ebaf-4de6-9c91-f5ee94cb3f3a)

Generate custom post page: 
    ![image](https://github.com/This-Is-Ko/football-updater/assets/52279273/1c07c116-db01-4cfa-a1a9-f0008c8c0b19)


### Build
To build jar use 
    
    ./gradlew bootjar

To run with the executable jar

    java -jar footballupdater-0.0.1-SNAPSHOT.jar

Additional env params can be passed in with

    java -jar footballupdater-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
    --SPRING_DATASOURCE_HOST=****
    --SPRING_DATASOURCE_USERNAME=***
    --SPRING_DATASOURCE_PASSWORD=***
    --SPRING_DATASOURCE_NAME=****
    --MAILER_FROM_ADDRESS=****
    --MAILER_FROM_PASSWORD=****
    --MAILER_TO_NAME=****
    --MAILER_TO_ADDRESS=****
    --ENDPOINT_SECRET=****

### Configurable

The main configurable groupings are:

Sprint Datasource (Database)

    SPRING_DATASOURCE_HOST
    SPRING_DATASOURCE_NAME
    SPRING_DATASOURCE_USERNAME
    SPRING_DATASOURCE_PASSWORD

Endpoint secret

    ENDPOINT_SECRET

Dashboard password

    DASHBOARD_PASSWORD

Datasource (Stat sources)

    DATASOURCE_PRIORITY

Post Version

    IG_POST_VERSION
    IG_POST_ACCOUNT_NAME
    IG_POST_DEFAULT_HASHTAGS

Stat Image generator
    
    IMAGE_GENERATOR_ENABLED
    IMAGE_GENERATOR_INPUT_PATH
    IMAGE_GENERATOR_OUTPUT_PATH

Mailer

    MAILER_IS_ENABLED
    MAILER_SUBJECT
    MAILER_FROM_NAME
    MAILER_FROM_ADDRESS
    MAILER_FROM_PASSWORD
    MAILER_TO_NAME
    MAILER_TO_ADDRESS
    MAILER_IS_ATTACH_IMAGES

AWS S3 Uploads

    AWS_S3_IS_ENABLED
    AWS_ACCESS_KEY
    AWS_SECRET_KEY
    AWS_S3_BUCKET_NAME

### API

API endpoints can be configured to check for a secret to prevent unauthorized access. This value can be configured with 

    ENDPOINT_SECRET

and needs to be passed in a custom header

    Auth-Secret

If no secret is set, all calls to the endpoint will be allowed.

### Dashboard

The dashboard can be accessed at

    hostname:port/posts

The Spring Security default login page is used and the password can be configured following the above.

    DASHBOARD_PASSWORD

Favicon from https://iconscout.com/free-icon/soccer-8


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

Set up service

    sudo nano /etc/systemd/system/football-updater.service

Inside football-updater.service

    [Unit]
    Description=football-updater-spring
    After=syslog.target
    
    [Service]
    User=[USER/root]
    ExecStart=[Jar Run command]
    SuccessExitStatus=143
    TimeoutStopSec=10
    Restart=on-failure
    RestartSec=5
    
    [Install]
    WantedBy=multi-user.target

Ensure jar path in run command contains the full path "/home/ec2-user/..."

Enable service

    sudo systemctl enable football-updater.service
    sudo systemctl start football-updater.service
    systemctl status football-updater.service
    # To see more logs - add -f to see log stream
    journalctl _SYSTEMD_UNIT=football-updater.service -f
    # Restart service after file changes
    sudo systemctl daemon-reload
    sudo systemctl restart football-updater

To set up fonts for image generation, add font files to 

    /usr/share/fonts/ChakraPetch-Bold.ttf
    /usr/share/fonts/Nike_Ithaca.otf

Additional reference for fonts on Linux https://medium.com/source-words/how-to-manually-install-update-and-uninstall-fonts-on-linux-a8d09a3853b0

