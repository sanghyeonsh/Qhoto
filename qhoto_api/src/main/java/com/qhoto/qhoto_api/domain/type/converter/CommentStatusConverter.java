package com.qhoto.qhoto_api.domain.type.converter;

import com.qhoto.qhoto_api.domain.type.CommentStatus;

import javax.persistence.Converter;

@Converter
public class CommentStatusConverter extends AbstractLegacyEnumAttributeConverter<CommentStatus>{
    private static final String ENUM_NAME = "λκΈ μν";

    public CommentStatusConverter() {
        super(CommentStatus.class,false, ENUM_NAME);
    }
}
