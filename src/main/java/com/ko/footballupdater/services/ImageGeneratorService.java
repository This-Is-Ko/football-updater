package com.ko.footballupdater.services;

import com.ko.footballupdater.configuration.ImageGeneratorProperies;
import com.ko.footballupdater.models.ImageStatEntry;
import com.ko.footballupdater.models.InstagramPost;
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

    public void generatePlayerStatImage(InstagramPost post) {
        if (!imageGeneratorProperies.isEnabled()) {
            return;
        }

        try {
            // Load the base image
            String playerImageBaseFilePath = imageGeneratorProperies.getInputPath() + post.getPlayer().getName().replaceAll(" ", "") + BASE_IMAGE_FILE_NAME;
            BufferedImage image = setUpBaseImage(playerImageBaseFilePath, post);

            // Match stats to image
            Graphics2D graphics = setUpStatsGraphicsDefaults(image);
            // Starting coordinate for first row
            int statNameX = 79;
            int statValueX = 450;
            int statY = STAT_Y_COORDINATE;

            int attributeCounter = 0;
            int createdImageCounter = 0;
//            Map<String, Object> attributeValueMap = new HashMap<>();
            for (Field field : post.getPlayerMatchPerformanceStats().getClass().getDeclaredFields()) {
                field.setAccessible(true); // Make the private field accessible
                try {
                    Object value = field.get(post.getPlayerMatchPerformanceStats()); // Get the field's value
                    if (value != null && !field.getName().equals("match")) {
                        List<String> zeroValueFilter = getZeroValueFilter();
                        if (zeroValueFilter.contains(field.getName()) && value.equals(0)) {
                            continue;
                        }
                        // Generate proper stat name - capitalise words and spacing
                        ImageStatEntry imageStatEntry = generateDisplayedName(field.getName(), value);

//                        attributeValueMap.put(field.getName(), value);
                        graphics.drawString(imageStatEntry.getName(), statNameX, statY);
                        graphics.drawString(imageStatEntry.getValue(), statValueX, statY);
                        // Shift y coordinate down to next position
                        statY += 51;
                        attributeCounter++;
                        // 12 Rows on one image
                        // Once max stats for one image is added, generate new image
                        if (attributeCounter % 12 == 0) {
                            createdImageCounter++;
                            saveImage(post, image, createdImageCounter);
                            image = setUpBaseImage(playerImageBaseFilePath, post);
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
            saveImage(post, image, createdImageCounter);
        } catch (Exception ex) {
            log.warn(post.getPlayer().getName() + " - Error while generating stat image ", ex);
        }
    }

    private BufferedImage setUpBaseImage(String baseImagePath, InstagramPost post) throws IOException {
        BufferedImage baseImage = ImageIO.read(new File(baseImagePath));
        Graphics2D graphics = baseImage.createGraphics();

        // Add player name to the image
        graphics.setFont(new Font("Nike Ithaca", Font.PLAIN, 47));
        graphics.setColor(Color.BLACK);
        int playerNameX = 77;
        int playerNameY = 116;
        graphics.drawString(post.getPlayer().getName().toUpperCase(), playerNameX, playerNameY);
        graphics.dispose();
        return baseImage;
    }

    private Graphics2D setUpStatsGraphicsDefaults(BufferedImage image) {
        Graphics2D graphics = image.createGraphics();
        Font font = new Font("Chakra Petch", Font.BOLD, 30);
        Color textColor = Color.BLACK;
        graphics.setFont(font);
        graphics.setColor(textColor);
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

    private void saveImage(InstagramPost post, BufferedImage image, int createdImageCounter) throws IOException {
        String fileName = post.getPlayer().getName().replaceAll(" ", "") + "_" + post.getPlayerMatchPerformanceStats().getMatch().getDateAsFormattedStringForFileName() + "_stat_image_" + createdImageCounter + ".jpg";
        String outputImageFilePath = imageGeneratorProperies.getOutputPath() + fileName;
        ImageIO.write(image, "jpg", new File(outputImageFilePath));
        post.getImagesFileNames().add(fileName);
        log.atInfo().setMessage(post.getPlayer().getName() + " - Generated stat image " + createdImageCounter).addKeyValue("player", post.getPlayer().getName()).log();
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
