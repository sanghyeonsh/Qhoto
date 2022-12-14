package com.qhoto.qhoto_api.dto.response.user;

import com.qhoto.qhoto_api.domain.type.RequestStatus;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ContactRes {

    private Long userId;
    private String name;
    private String nickname;
    private String phone;
    private String image;
    private String grade;

    private String relation;

    @QueryProjection
    @Builder

    public ContactRes(Long userId, String name, String nickname, String phone, String image, String grade, String relation) {
        this.userId = userId;
        this.name = name;
        this.nickname = nickname;
        this.phone = phone;
        this.image = image;
        this.grade = grade;
        this.relation = relation;
    }
}
