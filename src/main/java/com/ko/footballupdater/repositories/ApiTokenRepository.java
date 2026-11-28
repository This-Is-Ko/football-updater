package com.ko.footballupdater.repositories;

import com.ko.footballupdater.models.ApiToken;
import com.ko.footballupdater.models.TokenPlatform;
import org.springframework.data.repository.CrudRepository;

public interface ApiTokenRepository extends CrudRepository<ApiToken, TokenPlatform> {

}
