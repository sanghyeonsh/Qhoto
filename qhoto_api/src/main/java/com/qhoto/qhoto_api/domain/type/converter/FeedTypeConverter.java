package com.qhoto.qhoto_api.domain.type.converter;

import com.qhoto.qhoto_api.domain.type.FeedType;

import javax.persistence.Converter;

@Converter
public class FeedTypeConverter extends AbstractLegacyEnumAttributeConverter<FeedType>{

    private static final String ENUM_NAME = "νΌλ νμ";

    public FeedTypeConverter(){ super(FeedType.class, false, ENUM_NAME);}
}
