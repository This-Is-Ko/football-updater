package com.ko.footballupdater.repositories;

import com.ko.footballupdater.models.Team;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TeamRepository extends CrudRepository<Team, Integer> {

    List<Team> findByName(String name);

    @Query("SELECT DISTINCT t FROM Team t JOIN t.alternativeTeamNames a WHERE a.value = :name")
    List<Team> findByAlternativeTeamName(String name);

}
