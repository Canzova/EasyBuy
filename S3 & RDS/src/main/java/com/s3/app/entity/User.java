package com.s3.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
// Getters / Setters will be used by model mapper for mapping, also it will allow us to use these fields into other classes
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    private String name;

    @Lob
    private String bio;

    @Column(nullable = false)
    private String password;

    @OneToMany(mappedBy = "userId",  fetch = FetchType.LAZY,  cascade = CascadeType.ALL)
    private List<PostEntity> posts = new ArrayList<>();
}
