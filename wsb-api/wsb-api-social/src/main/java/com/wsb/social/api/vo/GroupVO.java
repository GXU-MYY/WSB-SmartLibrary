package com.wsb.social.api.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
public class GroupVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String groupName;
    private Long id;
    private Long ownerId;
    private String remark;
    private Long userId;
}
