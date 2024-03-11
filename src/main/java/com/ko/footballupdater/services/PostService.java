package com.ko.footballupdater.services;


import com.ko.footballupdater.configuration.InstagramPostProperies;
import com.ko.footballupdater.exceptions.GenerateStandoutException;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.PostType;
import com.ko.footballupdater.models.form.CreatePostDto;
import com.ko.footballupdater.models.form.CreatePostPlayerDto;
import com.ko.footballupdater.models.form.ImageUrlEntry;
import com.ko.footballupdater.models.form.PrepareStandoutImageDto;
import com.ko.footballupdater.models.form.StatisticEntryGenerateDto;
import com.ko.footballupdater.models.form.UploadPostDto;
import com.ko.footballupdater.repositories.PlayerRepository;
import com.ko.footballupdater.repositories.PostRepository;
import com.ko.footballupdater.request.CreatePostRequest;
import com.ko.footballupdater.utils.PostHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private ImageGeneratorService imageGeneratorService;

    @Autowired
    private AmazonS3Service amazonS3Service;

    @Autowired
    private FacebookApiService facebookApiService;

    @Autowired
    private PostHelper postHelper;

    @Autowired
    private InstagramPostProperies instagramPostProperies;

    final Map<String, String> CUSTOM_FIELD_NAME_MAPPING = Map.of(
            "goals", "goal",
            "assists", "assist",
            "yellowCards", "yellowCard",
            "redCards", "redCard"
    );


    public List<Post> getPosts(Boolean postedStatus, Integer pageNumber, Integer pageSize) {
        if (pageNumber == null || pageNumber < 0) {
            pageNumber = 0;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 30;
        }
        if (postedStatus != null) {
            return postRepository.findByPostedStatusOrderByDateGeneratedDesc(postedStatus, PageRequest.of(pageNumber,pageSize));
        }
        return postRepository.findAllByOrderByDateGeneratedDesc(PageRequest.of(pageNumber,pageSize));
    }

    public Post getPostById(Integer postId) throws IllegalArgumentException {
        Optional<Post> postSearchResult = postRepository.findById(postId);
        if (postSearchResult.isEmpty()) {
            throw new IllegalArgumentException("Post id not found");
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

    public CreatePostDto prepareDtoForCreateNewPost() {
        CreatePostDto createPostDto = new CreatePostDto();
        List<Player> players = playerRepository.findAll();
        if (players != null) {
            List<CreatePostPlayerDto> playerDtos = players.stream().map(player -> new CreatePostPlayerDto(player.getId(), player.getName())).toList();
            createPostDto.setPlayers(playerDtos);
        }
        return createPostDto;
    }

    public PrepareStandoutImageDto prepareDtoForGeneratePost(Integer postId) throws NoSuchElementException {
        PrepareStandoutImageDto prepareStandoutImageDto = new PrepareStandoutImageDto();

        // Search for post with id
        Optional<Post> postSearchResult = postRepository.findById(postId);
        if (postSearchResult.isEmpty()) {
            throw new NoSuchElementException("Post id not found");
        }
        Post post = postSearchResult.get();
        postHelper.generatePostImageSearchUrl(post);
        prepareStandoutImageDto.setPost(post);

        List<StatisticEntryGenerateDto> allStats = new ArrayList<>();
        if (post.getPlayerMatchPerformanceStats() == null) {
            throw new NoSuchElementException("Post doesn't contain match performance stats object");
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
            throw new NoSuchElementException("Post id not found");
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
        }

        try {
            // Generate standout post image
            imageGeneratorService.generateStandoutStatsImage(post, filteredStats, prepareStandoutImageDto.getImageGenParams());
            // Upload stat images to s3
            amazonS3Service.uploadToS3(post, true);
            // Save post
            postRepository.save(post);
            log.atInfo().setMessage("Successfully created standout stat image and saved").addKeyValue("player", post.getPlayer().getName()).log();
        } catch (Exception ex) {
            prepareStandoutImageDto.setPost(post);
            // Skip if image generation or upload fails, allows future retry
            log.atWarn().setMessage("Something went wrong while creating standout post").setCause(ex).addKeyValue("player", post.getPlayer().getName()).log();
            throw new GenerateStandoutException("Something went wrong while creating standout post: " + ex.getMessage());
        }
    }

    public void createPost(CreatePostDto createPostDto) {
        // Map CreatePostDto to CreatePostRequest
        CreatePostRequest createPostRequest = new CreatePostRequest();
        createPostRequest.setPlayerId(createPostDto.getSelectedPlayerId());
        createPostRequest.setStats(createPostDto.getPlayerMatchPerformanceStats());
        createPost(createPostRequest);
    }

    public Post createPost(CreatePostRequest createPostRequest) throws IllegalArgumentException {
        // Check if request is not null
        if (createPostRequest == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }

        // Check if playerId exists in the database
        Integer playerId = createPostRequest.getPlayerId();
        Optional<Player> playerSearchResult = playerRepository.findById(playerId);
        if (playerSearchResult.isEmpty()) {
            throw new IllegalArgumentException("Player with ID " + playerId + " not found in the database");
        }

        Post newPost = new Post(PostType.ALL_STAT_POST, playerSearchResult.get(), createPostRequest.getStats());
        String hashtags = "";
        if (newPost.getPlayerMatchPerformanceStats().getMatch().getRelevantTeam() != null && !newPost.getPlayerMatchPerformanceStats().getMatch().getRelevantTeam().isEmpty()) {
            hashtags = postHelper.generateTeamHashtags(newPost.getPlayerMatchPerformanceStats().getMatch().getRelevantTeam());
            log.atInfo().setMessage("Set team hashtags to: " + hashtags).addKeyValue("player", playerSearchResult.get().getName()).log();
        } else {
            log.atWarn().setMessage("Unable to generate team hashtags due to relevant team missing").addKeyValue("player", playerSearchResult.get().getName()).log();
        }

        postHelper.generatePostCaption(instagramPostProperies.getVersion(), newPost, instagramPostProperies.getDefaultHashtags() + hashtags);

        return postRepository.save(newPost);
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
                throw new IllegalArgumentException("Cannot upload due to duplicate index values");
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
