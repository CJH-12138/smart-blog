package com.blog.mq.event;

import lombok.Data;

@Data
public class RagIndexEvent {

    private String sourceType;
    private Long sourceId;
}
