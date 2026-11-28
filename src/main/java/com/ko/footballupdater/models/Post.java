package com.ko.footballupdater.models;

import com.ko.footballupdater.converter.StringListConverter;
import com.ko.footballupdater.llm.VisionQaService;
import com.ko.footballupdater.llm.models.QaIssue;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Holds a post related to a player.
 */
@Getter
@Setter
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;

    /**
     * The type of post.
     * Any null can be assumed as standard ALL_STAT_POST
     */
    @Column
    @Enumerated(EnumType.STRING)
    private PostType postType;

    /**
     * The player associated with the post.
     */
    @ManyToOne
    @JoinColumn(name = "player_id")
    @JdbcTypeCode(SqlTypes.JSON)
    private Player player;

    /**
     * The performance statistics of the player associated with the post.
     */
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "match_performance_stats_id")
    @JdbcTypeCode(SqlTypes.JSON)
    private PlayerMatchPerformanceStats playerMatchPerformanceStats;

    /**
     * The URLs of images.
     */
    @Convert(converter = StringListConverter.class)
    @Column(name = "image_urls", length = 10000)
    private List<String> imagesUrls;

    /**
     * Temporary storage for URLs used for image searches.
     * Generated for posts view
     */
    @Transient
    @Convert(converter = StringListConverter.class)
    private List<String> imageSearchUrls = new ArrayList<>();

    /**
     * Temporary storage for file names of images.
     */
    @Transient
    @Convert(converter = StringListConverter.class)
    private List<String> imagesFileNames = new ArrayList<>();

    /**
     * The date when the post was generated.
     */
    @NotNull
    private Date dateGenerated = new Date();

    /**
     * Caption of post.
     */
    @Column(name = "caption", length = 5000)
    private String caption;

    /**
     * Indicates whether the post has been posted.
     */
    @NotNull
    private boolean postedStatus;

    /**
     * Approval queue status. Null for posts created before the queue existed.
     */
    @Column
    @Enumerated(EnumType.STRING)
    private DraftStatus draftStatus;

    /**
     * Why the editor (LLM or rule-based fallback) picked this post type and stats, shown to the reviewer.
     */
    @Column(name = "llm_reason", length = 2000)
    private String llmReason;

    /**
     * Stat field names the editor chose to feature.
     */
    @Convert(converter = StringListConverter.class)
    @Column(name = "featured_stats", length = 1000)
    private List<String> featuredStats;

    /**
     * Whether the caption was written by the LLM or the template.
     */
    @Column
    @Enumerated(EnumType.STRING)
    private CaptionSource captionSource;

    /**
     * Vision QA verdict: true if no issues were found, null if QA hasn't run.
     * QA only flags issues for the reviewer, it never blocks approval.
     */
    @Column(name = "qa_passed")
    private Boolean qaPassed;

    /**
     * Vision QA issues as a JSON array of {type, detail}.
     */
    @Column(name = "qa_issues", length = 4000)
    private String qaIssues;

    @Column(name = "qa_checked_at")
    private Date qaCheckedAt;

    /**
     * Where the standout background photo came from, when a photo was used.
     */
    @Column(name = "photo_source_url", length = 2000)
    private String photoSourceUrl;

    @Column(name = "photo_source_name", length = 64)
    private String photoSourceName;

    /**
     * Free-text licence or credit for the photo.
     */
    @Column(name = "photo_licence", length = 500)
    private String photoLicence;

    public Post() {
    }

    public Post(PostType postType, Player player) {
        this.postType = postType;
        this.player = player;
    }

    public Post(PostType postType, Player player, PlayerMatchPerformanceStats playerMatchPerformanceStats) {
        this.postType = postType;
        this.player = player;
        this.playerMatchPerformanceStats = playerMatchPerformanceStats;
    }

    public Post(Integer id, boolean postedStatus) {
        this.id = id;
        this.postedStatus = postedStatus;
    }

    public Post(Player player, List<String> imagesUrls) {
        this.player = player;
        this.imagesUrls = imagesUrls;
        this.postedStatus = false;
    }

    public List<String> getFeaturedStats() {
        if (featuredStats == null) {
            return new ArrayList<>();
        }
        // The converter turns an empty column into a single empty string
        return featuredStats.stream().filter(stat -> stat != null && !stat.isBlank()).toList();
    }

    /**
     * QA issues for display, parsed from {@link #qaIssues}.
     */
    public List<QaIssue> getQaIssueList() {
        return VisionQaService.parseIssues(qaIssues);
    }

    public List<String> getImagesUrls() {
        if (imagesUrls == null) {
            imagesUrls = new ArrayList<>();
        }
        return imagesUrls;
    }
}
