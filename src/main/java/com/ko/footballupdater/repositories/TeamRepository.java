package com.ko.footballupdater.repositories;

import com.ko.footballupdater.models.Team;
import org.springframework.data.repository.CrudRepository;

public interface TeamRepository extends CrudRepository<Team, Integer> {

}