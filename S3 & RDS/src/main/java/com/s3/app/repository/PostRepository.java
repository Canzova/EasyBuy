package com.s3.app.repository;

import com.s3.app.entity.PostEntity;
import com.s3.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PostRepository extends JpaRepository<PostEntity, Long> {
    List<PostEntity> findAllByUser(User user);
}
