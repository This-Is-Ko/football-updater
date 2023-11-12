package com.ko.footballupdater.repositories;

import com.ko.footballupdater.models.Player;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PlayerRepository extends CrudRepository<Player, Integer> {

    @Override
    List<Player> findAll();

    List<Player> findByNameEquals(String name);

}