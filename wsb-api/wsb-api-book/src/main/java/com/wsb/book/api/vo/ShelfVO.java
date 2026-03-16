package com.wsb.book.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 书架VO
 */
@Data
public class ShelfVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String shelfName;
    private String address;
    private Boolean isPublic;
    private String remark;
    private Long userId;
    private Integer shelfType;
    private Boolean isDeleted;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}