package com.ko.footballupdater.repositories;

import com.ko.footballupdater.models.CheckedStatus;
import org.springframework.data.repository.CrudRepository;

public interface UpdateStatusRepository extends CrudRepository<CheckedStatus, Integer> {

}