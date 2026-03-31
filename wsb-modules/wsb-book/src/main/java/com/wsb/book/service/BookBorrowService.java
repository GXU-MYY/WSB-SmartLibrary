package com.wsb.book.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wsb.book.api.dto.BookBorrowDTO;
import com.wsb.book.api.dto.BookBorrowUpdateDTO;
import com.wsb.book.api.dto.BookReturnDTO;
import com.wsb.book.api.vo.BookBorrowRecordVO;
import com.wsb.book.api.vo.BookBorrowSummaryVO;
import com.wsb.book.api.vo.BookBorrowVO;
import com.wsb.book.domain.BookBorrow;

/**
 * 借阅服务接口
 */
public interface BookBorrowService extends IService<BookBorrow> {

    /**
     * 图书借阅
     */
    BookBorrowVO borrow(BookBorrowDTO dto);

    /**
     * 还书
     */
    BookBorrowVO returning(BookReturnDTO dto);

    /**
     * 查询借阅记录
     *
     * @param page 分页页码
     * @param pageSize 每页条数
     * @param borrowType 借阅类型：1=借入，2=借出，null=全部
     * @param status 借阅状态：0=借阅中，1=已归还，2=已逾期，null=全部
     */
    Page<BookBorrowRecordVO> getRecords(Integer page, Integer pageSize, Integer borrowType, Integer status);

    /**
     * 获取借阅汇总
     */
    BookBorrowSummaryVO getSummary();

    /**
     * 修改借阅信息
     */
    void updateBorrow(BookBorrowUpdateDTO dto);
}
