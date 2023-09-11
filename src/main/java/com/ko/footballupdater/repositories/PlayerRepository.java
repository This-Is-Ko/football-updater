package com.ko.footballupdater.repositories;

import org.springframework.data.repository.CrudRepository;

import com.ko.footballupdater.models.Player;

public interface PlayerRepository extends CrudRepository<Player, Integer> {

}