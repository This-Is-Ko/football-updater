package com.ko.footballupdater.utils;

import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.PostType;
import org.slf4j.spi.LoggingEventBuilder;

public class LogHelper {

    public static void logWithSubject(LoggingEventBuilder loggingEventBuilder, Post post) {
        String subject;
        if (post != null && (PostType.ALL_STAT_POST.equals(post.getPostType()) || PostType.STANDOUT_STATS_POST.equals(post.getPostType()))) {
            subject = post.getPlayer().getName();
        } else if (post.getPostType() != null) {
            subject = post.getPostType().toString();
        } else {
            subject = "generic_post";
        }
        loggingEventBuilder.addKeyValue("subject", subject).log();
    }
}
