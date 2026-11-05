package com.mss301.watch.repository;

import com.mss301.watch.entity.WatchCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchCategoryRepository
        extends JpaRepository<WatchCategory, Integer> {
}