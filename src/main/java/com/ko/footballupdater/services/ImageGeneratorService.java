package com.ko.footballupdater.services;

import com.ko.footballupdater.configuration.ImageGeneratorProperies;
import com.ko.footballupdater.models.ImageStatEntry;
import com.ko.footballupdater.models.InstagramPostHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ImageGeneratorService {

    @Autowired
    private ImageGeneratorProperies imageGeneratorProperies;

    private final int STAT_Y_COORDINATE = 350;
    private final String BASE_IMAGE_FILE_NAME = "_base_player_stat_image.jpg";

    public void generatePlayerStatImage(InstagramPostHolder postHolder) throws Exception {
        if (!imageGeneratorProperies.isEnabled()) {
            return;
        }

        try {
            // Load the base image
            String playerImageBaseFilePath = imageGeneratorProperies.getInputPath() + postHolder.getPost().getPlayer().getName().replaceAll(" ", "") + BASE_IMAGE_FILE_NAME;
            BufferedImage image = setUpBaseImage(playerImageBaseFilePath, postHolder);

            // Match stats to image
            Graphics2D graphics = setUpStatsGraphicsDefaults(image);
            // Starting coordinate for first row
            int statNameX = 79;
            int statValueX = 450;
            int statY = STAT_Y_COORDINATE;

            int attributeCounter = 0;
            int createdImageCounter = 0;
            for (Field field : postHolder.getPlayerMatchPerformanceStats().getClass().getDeclaredFields()) {
                field.setAccessible(true); // Make the private field accessible
                try {
                    Object value = field.get(postHolder.getPlayerMatchPerformanceStats()); // Get the field's value
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
                            saveImage(postHolder, image, createdImageCounter);
                            image = setUpBaseImage(playerImageBaseFilePath, postHolder);
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
            saveImage(postHolder, image, createdImageCounter);
        } catch (Exception ex) {
            log.atWarn().setMessage("Error while generating stat image").setCause(ex).addKeyValue("player", postHolder.getPost().getPlayer().getName()).log();
            throw new Exception(postHolder.getPost().getPlayer().getName() + " - Error while generating stat image ", ex);
        }
    }

    private BufferedImage setUpBaseImage(String baseImagePath, InstagramPostHolder postHolder) throws IOException {
        BufferedImage baseImage = ImageIO.read(new File(baseImagePath));
        Graphics2D graphics = baseImage.createGraphics();

        // Add player name to the image
        graphics.setFont(new Font("Nike Ithaca", Font.PLAIN, 47));
        graphics.setColor(Color.BLACK);
        int playerNameX = 77;
        int playerNameY = 116;
        graphics.drawString(postHolder.getPost().getPlayer().getName().toUpperCase(), playerNameX, playerNameY);
        graphics.dispose();
        return baseImage;
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

    private void saveImage(InstagramPostHolder postHolder, BufferedImage image, int createdImageCounter) throws IOException {
        String fileName = postHolder.getPost().getPlayer().getName().replaceAll(" ", "") + "_" + postHolder.getPlayerMatchPerformanceStats().getMatch().getDateAsFormattedStringForFileName() + "_stat_image_" + createdImageCounter + ".jpg";
        String outputImageFilePath = imageGeneratorProperies.getOutputPath() + fileName;
        ImageIO.write(image, "jpg", new File(outputImageFilePath));
        postHolder.getImagesFileNames().add(fileName);
        log.atInfo().setMessage("Generated stat image " + createdImageCounter).addKeyValue("player", postHolder.getPost().getPlayer().getName()).log();
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
}
