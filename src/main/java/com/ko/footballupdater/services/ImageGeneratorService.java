package com.ko.footballupdater.services;

import com.ko.footballupdater.configuration.ImageGeneratorProperies;
import com.ko.footballupdater.configuration.InstagramPostProperies;
import com.ko.footballupdater.models.ImageStatEntry;
import com.ko.footballupdater.models.PostType;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.form.StatisticEntryGenerateDto;
import com.ko.footballupdater.utils.DateTimeHelper;
import com.ko.footballupdater.utils.PostHelper;
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
import java.util.List;

@Slf4j
@Service
public class ImageGeneratorService {

    @Autowired
    private ImageGeneratorProperies imageGeneratorProperies;

    @Autowired
    private InstagramPostProperies instagramPostProperies;

    private final int STAT_Y_COORDINATE = 350;
    private final String BASE_IMAGE_FILE_NAME = "_base_player_stat_image.jpg";
    private final String STANDOUT_BASE_IMAGE_FILE_NAME = "_standout_base_player_stat_image.jpg";

    public void generatePlayerStatImage(Post post) throws Exception {
        if (!imageGeneratorProperies.isEnabled()) {
            return;
        }

        try {
            // Load the base image
            String playerImageBaseFilePath = imageGeneratorProperies.getInputPath() + post.getPlayer().getName().replaceAll(" ", "") + BASE_IMAGE_FILE_NAME;
            BufferedImage image = setUpBaseImage(playerImageBaseFilePath);

            // Add player name to the image
            Graphics2D nameGraphic = image.createGraphics();
            nameGraphic.setFont(new Font("Nike Ithaca", Font.PLAIN, 47));
            nameGraphic.setColor(Color.BLACK);
            int playerNameX = 77;
            int playerNameY = 116;
            nameGraphic.drawString(post.getPlayer().getName().toUpperCase(), playerNameX, playerNameY);
            nameGraphic.dispose();

            // Match stats to image
            Graphics2D graphics = setUpStatsGraphicsDefaults(image);
            // Starting coordinate for first row
            int statNameX = 79;
            int statValueX = 450;
            int statY = STAT_Y_COORDINATE;

            int attributeCounter = 0;
            int createdImageCounter = 0;
            for (Field field : post.getPlayerMatchPerformanceStats().getClass().getDeclaredFields()) {
                field.setAccessible(true); // Make the private field accessible
                try {
                    Object value = field.get(post.getPlayerMatchPerformanceStats()); // Get the field's value
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
                            image = setUpBaseImage(playerImageBaseFilePath);
                            graphics = setUpStatsGraphicsDefaults(image);
                            statY = STAT_Y_COORDINATE;
                        }
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
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

    private BufferedImage setUpBaseImage(String baseImagePath) throws IOException {
        return ImageIO.read(new File(baseImagePath));
    }

    private BufferedImage setUpBaseImageWithBackgroundImageUrl(String backgroundImageUrl, Boolean forceScaleImage) throws IOException {
        URL imageUrl = URI.create(backgroundImageUrl).toURL();
        BufferedImage downloadedImage = ImageIO.read(imageUrl);

        // Scale image down to height of 1000 - change width proportionally
        float scale = 1;
        if (forceScaleImage || downloadedImage.getHeight() > 1000) {
            scale = (float) 1000 /downloadedImage.getHeight();
        }
        BufferedImage background = new BufferedImage((int) (scale * downloadedImage.getWidth()), (int) (scale * downloadedImage.getHeight()), BufferedImage.TYPE_INT_RGB);
        Graphics2D imageGraphics = background.createGraphics();
        imageGraphics.scale(scale, scale);
        imageGraphics.drawImage(downloadedImage, 0 , 0, null);
        imageGraphics.dispose();

        return background;
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
            default -> {
                return post.getPlayer().getName().replaceAll(" ", "") + "_" + DateTimeHelper.getDateAsFormattedStringForFileName(post.getPlayerMatchPerformanceStats().getMatch().getDate()) + "_stat_image_" + createdImageCounter + ".jpg";
            }
        }
    }

    private void saveImage(Post post, BufferedImage image, String fileName, int createdImageCounter) throws IOException {
        String outputImageFilePath = imageGeneratorProperies.getOutputPath() + fileName;
        ImageIO.write(image, "jpg", new File(outputImageFilePath));
        post.getImagesFileNames().add(fileName);
        log.atInfo().setMessage("Generated image " + createdImageCounter).addKeyValue("player", post.getPlayer().getName()).log();
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

    public void generateStandoutStatsImage(Post post, List<StatisticEntryGenerateDto> selectedStats, String backgroundImageUrl, Boolean forceScaleImage) throws Exception {
        try {
            BufferedImage image;
            if (!backgroundImageUrl.isEmpty()) {
                // Download and set background image
                image = setUpBaseImageWithBackgroundImageUrl(backgroundImageUrl, forceScaleImage);

                // Add gradient
                drawGradient(image);

                // Add account name
                Font accountNameFont = new Font("Nike Ithaca", Font.PLAIN, 20);
                drawAccountName(image, accountNameFont, "@" + instagramPostProperies.getAccountName());
            } else {
                // Load the base image
                String playerImageBaseFilePath = imageGeneratorProperies.getInputPath() + post.getPlayer().getName().replaceAll(" ", "") + STANDOUT_BASE_IMAGE_FILE_NAME;
                image = setUpBaseImage(playerImageBaseFilePath);
            }

            // Add player name
            Font nikeIthacaFont = new Font("Nike Ithaca", Font.PLAIN, 47);
            drawXCenteredText(image, nikeIthacaFont, post.getPlayer().getName().toUpperCase(), image.getHeight() - 350);

            // Add match stats
            drawSplitStats(image, selectedStats);

            // Add match name
            String matchName = PostHelper.generateMatchName(post.getPlayerMatchPerformanceStats());
            Font matchNameFont = new Font("Chakra Petch", Font.BOLD, 30);
            drawXCenteredText(image, matchNameFont, matchName, image.getHeight() - 60);

            // Add match date
            String matchDateString = DateTimeHelper.getDateAsFormattedString(post.getPlayerMatchPerformanceStats().getMatch().getDate());
            Font matchDateFont = new Font("Chakra Petch", Font.BOLD, 20);
            drawXCenteredText(image, matchDateFont, matchDateString, image.getHeight() - 30);

            // Save the modified image
            saveImage(post, image, generateFileName(post, 1, PostType.STANDOUT_STATS_POST), 1);
        } catch (Exception ex) {
            log.atWarn().setMessage("Error while generating standout stat image").setCause(ex).addKeyValue("player", post.getPlayer().getName()).log();
            throw new Exception(post.getPlayer().getName() + " - Error while generating stat image ", ex);
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
        Graphics2D nameGraphic = image.createGraphics();
        nameGraphic.setFont(font);
        nameGraphic.setColor(Color.WHITE);
        FontMetrics metrics = nameGraphic.getFontMetrics(font);
        // Calculate X coordinate
        int x = (image.getWidth() - metrics.stringWidth(text)) / 2;
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
}
