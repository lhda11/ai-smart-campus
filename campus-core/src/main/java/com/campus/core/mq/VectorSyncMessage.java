package com.campus.core.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VectorSyncMessage implements Serializable {
    private String action;   // "UPSERT" | "DELETE"
    private Long announcementId;
    private String title;
    private String content;
    private String category;
}
