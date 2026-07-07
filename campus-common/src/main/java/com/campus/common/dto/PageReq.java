package com.campus.common.dto;

import lombok.Data;

@Data
public class PageReq {
    private int page = 1;
    private int size = 10;
}
