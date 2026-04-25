package com.wsb.community.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 群组借阅申请
 */
@Data
@TableName("t_group_borrow_request")
public class GroupBorrowRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("group_id")
    private Long groupId;

    @TableField("book_id")
    private Long bookId;

    private String bookName;

    private String coverUrl;

    @TableField("shelf_id")
    private Long shelfId;

    private String shelfName;

    @TableField("owner_user_id")
    private Long ownerUserId;

    private String ownerNickname;

    @TableField("borrower_user_id")
    private Long borrowerUserId;

    private String borrowerNickname;

    private LocalDate dueTime;

    private String requestRemark;

    private String borrowFlowId;

    private Integer status;

    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
