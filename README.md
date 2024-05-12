# Football Updater

Check for player match performance data from their latest game and generate images which contain their statistics for the selected game.

These are uploaded to a S3 bucket and displayed on dashboard to track which posts generated and posted, containing a generated post caption, S3 file links and Google Image search links for recent photos for the player.

From this dashboard, posts can be managed with features such as uploading directly to Instagram (with editable captions and image order), generating unique images to display standout statistics, tracking uploaded/pending posts and deleting.

The custom image generator allows custom backgrounds to be inserted and select what stats to include on the image.

Hosted on an EC2 instance, players/teams to check are stored in RDS MySQL database and configured with an external cron job to run at scheduled times.

An email notification is also sent to the configured email when new posts are generated.

Posts page: 
    ![image](https://github.com/This-Is-Ko/football-updater/assets/52279273/e0eced21-642f-42e5-ac63-d3613b827b8b)

Generate custom post page: 
    ![image](https://github.com/This-Is-Ko/football-updater/assets/52279273/90e5a368-9831-4777-a36a-c8e4e658f605)

Upload to Instagram page:
    ![image](https://github.com/This-Is-Ko/football-updater/assets/52279273/136ff159-ddf6-4e9f-bf3a-f437b95f8556)


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
    IMAGE_GENERATOR_GENERIC_BASE_IMAGE_FILE

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
    AWS_S3_OBJECT_KEY_PREFIX - Optional

Facebook and Instagram API

    FACEBOOK_API_CLIENT_ID
    FACEBOOK_API_CLIENT_SECRET
    FACEBOOK_API_RESPONSE_TYPE
    FACEBOOK_API_SCOPE
    FACEBOOK_API_REDIRECT_URI
    FACEBOOK_API_INSTAGRAM_USER_ID

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

The Facebook API config needs to be set up in order to use the uploader. Other features all work without the Facebook login setup.

Create an app in the Facebook developer portal and configure the clientId, secret and redirectUris. The instagram account will need to be linked and accountId also configured.

Instagram publishing follows the flow reccommended by Facebook seen here https://developers.facebook.com/docs/instagram-api/guides/content-publishing#carousel-posts

Due to Facebook's security rules, to run in an environment which is not "localhost", SSL needs to be configured (to use HTTPS connection) and a custom domain is required. 

Favicon from https://iconscout.com/free-icon/soccer-8


### Local MYSQL

Use to login to local MySQL

    ./mysql -u root -p

### AWS RDS

Current prod config points to an AWS RDS endpoint which is running MySQL.

Ensure the EC2 and RDS are within the same VPC and disable public access to avoid getting charged for multiple public IPs.

Update the application-[env].properties with the relevant config

    SPRING_DATASOURCE_HOST
    SPRING_DATASOURCE_NAME
    SPRING_DATASOURCE_USERNAME
    SPRING_DATASOURCE_PASSWORD

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

Alternatively use the dockerfiles to build and deploy. Ensure docker is installed on the machine.

### Certificate Generation

Follow the instructions here to renew certificate and generate JKS https://keychest.net/stories/lets-encrypt-certificate-into-java-jks

Next move the JKS into path which matches the config > server.ssl.key-store

### Fonts

To set up fonts for image generation, copy font files across to ec2

    scp -i .\[key.pem] .\src\main\resources\fonts\* ec2-user@[ec2 public dns]:~/.local/share/fonts

Add the following font files

    /usr/share/fonts/ChakraPetch-Bold.ttf
    /usr/share/fonts/Nike_Ithaca.otf
    /usr/share/fonts/Wagner_Modern.ttf

Refresh font cache

    fc-cache -f -v

Check fonts are available

    fc-list | grep Wagner Modern

Additional reference for fonts on Linux https://medium.com/source-words/how-to-manually-install-update-and-uninstall-fonts-on-linux-a8d09a3853b0

