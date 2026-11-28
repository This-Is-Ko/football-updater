package com.ko.footballupdater.repositories;

import com.ko.footballupdater.models.PlayerFaceEmbedding;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PlayerFaceEmbeddingRepository extends CrudRepository<PlayerFaceEmbedding, Long> {

    List<PlayerFaceEmbedding> findByPlayerIdAndModel(Integer playerId, String model);

    List<PlayerFaceEmbedding> findByPlayerId(Integer playerId);

    @Transactional
    long deleteByPlayerId(Integer playerId);
}
