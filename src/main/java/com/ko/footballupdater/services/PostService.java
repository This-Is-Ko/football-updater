package com.ko.footballupdater.services;


import com.ko.footballupdater.configuration.InstagramPostProperies;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.PostType;
import com.ko.footballupdater.models.form.ImageUrlEntry;
import com.ko.footballupdater.models.form.PrepareStandoutImageDto;
import com.ko.footballupdater.models.form.StatisticEntryGenerateDto;
import com.ko.footballupdater.models.form.UploadPostDto;
import com.ko.footballupdater.repositories.PlayerRepository;
import com.ko.footballupdater.repositories.PostRepository;
import com.ko.footballupdater.utils.PostHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ko.footballupdater.utils.PostHelper.generatePostImageSearchUrl;

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
    private FacebookApiService facebookApiService;

    @Autowired
    private InstagramPostProperies instagramPostProperies;

    final Map<String, String> CUSTOM_FIELD_NAME_MAPPING = Map.of(
            "goals", "goal",
            "assists", "assist",
            "yellowCards", "yellowCard",
            "redCards", "redCard"
    );


    public List<Post> getPosts(Boolean postedStatus) {
        if (postedStatus != null) {
            return postRepository.findByPostedStatusOrderByDateGeneratedDesc(postedStatus);
        }
        return postRepository.findAllByOrderByDateGeneratedDesc();
    }

    public Post getPostById(Integer postId) throws Exception {
        Optional<Post> postSearchResult = postRepository.findById(postId);
        if (postSearchResult.isEmpty()) {
            throw new Exception("Post id not found");
        }
        return postSearchResult.get();
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

    public PrepareStandoutImageDto prepareDtoForGeneratePost(Integer postId) throws Exception {
        PrepareStandoutImageDto prepareStandoutImageDto = new PrepareStandoutImageDto();

        // Search for post with id
        Optional<Post> postSearchResult = postRepository.findById(postId);
        if (postSearchResult.isEmpty()) {
            throw new Exception("Post id not found");
        }
        Post post = postSearchResult.get();
        generatePostImageSearchUrl(post);
        prepareStandoutImageDto.setPost(post);

        List<StatisticEntryGenerateDto> allStats = new ArrayList<>();
        if (post.getPlayerMatchPerformanceStats() == null) {
            throw new Exception("Post doesn't contain match performance stats object");
        }
        for (Field field : post.getPlayerMatchPerformanceStats().getClass().getDeclaredFields()) {
            field.setAccessible(true); // Make the private field accessible
            String fieldName = field.getName();
            try {
                // Skip match object
                if (fieldName.equals("match") || fieldName.equals("id")) {
                    continue;
                }
                Object value = field.get(post.getPlayerMatchPerformanceStats()); // Get the field's value
                if (value != null) {
                    // Change name for grammar
                    if (CUSTOM_FIELD_NAME_MAPPING.containsKey(fieldName) && (int) value == 1) {
                        fieldName = CUSTOM_FIELD_NAME_MAPPING.get(fieldName);
                    }
                    allStats.add(new StatisticEntryGenerateDto(fieldName, value.toString(), false));
                }
            } catch (IllegalAccessException ex) {
                log.atError().setMessage("Error while converting stat fields and values to map").setCause(ex).log();
            }
        }
        prepareStandoutImageDto.setAllStats(allStats);

        return prepareStandoutImageDto;
    }

    public void generateStandoutPost(PrepareStandoutImageDto prepareStandoutImageDto) throws Exception {
        // Search for post with id
        Optional<Post> postSearchResult = postRepository.findById(prepareStandoutImageDto.getPostId());
        if (postSearchResult.isEmpty()) {
            throw new Exception("Post id not found");
        }

        // Update existing post type to reflect new type
        Post post = postSearchResult.get();
        post.setPostType(PostType.STANDOUT_STATS_POST);
        post.setImagesUrls(new ArrayList<>(post.getImagesUrls()));

        // Only use selected stats
        List<StatisticEntryGenerateDto> filteredStats = prepareStandoutImageDto.getAllStats().stream()
                .filter(StatisticEntryGenerateDto::isSelected)
                .toList();

        if (filteredStats.isEmpty()) {
            prepareStandoutImageDto.setPost(post);
            log.atInfo().setMessage("No stat selected, unable to generate standout stat image").log();
        }

        try {
            // Generate standout post image
            imageGeneratorService.generateStandoutStatsImage(post, filteredStats, prepareStandoutImageDto.getImageGenParams());
            // Upload stat images to s3
            amazonS3Service.uploadToS3(post, true);
            // Generate caption
            PostHelper.generatePostCaption(instagramPostProperies.getVersion(), post, instagramPostProperies.getDefaultHashtags());
            // Save post
            postRepository.save(post);
            log.atInfo().setMessage("Successfully created standout stat image and saved").addKeyValue("player", post.getPlayer().getName()).log();
        } catch (Exception ex) {
            prepareStandoutImageDto.setPost(post);
            // Skip if image generation or upload fails, allows future retry
            log.atWarn().setMessage("Something went wrong while creating standout post").setCause(ex).addKeyValue("player", post.getPlayer().getName()).log();
            throw new Exception("Something went wrong while creating standout post: " + ex.getMessage());
        }
    }

    public void uploadPost(UploadPostDto uploadPostForm) throws Exception {
        Post post = getPostById(uploadPostForm.getPostId());

        // Extract non-zero image indices
        Set<Integer> nonZeroIndices = uploadPostForm.getImageUrls().stream()
                .map(ImageUrlEntry::getImageIndex)
                .filter(index -> index != null && index != 0)
                .collect(Collectors.toSet());

        // Check for duplicates
        Set<Integer> uniqueIndices = new HashSet<>();
        for (Integer index : nonZeroIndices) {
            if (!uniqueIndices.add(index)) {
                throw new Exception("Cannot upload due to duplicate index values");
            }
        }

        // Extract selected images - index as 0 will not be uploaded
        // Reorder in ascending order based on index
        List<ImageUrlEntry> imagesToUpload = uploadPostForm.getImageUrls().stream()
                .filter(entry -> entry.getImageIndex() != null && entry.getImageIndex() != 0)
                .sorted(Comparator.comparingInt(ImageUrlEntry::getImageIndex))
                .toList();

        facebookApiService.postToInstagram(post, imagesToUpload, uploadPostForm.getCaption());

        post.setPostedStatus(true);
        postRepository.save(post);
    }
}
