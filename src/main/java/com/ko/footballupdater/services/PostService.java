package com.ko.footballupdater.services;


import com.ko.footballupdater.configuration.InstagramPostProperies;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.PostType;
import com.ko.footballupdater.models.form.PreparePostDto;
import com.ko.footballupdater.models.form.StatisticEntryGenerateDto;
import com.ko.footballupdater.repositories.PlayerRepository;
import com.ko.footballupdater.repositories.PostRepository;
import com.ko.footballupdater.utils.PostHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private ParsingService parsingService;

    @Autowired
    private ImageGeneratorService imageGeneratorService;

    @Autowired
    private AmazonS3Service amazonS3Service;

    @Autowired
    private InstagramPostProperies instagramPostProperies;

    public List<Post> getPosts() {
        return postRepository.findAllByOrderByDateGeneratedDesc();
    }

    public void updatePostPostedStatus(List<Post> postsToSave) {
        postsToSave.forEach(post -> postRepository.updatePostSetPostedStatusForId(post.isPostedStatus(), post.getId()));
    }

    public void deletePost(Integer postId) throws Exception {
        postRepository.deleteById(postId);
        if (postRepository.existsById(postId)) {
            throw new Exception("Post was not deleted");
        }
    }

    public PreparePostDto prepareDtoForGeneratePost(Integer postId) throws Exception {
        PreparePostDto preparePostDto = new PreparePostDto();

        // Search for post with id
        Optional<Post> postSearchResult = postRepository.findById(postId);
        if (postSearchResult.isEmpty()) {
            throw new Exception("Post id not found");
        }
        Post post = postSearchResult.get();

        HashMap<String, String> availableStatMap = new HashMap<>();
        List<StatisticEntryGenerateDto> allStats = new ArrayList<>();
        if (post.getPlayerMatchPerformanceStats() == null) {
            throw new Exception("Post doesn't contain match performance stats object");
        }
        for (Field field : post.getPlayerMatchPerformanceStats().getClass().getDeclaredFields()) {
            field.setAccessible(true); // Make the private field accessible
            try {
                // Skip match object
                if (field.getName().equals("match") || field.getName().equals("id")) {
                    continue;
                }
                Object value = field.get(post.getPlayerMatchPerformanceStats()); // Get the field's value
                if (value != null) {
                    allStats.add(new StatisticEntryGenerateDto(field.getName(), value.toString(), false));
                    availableStatMap.put(field.getName(), value.toString());
                }
            } catch (IllegalAccessException ex) {
                log.atError().setMessage("Error while converting stat fields and values to map").setCause(ex).log();
            }
        }
        preparePostDto.setAvailableStatMap(availableStatMap);
        preparePostDto.setAllStats(allStats);
        preparePostDto.setSelectedStats(new ArrayList<>());

        return preparePostDto;
    }

    public Boolean generateStandoutPost(Integer postId, List<StatisticEntryGenerateDto> allStats, String backgroundImageUrl) throws Exception {
        // Search for post with id
        Optional<Post> postSearchResult = postRepository.findById(postId);
        if (postSearchResult.isEmpty()) {
            throw new Exception("Post id not found");
        }
        Post existingPost = postSearchResult.get();
        Post newPost = new Post(PostType.STANDOUT_STATS_POST, existingPost.getPlayer(), existingPost.getPlayerMatchPerformanceStats());

        // Only use selected stats
        List<StatisticEntryGenerateDto> filteredStats = allStats.stream()
                .filter(StatisticEntryGenerateDto::isSelected)
                .toList();

        if (filteredStats.isEmpty()) {
            throw new Exception("No stat selected, unable to generate standout stat image");
        }

        try {
            imageGeneratorService.generateStandoutStatsImage(newPost, filteredStats, backgroundImageUrl);
            // Upload stat images to s3
            amazonS3Service.uploadtoS3(newPost);
            // Generate caption
            PostHelper.generatePostCaption(instagramPostProperies.getVersion(), newPost);
            // Save post
            postRepository.save(newPost);
            log.atInfo().setMessage("Successfully created standout stat image and saved").addKeyValue("player", newPost.getPlayer().getName()).log();
        } catch (Exception e) {
            // Skip if image generation or upload fails, allows future retry
            log.warn(newPost.getPlayer().getName() + " - Unable to generate or upload image");
        }
        return true;
    }
}
