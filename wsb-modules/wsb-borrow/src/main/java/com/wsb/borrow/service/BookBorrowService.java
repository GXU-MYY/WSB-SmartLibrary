package com.wsb.borrow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wsb.borrow.api.dto.BookBorrowDTO;
import com.wsb.borrow.api.dto.BookBorrowUpdateDTO;
import com.wsb.borrow.api.dto.BookReturnDTO;
import com.wsb.borrow.api.vo.BookBorrowRecordVO;
import com.wsb.borrow.api.vo.BookBorrowVO;
import com.wsb.borrow.domain.BookBorrow;

import java.util.List;

public interface BookBorrowService extends IService<BookBorrow> {

    /**
     * 书籍借阅
     */
    BookBorrowVO borrow(BookBorrowDTO dto);

    /**
     * 还书
     */
    BookBorrowVO returning(BookReturnDTO dto);

    /**
     * 查询借书记录
     * @param borrowType 借书类型：1=借入，2=借出，null=全部
     */
    List<BookBorrowRecordVO> getRecords(Integer borrowType);

    /**
     * 修改借书信息
     */
    void updateBorrow(BookBorrowUpdateDTO dto);
}