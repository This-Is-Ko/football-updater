package com.ko.footballupdater.services;

import com.ko.footballupdater.configuration.ImageGeneratorProperties;
import com.ko.footballupdater.configuration.InstagramPostProperies;
import com.ko.footballupdater.configuration.TeamProperties;
import com.ko.footballupdater.models.ImageStatEntry;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import com.ko.footballupdater.models.PostType;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.Team;
import com.ko.footballupdater.models.form.HorizontalTranslation;
import com.ko.footballupdater.models.form.ImageGenParams;
import com.ko.footballupdater.models.form.StatisticEntryGenerateDto;
import com.ko.footballupdater.models.form.VerticalTranslation;
import com.ko.footballupdater.repositories.TeamRepository;
import com.ko.footballupdater.utils.DateTimeHelper;
import com.ko.footballupdater.utils.LogHelper;
import com.ko.footballupdater.utils.PostHelper;
import com.ko.footballupdater.utils.TeamHelpers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ko.footballupdater.utils.ImageGeneratorConstants.ASSIST_ICON_FILE_NAME;
import static com.ko.footballupdater.utils.ImageGeneratorConstants.BASE_IMAGE_DIRECTORY;
import static com.ko.footballupdater.utils.ImageGeneratorConstants.BASE_IMAGE_FILE_NAME;
import static com.ko.footballupdater.utils.ImageGeneratorConstants.GOAL_ICON_FILE_NAME;
import static com.ko.footballupdater.utils.ImageGeneratorConstants.ICON_DIRECTORY;
import static com.ko.footballupdater.utils.ImageGeneratorConstants.RED_CARD_ICON_FILE_NAME;
import static com.ko.footballupdater.utils.ImageGeneratorConstants.STANDOUT_BASE_IMAGE_FILE_NAME;
import static com.ko.footballupdater.utils.ImageGeneratorConstants.STAT_Y_COORDINATE;
import static com.ko.footballupdater.utils.ImageGeneratorConstants.TEAM_LOGO_DIRECTORY;
import static com.ko.footballupdater.utils.ImageGeneratorConstants.YELLOW_CARD_ICON_FILE_NAME;

@Slf4j
@Service
public class ImageGeneratorService {

    @Autowired
    private TeamHelpers teamHelpers;

    @Autowired
    private ImageGeneratorProperties imageGeneratorProperties;

    @Autowired
    private InstagramPostProperies instagramPostProperies;

    @Autowired
    private TeamProperties teamProperties;
    private final Map<String, BufferedImage> teamLogoCache = new HashMap<>();
    private final Map<String, BufferedImage> iconCache = new HashMap<>();
    private final Map<String, BufferedImage> baseImageCache = new HashMap<>();

    public void generatePlayerStatImage(Post post) throws Exception {
        if (!imageGeneratorProperties.isEnabled()) {
            return;
        }

        try {
            // Load the base image - priority order:
            // 1. Team specific base image
            // 2. Player specific base image
            // 3. Generic base image
            BufferedImage image = null;
            BufferedImage selectedBaseImage = null;
            try {
                if (post.getPlayerMatchPerformanceStats().getMatch().getRelevantTeam() != null) {
                    image = getImageUsingCache(post.getPlayer().getName().replaceAll(" ", "") + BASE_IMAGE_FILE_NAME, imageGeneratorProperties.getExternalImageStoreUri() + BASE_IMAGE_DIRECTORY + imageGeneratorProperties.getInputPath() + post.getPlayerMatchPerformanceStats().getMatch().getRelevantTeam().replaceAll(" ", "") + "/", baseImageCache);
                    selectedBaseImage = cloneBufferImage(image);
                }
            } catch (IOException | IllegalArgumentException ex) {
                log.atDebug().setMessage("No team specific base image found" + post.getPlayer().getName()).log();
            }

            if (image == null) {
                // Use default base image for player
                try {
                    image = getImageUsingCache(post.getPlayer().getName().replaceAll(" ", "") + BASE_IMAGE_FILE_NAME, imageGeneratorProperties.getExternalImageStoreUri() + BASE_IMAGE_DIRECTORY + imageGeneratorProperties.getInputPath() + "/", baseImageCache);
                    selectedBaseImage = cloneBufferImage(image);
                } catch (IOException ex) {
                    log.atDebug().setMessage("No player base image found" + post.getPlayer().getName()).log();
                }
                if (image == null) {
                    // Generic base image
                    image = getImageUsingCache(imageGeneratorProperties.getGenericBaseImageFile(), imageGeneratorProperties.getExternalImageStoreUri() + BASE_IMAGE_DIRECTORY + imageGeneratorProperties.getInputPath() + "/", baseImageCache);
                    selectedBaseImage = cloneBufferImage(image);
                }
            }

            drawAllStatsPlayerName(image, post);

            // Match stats to image
            Graphics2D graphics = setUpStatsGraphicsDefaults(image);
            // Starting coordinate for first row
            int statNameX = 79;
            int statValueX = 450;
            int statY = STAT_Y_COORDINATE;

            int attributeCounter = 0;
            int createdImageCounter = 0;
            for (Field field : post.getPlayerMatchPerformanceStats().getClass().getDeclaredFields()) {
                // Make the private field accessible
                field.setAccessible(true);
                try {
                    // Get the field's value
                    Object value = field.get(post.getPlayerMatchPerformanceStats());
                    // Only use stat values which are populated and filter select stats if they are NOT zero
                    if (value != null && !field.getName().equals("match") && !field.getName().equals("dataSourceSiteName")) {
                        List<String> zeroValueFilter = getZeroValueFilter();
                        if (zeroValueFilter.contains(field.getName()) && value.equals(0)) {
                            continue;
                        }
                        // Generate proper stat name - capitalise words and spacing
                        ImageStatEntry imageStatEntry = generateDisplayedName(field.getName(), value);

                        graphics.drawString(imageStatEntry.getName(), statNameX, statY);
                        graphics.drawString(imageStatEntry.getValue(), statValueX, statY);
                        // Shift y coordinate down to next position
                        statY += 51;
                        attributeCounter++;
                        // 12 Rows on one image
                        // Once max stats for one image is added, generate new image
                        if (attributeCounter % 12 == 0) {
                            createdImageCounter++;
                            saveImage(post, image, generateFileName(post, createdImageCounter, PostType.ALL_STAT_POST), createdImageCounter);
                            image = cloneBufferImage(selectedBaseImage);
                            drawAllStatsPlayerName(image, post);
                            graphics = setUpStatsGraphicsDefaults(image);
                            statY = STAT_Y_COORDINATE;
                        }
                    }
                } catch (IllegalAccessException ex) {
                    log.atWarn().setMessage("Error while generating stat image").setCause(ex).addKeyValue("player", post.getPlayer().getName()).log();
                }
            }

            // Dispose of the graphics object to release resources
            graphics.dispose();

            // Save the modified image
            createdImageCounter++;
            saveImage(post, image, generateFileName(post, createdImageCounter, PostType.ALL_STAT_POST), createdImageCounter);
        } catch (Exception ex) {
            log.atWarn().setMessage("Error while generating stat image").setCause(ex).addKeyValue("player", post.getPlayer().getName()).log();
            throw new Exception(post.getPlayer().getName() + " - Error while generating stat image ", ex);
        }
    }

    private BufferedImage loadImage(String baseImagePath) throws IOException {
        return ImageIO.read(new File(baseImagePath));
    }

    private BufferedImage setUpBaseImageWithBackgroundImageUrl(ImageGenParams imageGenParams) throws IOException {
        URL imageUrl = URI.create(imageGenParams.getImageUrl()).toURL();
        BufferedImage downloadedImage = ImageIO.read(imageUrl);
        BufferedImage background;
        float scale = 1;

        if (downloadedImage == null) {
            throw new IOException(String.format("Downloaded image is missing - image from %s", imageUrl));
        }
        // Landscape image
        if (downloadedImage.getWidth() >= downloadedImage.getHeight()) {
            // Scale image down to height of 1000 - change width proportionally
            if (imageGenParams.getForceScaleImage() || downloadedImage.getHeight() > 1000) {
                scale = (float) 1000 /downloadedImage.getHeight();
            }

            // No horizontal translation, directly draw image
            if (imageGenParams.getImageHorizontalTranslation() == null || HorizontalTranslation.NONE.equals(imageGenParams.getImageHorizontalTranslation())) {
                background = new BufferedImage((int) (scale * downloadedImage.getWidth()), (int) (scale * downloadedImage.getHeight()), BufferedImage.TYPE_INT_RGB);
                Graphics2D imageGraphics = background.createGraphics();
                imageGraphics.scale(scale, scale);
                imageGraphics.drawImage(downloadedImage, 0 , 0, null);
                imageGraphics.dispose();
            }
            // Requires horizontal translation as subject is not center of image
            // Since we are moving the image, default to 1000 pixel size
            else {
                background = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB);
                Graphics2D imageGraphics = background.createGraphics();
                imageGraphics.scale(scale, scale);
                if (HorizontalTranslation.CENTER.equals(imageGenParams.getImageHorizontalTranslation())) {
                    // Top left may need to be drawn off-canvas in order to center
                    int xTranslation = (int) ((downloadedImage.getWidth() * scale) - background.getWidth()) / 2;
                    imageGraphics.drawImage(downloadedImage, -xTranslation, 0, null);
                } else if (HorizontalTranslation.LEFT.equals(imageGenParams.getImageHorizontalTranslation())) {
                    // Top left will remain 0,0
                    // Horizontal offset will move image towards the left
                    // Offset will only be used from left side
                    int horizontalOffset = 0;
                    if (imageGenParams.getImageHorizontalOffset() != null) {
                        horizontalOffset = imageGenParams.getImageHorizontalOffset();
                    }
                    imageGraphics.drawImage(downloadedImage, -horizontalOffset, 0, null);
                } else if(HorizontalTranslation.RIGHT.equals(imageGenParams.getImageHorizontalTranslation())) {
                    // Top left will need to be drawn off-canvas and right side of image will appear at center
                    int xTranslation = (int) ((downloadedImage.getWidth() * scale) - background.getWidth());
                    imageGraphics.drawImage(downloadedImage, -xTranslation, 0, null);
                }
                imageGraphics.dispose();
            }
            return background;
        }
        // Portrait image
        else {
            if (imageGenParams.getForceScaleImage() || downloadedImage.getWidth() > 1000) {
                scale = (float) 1000 /downloadedImage.getWidth();
            }
            // Scale image, default to 1000x1000 pixel size
            background = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB);
            Graphics2D imageGraphics = background.createGraphics();
            imageGraphics.scale(scale, scale);
            if (VerticalTranslation.CENTER.equals(imageGenParams.getImageVerticalTranslation())) {
                // Top left may need to be drawn off-canvas in order to center
                int yTranslation = (int) ((downloadedImage.getHeight() * scale) - background.getHeight()) / 2;
                imageGraphics.drawImage(downloadedImage, 0, -yTranslation, null);
            } else if (imageGenParams.getImageVerticalTranslation() == null || VerticalTranslation.NONE.equals(imageGenParams.getImageVerticalTranslation()) ||  VerticalTranslation.TOP.equals(imageGenParams.getImageVerticalTranslation())) {
                // Top left will remain 0,0
                // vertical offset will move image downward
                int verticalOffset = 0;
                if (imageGenParams.getImageVerticalOffset() != null) {
                    verticalOffset = imageGenParams.getImageVerticalOffset();
                }
                imageGraphics.drawImage(downloadedImage, 0, -verticalOffset, null);
            } else if(VerticalTranslation.BOTTOM.equals(imageGenParams.getImageVerticalTranslation())) {
                // Top left will need to be drawn off-canvas
                // vertical offset will move image towards the top
                int yTranslation = (int) ((downloadedImage.getHeight() * scale) - background.getHeight());
                imageGraphics.drawImage(downloadedImage, 0, -yTranslation, null);
            }
            imageGraphics.dispose();
            return background;
        }
    }

    private Graphics2D setUpStatsGraphicsDefaults(BufferedImage image) {
        Graphics2D graphics = image.createGraphics();
        Font font = new Font("Chakra Petch", Font.BOLD, 30);
        Color textColor = Color.BLACK;
        graphics.setFont(font);
        graphics.setColor(textColor);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return graphics;
    }

    private void drawAllStatsPlayerName(BufferedImage image, Post post) {
        // Add player name to the image
        Graphics2D nameGraphic = image.createGraphics();
        nameGraphic.setFont(new Font("Nike Ithaca", Font.PLAIN, 47));
        nameGraphic.setColor(Color.BLACK);
        int playerNameX = 77;
        int playerNameY = 116;
        nameGraphic.drawString(post.getPlayer().getName().toUpperCase(), playerNameX, playerNameY);
        nameGraphic.dispose();
    }

    private ImageStatEntry generateDisplayedName(String displayStatName, Object value) {
        if (displayStatName.equals("minutesPlayed")) {
            displayStatName = "minutes";
        }
        if (displayStatName.contains("All")) {
            displayStatName = displayStatName.replace("All", "");
        }
        if (displayStatName.contains("Percentage")) {
            displayStatName = displayStatName.replace("Percentage", "");
            value = value + "%";
        }
        if (displayStatName.contains("gk")) {
            displayStatName = displayStatName.replace("gk", "");
            if (displayStatName.contains("Penalties")) {
                displayStatName = displayStatName.replace("Penalties", "Pens");
            }
        }
        displayStatName = displayStatName.replaceAll("(.)([A-Z])", "$1 $2");
        displayStatName = displayStatName.substring(0, 1).toUpperCase() + displayStatName.substring(1);
        return new ImageStatEntry(displayStatName, String.valueOf(value));
    }

    private String generateFileName(Post post, int createdImageCounter, PostType postType) {
        switch (postType) {
            case STANDOUT_STATS_POST -> {
                return post.getPlayer().getName().replaceAll(" ", "") + "_" + DateTimeHelper.getDateAsFormattedStringForFileName(post.getPlayerMatchPerformanceStats().getMatch().getDate()) + "_standout_stat_image_" + createdImageCounter + ".jpg";
            }
            case SUMMARY_POST -> {
                return DateTimeHelper.getDateAsFormattedStringForFileName(post.getDateGenerated()) + "_summary_image_" + createdImageCounter + ".jpg";
            }
            default -> {
                return post.getPlayer().getName().replaceAll(" ", "") + "_" + DateTimeHelper.getDateAsFormattedStringForFileName(post.getPlayerMatchPerformanceStats().getMatch().getDate()) + "_stat_image_" + createdImageCounter + ".jpg";
            }
        }
    }

    private void saveImage(Post post, BufferedImage image, String fileName, int createdImageCounter) throws IOException {
        String outputImageFilePath = imageGeneratorProperties.getOutputPath() + "/" + fileName;
        ImageIO.write(image, "jpg", new File(outputImageFilePath));
        post.getImagesFileNames().add(fileName);
        LogHelper.logWithSubject(log.atInfo().setMessage("Generated image " + createdImageCounter), post);
    }

    private static List<String> getZeroValueFilter() {
        List<String> zeroValueFilter = new ArrayList<>();
        zeroValueFilter.add("penaltiesScored");
        zeroValueFilter.add("penaltiesWon");
        zeroValueFilter.add("shotsOnTarget");
        zeroValueFilter.add("shotsBlocked");
        zeroValueFilter.add("yellowCards");
        zeroValueFilter.add("redCards");
        zeroValueFilter.add("fouled");
        zeroValueFilter.add("offsides");
        zeroValueFilter.add("crosses");
        zeroValueFilter.add("crossingAccuracyAll");
        zeroValueFilter.add("groundDuelsWon");
        zeroValueFilter.add("aerialDuelsWon");
        zeroValueFilter.add("xg");
        zeroValueFilter.add("xg_assist");
        zeroValueFilter.add("chancesCreatedAll");
        zeroValueFilter.add("gkPenaltiesAttemptedAgainst");
        zeroValueFilter.add("gkPenaltiesScoredAgainst");
        zeroValueFilter.add("gkPenaltiesSaved");
        return zeroValueFilter;
    }

    public void generateStandoutStatsImage(Post post, List<StatisticEntryGenerateDto> selectedStats, ImageGenParams imageGenParams) throws Exception {
        try {
            BufferedImage image;
            if (imageGenParams != null && imageGenParams.getImageUrl() != null && !imageGenParams.getImageUrl().isEmpty()) {
                // Download and set background image
                image = setUpBaseImageWithBackgroundImageUrl(imageGenParams);

                // Add gradient
                drawGradient(image);

                // Add account name
                Font accountNameFont = new Font("Nike Ithaca", Font.PLAIN, 20);
                drawAccountName(image, accountNameFont, "@" + instagramPostProperies.getAccountName());
            } else {
                // Load the base image
                String playerImageBaseFilePath = imageGeneratorProperties.getInputPath() + post.getPlayer().getName().replaceAll(" ", "") + STANDOUT_BASE_IMAGE_FILE_NAME;
                image = loadImage(playerImageBaseFilePath);
            }
            log.atDebug().setMessage("Prepared STANDOUT_STATS_POST image object").addKeyValue("player", post.getPlayer().getName()).log();

            // Add player name
            Font playerNameFont = new Font("Wagner Modern", Font.PLAIN, 50);
            if (!selectedStats.isEmpty()) {
                drawXCenteredText(image, playerNameFont, post.getPlayer().getName().toUpperCase(), image.getHeight() - 320);
            } else {
                drawXCenteredText(image, playerNameFont, post.getPlayer().getName().toUpperCase(), image.getHeight() - 100);
            }

            // Add match stats
            if (!selectedStats.isEmpty()){
                drawSplitStats(image, selectedStats);
            }

            // Add match name
            String matchName = PostHelper.generateMatchName(post.getPlayerMatchPerformanceStats());
            Font matchNameFont = new Font("Chakra Petch", Font.BOLD, 30);
            drawXCenteredText(image, matchNameFont, matchName, image.getHeight() - 60);

            // Add match date
            if (post.getPlayerMatchPerformanceStats().getMatch() != null && post.getPlayerMatchPerformanceStats().getMatch().getDate() != null) {
                String matchDateString = DateTimeHelper.getDateAsFormattedString(post.getPlayerMatchPerformanceStats().getMatch().getDate());
                Font matchDateFont = new Font("Chakra Petch", Font.BOLD, 20);
                drawXCenteredText(image, matchDateFont, matchDateString, image.getHeight() - 30);
            }
            log.atDebug().setMessage("Added text to STANDOUT_STATS_POST image").addKeyValue("player", post.getPlayer().getName()).log();

            // Save the modified image
            saveImage(post, image, generateFileName(post, 1, PostType.STANDOUT_STATS_POST), 1);
        } catch (IOException ex) {
            log.atWarn().setMessage("Unable to find/read image file").addKeyValue("player", post.getPlayer().getName()).log();
            throw new Exception(post.getPlayer().getName() + " - Unable to find/read image file ", ex);
        } catch (Exception ex) {
            log.atWarn().setMessage("Error while generating standout stat image").addKeyValue("player", post.getPlayer().getName()).log();
            throw new Exception(post.getPlayer().getName() + " - Error while generating stat image ", ex);
        }
    }

    public void generateSummaryImage(Post summaryPost, List<Post> playerPosts, ImageGenParams imageGenParams) throws Exception {
        try {
            BufferedImage image;

            ImageGenParams backgroundImageGenParams = new ImageGenParams();
            backgroundImageGenParams.setImageUrl(imageGeneratorProperties.getExternalImageStoreUri() + imageGeneratorProperties.getSummaryBaseImageFile());
            image = setUpBaseImageWithBackgroundImageUrl(backgroundImageGenParams);

            // Add individual player stats
            log.atInfo().setMessage("Adding player stats to summary image(s)").log();
            List<BufferedImage> outputImages = summaryDrawPlayerStats(image, playerPosts);

            // Add side image
            log.atInfo().setMessage("Adding side image to summary image(s)").log();
            summaryDrawSideImage(outputImages, imageGenParams);

            // Save the modified image
            if (outputImages.isEmpty()) {
                throw new Exception("No output images created");
            }
            int savedImagesCount = 0;
            for (BufferedImage imageToSave : outputImages) {
                savedImagesCount++;
                saveImage(summaryPost, imageToSave, generateFileName(summaryPost, savedImagesCount, PostType.SUMMARY_POST), savedImagesCount);
            }
        } catch (Exception ex) {
            log.atWarn().setMessage("Error while generating summary image").setCause(ex).log();
            throw new Exception("Summary image - Error while generating summary image ", ex);
        }
    }

    public void drawGradient(BufferedImage image) {
        Graphics2D gradientGraphics = image.createGraphics();
        int gradientHeight = 600;

        // Create a gradient paint from transparent to black
        GradientPaint gradientPaint = new GradientPaint(0, image.getHeight(), Color.BLACK, 0,
                image.getHeight() - gradientHeight, new Color(0, 0, 0, 0));

        // Set the paint to the gradient
        gradientGraphics.setPaint(gradientPaint);

        // Fill the entire image with the gradient
        gradientGraphics.fillRect(0, image.getHeight() - gradientHeight, image.getWidth(), gradientHeight);
        gradientGraphics.dispose();
    }

    public void drawAccountName(BufferedImage image, Font font, String text) {
        if (text != null) {
            Graphics2D textGraphic = image.createGraphics();
            textGraphic.setFont(font);
            textGraphic.setColor(Color.WHITE);
            FontMetrics metrics = textGraphic.getFontMetrics(font);
            // Calculate X coordinate
            int x = image.getWidth() - metrics.stringWidth(text) - 30;
            int y = 30;
            textGraphic.drawString(text, x, y);
            textGraphic.dispose();
        }
    }

    public void drawXCenteredText(BufferedImage image, Font font, String text, int y) {
        drawXCenteredText(image, image.getWidth(), font, text, y, Color.WHITE);
    }

    public void drawXCenteredText(BufferedImage image, int drawingAreaWidth, Font font, String text, int y, Color color) {
        Graphics2D nameGraphic = image.createGraphics();
        nameGraphic.setFont(font);
        nameGraphic.setColor(color);
        FontMetrics metrics = nameGraphic.getFontMetrics(font);
        // Calculate X coordinate
        int x = (drawingAreaWidth - metrics.stringWidth(text)) / 2;
        nameGraphic.drawString(text, x, y);
        nameGraphic.dispose();
    }

    public void drawSplitStats(BufferedImage image, List<StatisticEntryGenerateDto> selectedStats) {
        int numSelectedStats = selectedStats.size();
        // Add buffer on edges of image
        int edgeBuffer = 10;
        if (numSelectedStats < 3 && image.getWidth() > 900) {
            edgeBuffer = 200;
        }
        int individualPartitionSize = (image.getWidth() - (edgeBuffer * 2)) / numSelectedStats;

        // Set up stat name graphics
        Graphics2D statNameGraphic = image.createGraphics();
        Font statNameFont = new Font("Chakra Petch", Font.BOLD, 30);
        statNameGraphic.setFont(statNameFont);
        statNameGraphic.setColor(Color.WHITE);
        FontMetrics statNameMetrics = statNameGraphic.getFontMetrics(statNameFont);

        // Set up stat value graphics
        Graphics2D statValueGraphic = image.createGraphics();
        Font statValueFont = new Font("Chakra Petch", Font.BOLD, 160);
        statValueGraphic.setFont(statValueFont);
        statValueGraphic.setColor(Color.WHITE);
        FontMetrics statValueMetrics = statValueGraphic.getFontMetrics(statValueFont);

        // Handle multiple stats by equally splitting the image size
        // Starting from left to right to draw each selected stat
        for (int i = 0; i < numSelectedStats; i++) {
            int partitionTranslation = individualPartitionSize * i;
            ImageStatEntry imageStatEntry = generateDisplayedName(selectedStats.get(i).getName(), selectedStats.get(i).getValue());
            int nameY = image.getHeight()-120, valueY = image.getHeight()-170;
            int nameX = (individualPartitionSize - statNameMetrics.stringWidth(imageStatEntry.getName())) / 2 + partitionTranslation + edgeBuffer;
            statNameGraphic.drawString(imageStatEntry.getName(), nameX, nameY);
            int valueX = (individualPartitionSize - statValueMetrics.stringWidth(imageStatEntry.getValue())) / 2 + partitionTranslation + edgeBuffer;
            statValueGraphic.drawString(imageStatEntry.getValue(), valueX, valueY);
        }
        statNameGraphic.dispose();
        statValueGraphic.dispose();
    }

    public List<BufferedImage> summaryDrawPlayerStats(BufferedImage image, List<Post> playerPosts) {
        BufferedImage currentImage = copyBufferedImage(image);
        Graphics2D playerNameGraphic = currentImage.createGraphics();
        Font playerNameFont = new Font("Chakra Petch", Font.BOLD, 30);
        playerNameGraphic.setFont(playerNameFont);

        int imageHeight = 1000;
        int teamLogoOffsetFromNameY = 35;
        int playerNameX = 130;
        int playerMinutesPlayedX = 550;
        int statsDrawingAreaWidth = 650;

        // Dynamic
        int currentNameY = 200;
        String previousTeam = null;

        List<BufferedImage> outputImages = new ArrayList<>();

        for (int i = 0; i < playerPosts.size(); i++) {
            Post playerPost = playerPosts.get(i);
            if (playerPost.getPlayer() != null
                    && playerPost.getPlayerMatchPerformanceStats() != null
                    && playerPost.getPlayerMatchPerformanceStats().getMatch() != null
                    && playerPost.getPlayerMatchPerformanceStats().getMatch().getRelevantTeam() != null) {
                // Add separator and add match name
                if (!playerPost.getPlayerMatchPerformanceStats().getMatch().getRelevantTeam().equals(previousTeam)) {
                    playerNameGraphic.setColor(Color.GRAY);
                    int separatorY = currentNameY - 10;
                    playerNameGraphic.drawLine(50, separatorY, statsDrawingAreaWidth - 50, separatorY);
                    currentNameY += 20;
                    String matchName = PostHelper.generateMatchNameWithSuffixRemoved(playerPost.getPlayerMatchPerformanceStats(), teamProperties) + " " + PostHelper.generateMatchScore(playerPost.getPlayerMatchPerformanceStats());
                    Font matchNameFont = new Font("Chakra Petch", Font.BOLD, 20);
                    drawXCenteredText(currentImage, statsDrawingAreaWidth, matchNameFont, matchName, currentNameY, Color.GRAY);
                    currentNameY += 50;
                }

                try {
                    // Draw team logo for player
                    Team team = teamHelpers.findTeamByNameOrAlternativeName(playerPost.getPlayerMatchPerformanceStats().getMatch().getRelevantTeam());
                    if (team != null) {
                        BufferedImage teamLogo = getTeamLogo(team.getLogoFileName());
                        Graphics2D imageGraphics = currentImage.createGraphics();
                        imageGraphics.drawImage(teamLogo, 70, currentNameY - teamLogoOffsetFromNameY, null);
                        imageGraphics.dispose();
                        log.atInfo().setMessage("Team logo added to summary image").addKeyValue("player", playerPost.getPlayer().getName()).log();
                    } else {
                        log.atWarn().setMessage("Unable to determine team to draw team logo").addKeyValue("player", playerPost.getPlayer().getName()).log();
                    }
                } catch (IOException e) {
                    log.atWarn().setMessage("URL to team logo was invalid").addKeyValue("player", playerPost.getPlayer().getName()).log();
                }

                playerNameGraphic.setColor(Color.BLACK);
                playerNameGraphic.drawString(playerPost.getPlayer().getName(), playerNameX, currentNameY);
                playerNameGraphic.drawString(String.valueOf(playerPost.getPlayerMatchPerformanceStats().getMinutesPlayed()), playerMinutesPlayedX, currentNameY);

                // Draw goal, assist, cards icons and count
                boolean statsAdded = drawSummaryPlayerStats(currentImage, playerPost.getPlayerMatchPerformanceStats(), currentNameY);
                if (statsAdded) {
                    currentNameY += 105;
                } else {
                    currentNameY += 65;
                }

                previousTeam = playerPost.getPlayerMatchPerformanceStats().getMatch().getRelevantTeam();

                // Check if there is any space left to add another entry
                if (currentNameY > (imageHeight - 80)) {
                    outputImages.add(currentImage);
                    currentImage = copyBufferedImage(image);
                    playerNameGraphic = currentImage.createGraphics();
                    playerNameGraphic.setFont(playerNameFont);
                    currentNameY = 200;
                    previousTeam = null;
                }
            }
            if (i + 1 == playerPosts.size()) {
                outputImages.add(currentImage);
            }
        }
        playerNameGraphic.dispose();
        return outputImages;
    }

    public boolean drawSummaryPlayerStats(BufferedImage image, PlayerMatchPerformanceStats playerMatchPerformanceStats, int currentY) {
        try {
            Graphics2D imageGraphics = image.createGraphics();
            Font playerNameFont = new Font("Chakra Petch", Font.PLAIN, 30);
            imageGraphics.setFont(playerNameFont);
            imageGraphics.setColor(Color.BLACK);

            // Format will be [icon1]x[value1] [icon2]x[value2] etc
            int currentStatX = 520;
            int statValueXMinimumOffset = 45;

            // Place stats under minutes played
            currentY += 50;
            boolean isAddedStats = false;

            // Red cards
            if (playerMatchPerformanceStats.getRedCards() != null && playerMatchPerformanceStats.getRedCards() > 0) {
                drawSummaryPlayerStatWithIcon(imageGraphics,
                        RED_CARD_ICON_FILE_NAME,
                        "x" + playerMatchPerformanceStats.getRedCards(),
                        currentStatX,
                        statValueXMinimumOffset,
                        currentY);
                currentStatX -= 80;
                isAddedStats = true;
            }

            // Yellow cards
            if (playerMatchPerformanceStats.getYellowCards() != null && playerMatchPerformanceStats.getYellowCards() > 0) {
                drawSummaryPlayerStatWithIcon(imageGraphics,
                        YELLOW_CARD_ICON_FILE_NAME,
                        "x" + playerMatchPerformanceStats.getYellowCards(),
                        currentStatX,
                        statValueXMinimumOffset,
                        currentY);
                currentStatX -= 80;
                isAddedStats = true;
            }

            // Assists
            if (playerMatchPerformanceStats.getAssists() != null && playerMatchPerformanceStats.getAssists() > 0) {
                drawSummaryPlayerStatWithIcon(imageGraphics,
                        ASSIST_ICON_FILE_NAME,
                        "x" + playerMatchPerformanceStats.getAssists(),
                        currentStatX,
                        statValueXMinimumOffset,
                        currentY);
                currentStatX -= 80;
                isAddedStats = true;
            }

            // Goals
            if (playerMatchPerformanceStats.getGoals() != null && playerMatchPerformanceStats.getGoals() > 0) {
                drawSummaryPlayerStatWithIcon(imageGraphics,
                        GOAL_ICON_FILE_NAME,
                        "x" + playerMatchPerformanceStats.getGoals(),
                        currentStatX,
                        statValueXMinimumOffset,
                        currentY);
                isAddedStats = true;
            }

            imageGraphics.dispose();
            return isAddedStats;
        } catch (IOException e) {
            log.atError().setMessage("Failure while adding stat icons").log();
            throw new RuntimeException(e);
        }
    }

    private void drawSummaryPlayerStatWithIcon(Graphics2D imageGraphics, String iconFileName, String statValue, int currentStatX, int statValueXOffset, int currentY) throws IOException {
        BufferedImage downloadedImage = getIcons(iconFileName);
        imageGraphics.drawImage(downloadedImage, currentStatX, currentY - 35, null);
        imageGraphics.drawString(statValue, currentStatX + statValueXOffset, currentY);
    }

    public void summaryDrawSideImage(List<BufferedImage> outputImages, ImageGenParams imageGenParams) throws IOException {
        int statsDrawingAreaWidth = 650;
        if (imageGenParams == null) {
            throw new RuntimeException("Missing imageGenParams");
        }

        String sideImageUri = imageGenParams.getImageUrl();
        if (sideImageUri == null || sideImageUri.isEmpty()) {
            // Use fallback image
            sideImageUri = imageGeneratorProperties.getExternalImageStoreUri() + "/default_side_image.jpg";
        }
        URL imageUrl = URI.create(sideImageUri).toURL();
        BufferedImage downloadedImage = ImageIO.read(imageUrl);

        // Determine the scaling factor to make the height 1000px
        int targetHeight = 1000;
        int originalWidth = downloadedImage.getWidth();
        int originalHeight = downloadedImage.getHeight();
        int scaledWidth = (int) ((double) targetHeight / originalHeight * originalWidth);

        // Scale the image
        Image tempScaledImage = downloadedImage.getScaledInstance(scaledWidth, targetHeight, Image.SCALE_SMOOTH);

        // Convert scaled Image back to BufferedImage
        BufferedImage scaledImage = new BufferedImage(scaledWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.drawImage(tempScaledImage, 0, 0, null);
        g2d.dispose();

        // Crop image
        int horizontalOffset = (imageGenParams.getImageHorizontalOffset() != null ? imageGenParams.getImageHorizontalOffset() : 0);
        BufferedImage croppedImage = scaledImage.getSubimage(horizontalOffset, 0, 1000 - statsDrawingAreaWidth, 1000);

        int imageCount = 1;
        for (BufferedImage image : outputImages) {
            Graphics2D imageGraphics = image.createGraphics();
            imageGraphics.drawImage(croppedImage, statsDrawingAreaWidth, 0, null);
            imageGraphics.dispose();
            log.atInfo().setMessage("Added side image to summary image " + imageCount).log();
            imageCount++;
        }
    }

    public static BufferedImage copyBufferedImage(BufferedImage original) {
        // Create a new BufferedImage with the same dimensions and type as the original
        BufferedImage copy = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());

        // Create a Graphics2D object to draw the original image onto the new image
        Graphics2D g2d = copy.createGraphics();
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();

        return copy;
    }

    public BufferedImage getTeamLogo(String logoFileName) throws IOException {
        return getImageUsingCache(logoFileName, imageGeneratorProperties.getExternalImageStoreUri() + TEAM_LOGO_DIRECTORY + "/", teamLogoCache);
    }

    public BufferedImage getIcons(String iconFileName) throws IOException {
        return getImageUsingCache(iconFileName, imageGeneratorProperties.getExternalImageStoreUri() + ICON_DIRECTORY + "/", iconCache);
    }

    public BufferedImage getImageUsingCache(String fileName, String fileLocation, Map<String, BufferedImage> cache) throws IOException {
        // Check in-memory cache to reduce downloads
        if (fileName == null) {
            throw new IOException("No filename defined");
        }
        BufferedImage cachedImage = null;
        // Check cache for image
        if (cache != null) {
            cachedImage = cache.get(fileName);
        }
        // Download image and store in cache
        if (cachedImage == null) {
            URL imageUrl = URI.create(fileLocation + fileName).toURL();
            log.atInfo().setMessage("Attempting to retrieve image - " + imageUrl).log();
            cachedImage = ImageIO.read(imageUrl);
            if (cache != null) {
                log.atInfo().setMessage("Stored image in cache - " + fileName).log();
                cache.put(fileName, cachedImage);
            }
        } else {
            log.atInfo().setMessage("Using image in cache - " + fileName).log();
        }
        return cachedImage;
    }

    public static BufferedImage cloneBufferImage(BufferedImage image) throws Exception {
        if (image == null) {
            throw new Exception("Unable to clone image");
        }
        BufferedImage clone = new BufferedImage(image.getWidth(),
                image.getHeight(), image.getType());
        Graphics2D g2d = clone.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return clone;
    }
}
