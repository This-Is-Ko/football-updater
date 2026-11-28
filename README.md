# Football Updater

Check for player match performance data from their latest game and generate images which contain their statistics for the selected game.

These are uploaded to a S3 bucket and displayed on dashboard to track which posts generated and posted, containing a generated post caption, S3 file links and Google Image search links for recent photos for the player.

From this dashboard, posts can be managed with features such as uploading directly to Instagram (with editable captions and image order), generating unique images to display standout statistics, tracking uploaded/pending posts and deleting.

The custom image generator allows custom backgrounds to be inserted and select what stats to include on the image.

Previously hosted on an EC2 instance, players/teams to check are stored in RDS MySQL database and configured with an external cron job to run at scheduled times.

Currently hosted on a server and deployed in a docker container with data stored in aiven.io MySQL database.

An email notification is also sent to the configured email when new posts are generated.

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
    FACEBOOK_API_TOKEN_REFRESH_THRESHOLD_DAYS - Optional, default 10

After the Facebook login the short-lived token is exchanged for a long-lived token (~60 days), which is saved in the `api_token` table and loaded on startup. It is refreshed automatically once it has fewer than `FACEBOOK_API_TOKEN_REFRESH_THRESHOLD_DAYS` days left. TikTok tokens are saved in the same table and refreshed with the TikTok refresh token. Create the table with `src/main/resources/database/api_token.sql`.

Scheduler (Spring cron format `second minute hour day month weekday`, `-` disables a job)

    SCHEDULER_ZONE - Optional, default UTC
    SCHEDULER_PLAYER_UPDATE_CRON - Optional, default "-" (disabled). e.g. "0 0 6 * * *" to run the player data update daily at 06:00
    SCHEDULER_TOKEN_REFRESH_CRON - Optional, default "0 0 * * * *" (hourly check)
    SCHEDULER_POOL_SIZE - Optional, default 2

The player data update can still be triggered manually with `GET /api/player/data/update`.

LLM editor and caption agents (optional)

    LLM_ENABLED - Optional, default false
    ANTHROPIC_API_KEY
    LLM_MODEL - Optional, default claude-sonnet-5-5
    LLM_MAX_POSTS_PER_RUN - Optional, default 10
    LLM_CAPTION_EXAMPLES_LOCATION - Optional, default classpath:llm/caption-examples.txt

After each player update, new posts go into an approval queue at `/posts/review` (requires the dashboard login).
With the LLM enabled, an editor agent picks which posts to publish, the post type and up to 5 stats to feature, and a
caption agent writes the caption in the style of the examples in `src/main/resources/llm/caption-examples.txt`.
Every number in an LLM caption must appear in the post's stats or match data and names must match the database,
otherwise it retries once and then falls back to the template caption. With the LLM disabled (or on any failure),
the rule-based highlights and template captions are used. STANDOUT picks get a standout image generated with the
player's standout base image, or the generic base image (IMAGE_GENERATOR_GENERIC_BASE_IMAGE_FILE) if there isn't one. Approve uploads to Instagram through the normal upload path, Reject removes the draft from the queue.
Create the new columns with `src/main/resources/database/post_draft_review.sql`.

    LLM_VISION_MODEL - Optional, default claude-opus-5-5, used for photo framing and vision QA

With the LLM enabled, vision QA checks each draft's rendered images and caption after they're generated and shows any
issues (text overflow, clipped face, caption/stat mismatch, low quality, watermark) as warnings on the review page.
It only flags issues, it never blocks approval or edits the post.

Standout photo pipeline

    PHOTO_SOURCES - Optional, default NONE. Priority list of photo sources, like DATASOURCE_PRIORITY
    PHOTO_MATCHER - Optional, default NOOP. NOOP only accepts photos picked on the review page. FACE_SIDECAR matches faces
    PHOTO_FRAMER - Optional, default CENTER. VISION asks the vision model where the player is
    PHOTO_MAX_DOWNLOAD_BYTES - Optional, default 10485760
    PHOTO_DOWNLOAD_TIMEOUT_SECONDS - Optional, default 10

On the review page, "Use photo URL" downloads an http/https image (size limit, timeout, no private addresses), frames
it, regenerates the standout image, re-runs QA and stores the photo URL and licence on the post. No automatic photo
source exists yet, so automatic photos are never used. Create the new columns with
`src/main/resources/database/photo_pipeline.sql`.

Face matching (PHOTO_MATCHER=FACE_SIDECAR)

    FACE_SIDECAR_URL - Optional, default http://localhost:8001
    FACE_MODEL_PACK - Optional, default buffalo_l. Must match the sidecar's model; embeddings are stored per model
    FACE_SIDECAR_CONNECT_TIMEOUT_MILLIS - Optional, default 2000
    FACE_SIDECAR_READ_TIMEOUT_MILLIS - Optional, default 15000
    FACE_MIN_SIMILARITY - Optional, default 0.30. Automatic photos below this are rejected
    FACE_CONFIDENT_SIMILARITY - Optional, default 0.45. Matches below this are flagged on the review card
    FACE_MIN_REFERENCES - Optional, default 3. Reference photos needed before automatic photos are matched
    FACE_MAX_REFERENCES - Optional, default 5
    FACE_CROSS_CHECK_ENABLED - Optional, default true. Claude reads the jersey number, shirt name and caption (needs LLM_ENABLED)

`face-sidecar/` is a stateless Python service (InsightFace detection + ArcFace embeddings) that only embeds and
compares faces. Start it with `docker compose up -d face-sidecar`; the model is baked into the image at build time and
the port is only bound to localhost. Create the table and columns with `src/main/resources/database/face_matching.sql`.

Enrol 3 to 5 reference photos per player, each showing only that player's face:

    POST /api/player/{playerId}/faces   {"urls": ["https://...", "https://...", "https://..."]}
    GET /api/player/{playerId}/faces    (number of references)
    DELETE /api/player/{playerId}/faces

Enrolling replaces the player's existing references. Photos with no face, several similar-sized faces, or a face that
doesn't match the other references are rejected and listed in the response. Set the shirt number with
`PATCH /api/player/{playerId}` `{"shirtNumber": 10}` so the cross-check can compare it.

When matching, the largest face is compared with the references. An automatic photo is skipped when the sidecar is
down or slow, the player has too few references, no face is found, the player isn't the main subject, the similarity
is below FACE_MIN_SIMILARITY, or the jersey number, shirt name or caption contradicts the match. Claude is never asked
to recognise faces. A photo picked on the review page is always used, but if it couldn't be verified it gets
confidence 0 and is flagged. Face embeddings are biometric data: keep them to the squad, and delete them
(`DELETE .../faces`, or deleting the player) when a player leaves. The buffalo_l pretrained models are licensed by
InsightFace for non-commercial research use only.

Approve from your phone (optional)

    REVIEW_LINK_BASE_URL - Public address of the app, e.g. https://updater.example.com
    REVIEW_LINK_SECRET - Random value of at least 32 characters used to sign the links
    REVIEW_LINK_EXPIRY_HOURS - Optional, default 48

When both are set (and the email is enabled), the update email has a review link for each draft. The link opens a
phone-sized page with the images and an editable caption, and Approve / Reject buttons. No dashboard login is needed:
the link is signed and single-use, so it stops working once the draft is approved or rejected, when a newer email
replaces it, or when it expires. Opening the link never approves anything on its own, so mail scanners that preview
links are harmless. If approving fails (e.g. Instagram is logged out) the link stays usable for a retry.
Create the new column with `src/main/resources/database/review_link.sql`.

### API

API endpoints can be configured to check for a secret to prevent unauthorized access. This value can be configured with 

    ENDPOINT_SECRET

and needs to be passed in a custom header

    Auth-Secret

If no secret is set, all calls to the endpoint will be allowed.

### Dashboard

The dashboard can be accessed at

    hostname:port/posts

All dashboard pages (`/posts`, `/players`, `/teams`, `/performance`) require the dashboard login (user `admin`). The Spring Security default login page is used and the password can be configured following the above.

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

    scp -i .\[key.pem] .\build\libs\app.jar ec2-user@[ec2 public dns]:[save path]

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
    ExecStart=java -jar /home/ec2-user/app.jar --spring.profiles.active=prod
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

To set up fonts for image generation, add font files into 

    /src/resources/fonts

These fonts will be registered on startup and displayed in the Font Validator to ensure they are registered

Alternatively you can copy font files on to the server and follow below commands

    scp -i .\[key.pem] .\src\main\resources\fonts\* ec2-user@[ec2 public dns]:~/.local/share/fonts

Install fontconfig packages

    sudo apt install fontconfig

Add the following font files

    /usr/share/fonts/ChakraPetch-Bold.ttf
    /usr/share/fonts/Nike_Ithaca.otf
    /usr/share/fonts/Wagner_Modern.ttf

Refresh font cache

    fc-cache -f -v

Check fonts are available

    fc-list | grep Wagner Modern

Additional reference for fonts on Linux https://medium.com/source-words/how-to-manually-install-update-and-uninstall-fonts-on-linux-a8d09a3853b0

### Images

Posts page:
![image](https://github.com/This-Is-Ko/football-updater/assets/52279273/e0eced21-642f-42e5-ac63-d3613b827b8b)

Generate custom post page:
![image](https://github.com/This-Is-Ko/football-updater/assets/52279273/90e5a368-9831-4777-a36a-c8e4e658f605)

Upload to Instagram page:
![image](https://github.com/This-Is-Ko/football-updater/assets/52279273/136ff159-ddf6-4e9f-bf3a-f437b95f8556)
