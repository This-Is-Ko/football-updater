package com.ko.footballupdater.repositories;

import org.springframework.data.repository.CrudRepository;

import com.ko.footballupdater.models.Player;
import java.util.List;

public interface PlayerRepository extends CrudRepository<Player, Integer> {

    List<Player> findByNameEquals(String name);

}