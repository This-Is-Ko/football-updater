package com.ko.footballupdater.repositories;

import com.ko.footballupdater.models.Team;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TeamRepository extends CrudRepository<Team, Integer> {

    List<Team> findByNameAndCountryAndLeague(String name, String country, String league);

}