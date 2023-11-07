package com.ko.footballupdater.repositories;

import com.ko.footballupdater.models.Post;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface PostRepository extends CrudRepository<Post, Integer> {

    @Modifying
    @Query("update Post p set p.postedStatus = :postedStatus where p.id = :id")
    void updatePostSetPostedStatusForId(@Param("postedStatus") boolean postedStatus, @Param("id") int id);

}