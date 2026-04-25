package com.wsb.book.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wsb.book.api.constant.BookBorrowStatus;
import com.wsb.book.api.dto.BookBorrowCountDTO;
import com.wsb.book.api.dto.BookRemoteDTO;
import com.wsb.book.api.dto.BorrowCategoryStatsDTO;
import com.wsb.book.api.dto.CategoryCountDTO;
import com.wsb.book.api.dto.CommunityBorrowCreateDTO;
import com.wsb.book.api.dto.PublicShelfBookDTO;
import com.wsb.book.api.dto.ShelfRemoteDTO;
import com.wsb.book.api.dto.UserBookCountDTO;
import com.wsb.book.api.dto.UserBorrowStatsDTO;
import com.wsb.book.api.vo.CommunityBorrowFlowVO;
import com.wsb.book.convert.BookInnerConverter;
import com.wsb.book.domain.Book;
import com.wsb.book.domain.BookBorrow;
import com.wsb.book.domain.BookShelf;
import com.wsb.book.domain.Shelf;
import com.wsb.book.mapper.BookBorrowMapper;
import com.wsb.book.mapper.BookShelfMapper;
import com.wsb.book.mapper.ShelfMapper;
import com.wsb.book.service.BookBorrowService;
import com.wsb.book.service.BookInnerService;
import com.wsb.book.service.BookService;
import com.wsb.book.service.support.BookRemoteCacheService;
import com.wsb.book.util.BookClcUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookInnerServiceImpl implements BookInnerService {

    private static final int EMBEDDING_STATUS_PENDING = 0;

    private final BookService bookService;
    private final BookBorrowService bookBorrowService;
    private final BookBorrowMapper bookBorrowMapper;
    private final BookShelfMapper bookShelfMapper;
    private final ShelfMapper shelfMapper;
    private final BookInnerConverter bookInnerConverter;
    private final BookRemoteCacheService bookRemoteCacheService;

    @Override
    public BookRemoteDTO getBookById(Long bookId) {
        BookRemoteDTO cachedBook = bookRemoteCacheService.getBook(bookId);
        if (cachedBook != null) {
            return cachedBook;
        }

        Book book = bookService.getById(bookId);
        if (book == null) {
            return null;
        }

        BookRemoteDTO remoteDTO = bookInnerConverter.toBookRemoteDTO(book);
        bookRemoteCacheService.cacheBook(remoteDTO);
        return remoteDTO;
    }

    @Override
    public List<BookRemoteDTO> getBooksByIds(List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return List.of();
        }

        List<Long> uniqueIds = bookIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (uniqueIds.isEmpty()) {
            return List.of();
        }

        Map<Long, BookRemoteDTO> cachedBooks = bookRemoteCacheService.getBooks(uniqueIds);
        List<Long> missIds = uniqueIds.stream()
                .filter(bookId -> !cachedBooks.containsKey(bookId))
                .toList();

        if (!missIds.isEmpty()) {
            List<BookRemoteDTO> loadedBooks = bookInnerConverter.toBookRemoteDTOList(bookService.listByIds(missIds).stream()
                    .filter(book -> !Boolean.TRUE.equals(book.getIsDeleted()))
                    .toList());
            bookRemoteCacheService.cacheBooks(loadedBooks);
            loadedBooks.forEach(book -> cachedBooks.put(book.getId(), book));
        }

        return uniqueIds.stream()
                .map(cachedBooks::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<ShelfRemoteDTO> getPublicShelvesByOwners(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        return shelfMapper.selectList(Wrappers.<Shelf>lambdaQuery()
                        .in(Shelf::getUserId, userIds)
                        .eq(Shelf::getIsDeleted, false)
                        .eq(Shelf::getIsPublic, true)
                        .orderByDesc(Shelf::getUpdateTime)
                        .orderByDesc(Shelf::getId))
                .stream()
                .map(this::toShelfRemoteDTO)
                .toList();
    }

    @Override
    public List<PublicShelfBookDTO> getPublicShelfBooksByOwners(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        List<Shelf> publicShelves = shelfMapper.selectList(Wrappers.<Shelf>lambdaQuery()
                .in(Shelf::getUserId, userIds)
                .eq(Shelf::getIsDeleted, false)
                .eq(Shelf::getIsPublic, true));
        if (publicShelves.isEmpty()) {
            return List.of();
        }

        Map<Long, Shelf> shelfMap = publicShelves.stream()
                .collect(Collectors.toMap(Shelf::getId, shelf -> shelf));

        List<BookShelf> relations = bookShelfMapper.selectList(Wrappers.<BookShelf>lambdaQuery()
                .in(BookShelf::getShelfId, shelfMap.keySet())
                .eq(BookShelf::getIsDeleted, false));
        if (relations.isEmpty()) {
            return List.of();
        }

        Map<Long, Book> bookMap = bookService.listByIds(relations.stream()
                        .map(BookShelf::getBookId)
                        .distinct()
                        .toList())
                .stream()
                .filter(book -> !Boolean.TRUE.equals(book.getIsDeleted()))
                .collect(Collectors.toMap(Book::getId, book -> book));

        return relations.stream()
                .map(relation -> {
                    Shelf shelf = shelfMap.get(relation.getShelfId());
                    Book book = bookMap.get(relation.getBookId());
                    if (shelf == null || book == null) {
                        return null;
                    }

                    PublicShelfBookDTO dto = new PublicShelfBookDTO();
                    dto.setShelfId(shelf.getId());
                    dto.setShelfName(shelf.getShelfName());
                    dto.setOwnerUserId(shelf.getUserId());
                    dto.setBookId(book.getId());
                    dto.setTitle(book.getTitle());
                    dto.setAuthor(book.getAuthor());
                    dto.setCoverUrl(book.getCoverUrl());
                    dto.setIsBorrowed(Boolean.TRUE.equals(book.getIsBorrowed()));
                    dto.setIsLentOut(Boolean.TRUE.equals(book.getIsLentOut()));
                    dto.setBorrowable(!Boolean.TRUE.equals(book.getIsBorrowed()) && !Boolean.TRUE.equals(book.getIsLentOut()));
                    return dto;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(PublicShelfBookDTO::getShelfId).thenComparing(PublicShelfBookDTO::getBookId))
                .toList();
    }

    @Override
    public List<UserBookCountDTO> countBooksByUsers(List<Long> userIds) {
        List<Book> books;
        if (userIds != null && !userIds.isEmpty()) {
            books = bookService.list(Wrappers.<Book>lambdaQuery()
                    .in(Book::getUserId, userIds)
                    .eq(Book::getIsDeleted, false)
                    .and(wrapper -> wrapper
                            .isNull(Book::getIsBorrowed)
                            .or()
                            .eq(Book::getIsBorrowed, false)));
        } else {
            books = bookService.list(Wrappers.<Book>lambdaQuery()
                    .eq(Book::getIsDeleted, false)
                    .and(wrapper -> wrapper
                            .isNull(Book::getIsBorrowed)
                            .or()
                            .eq(Book::getIsBorrowed, false)));
        }

        return books.stream()
                .collect(Collectors.groupingBy(Book::getUserId, Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> {
                    UserBookCountDTO dto = new UserBookCountDTO();
                    dto.setUserId(entry.getKey());
                    dto.setBookCount(entry.getValue().intValue());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryCountDTO> countBooksByCategory(Long userId) {
        List<Book> books = bookService.list(Wrappers.<Book>lambdaQuery()
                .eq(Book::getUserId, userId)
                .eq(Book::getIsDeleted, false)
                .and(wrapper -> wrapper
                        .isNull(Book::getIsBorrowed)
                        .or()
                        .eq(Book::getIsBorrowed, false)));

        return books.stream()
                .map(book -> BookClcUtils.resolveCategory(book.getClc()))
                .collect(Collectors.groupingBy(category -> category, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry::getKey))
                .limit(10)
                .map(entry -> {
                    CategoryCountDTO dto = new CategoryCountDTO();
                    dto.setCategory(entry.getKey());
                    dto.setCount(entry.getValue().intValue());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getBookIdsByOwner(Long userId) {
        return bookService.list(Wrappers.<Book>lambdaQuery()
                        .eq(Book::getUserId, userId)
                        .eq(Book::getIsDeleted, false)
                        .and(wrapper -> wrapper
                                .isNull(Book::getIsBorrowed)
                                .or()
                                .eq(Book::getIsBorrowed, false))
                        .select(Book::getId))
                .stream()
                .map(Book::getId)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookBorrowCountDTO> countBorrowByBooks(List<Long> bookIds) {
        List<BookBorrow> borrows;
        if (bookIds != null && !bookIds.isEmpty()) {
            borrows = bookBorrowMapper.selectList(Wrappers.<BookBorrow>lambdaQuery()
                    .in(BookBorrow::getBookId, bookIds)
                    .eq(BookBorrow::getIsDeleted, false)
                    .eq(BookBorrow::getBorrowType, 2));
        } else {
            borrows = bookBorrowMapper.selectList(Wrappers.<BookBorrow>lambdaQuery()
                    .eq(BookBorrow::getIsDeleted, false)
                    .eq(BookBorrow::getBorrowType, 2));
        }

        return borrows.stream()
                .collect(Collectors.groupingBy(BookBorrow::getBookId, Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> {
                    BookBorrowCountDTO dto = new BookBorrowCountDTO();
                    dto.setBookId(entry.getKey());
                    dto.setBorrowCount(entry.getValue().intValue());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public UserBorrowStatsDTO getUserBorrowStats(Long userId) {
        List<BookBorrow> borrows = bookBorrowMapper.selectList(Wrappers.<BookBorrow>lambdaQuery()
                .eq(BookBorrow::getUserId, userId)
                .eq(BookBorrow::getIsDeleted, false));

        UserBorrowStatsDTO dto = new UserBorrowStatsDTO();
        dto.setTotalBorrowed(borrows.size());
        dto.setBorrowedIn((int) borrows.stream()
                .filter(borrow -> borrow.getBorrowType() != null && borrow.getBorrowType() == 1)
                .filter(borrow -> borrow.getStatus() != null && borrow.getStatus() != BookBorrowStatus.RETURNED)
                .count());
        dto.setBorrowedOut((int) borrows.stream()
                .filter(borrow -> borrow.getBorrowType() != null && borrow.getBorrowType() == 2)
                .filter(borrow -> borrow.getStatus() != null && borrow.getStatus() != BookBorrowStatus.RETURNED)
                .count());
        dto.setUnreturned((int) borrows.stream()
                .filter(borrow -> borrow.getStatus() != null && borrow.getStatus() != BookBorrowStatus.RETURNED)
                .count());
        return dto;
    }

    @Override
    public Integer countUnreturnedByOwner(Long ownerId) {
        Long count = bookBorrowMapper.selectCount(Wrappers.<BookBorrow>lambdaQuery()
                .eq(BookBorrow::getUserId, ownerId)
                .eq(BookBorrow::getBorrowType, 2)
                .eq(BookBorrow::getIsDeleted, false)
                .in(BookBorrow::getStatus, BookBorrowStatus.BORROWING, BookBorrowStatus.OVERDUE));
        return count.intValue();
    }

    @Override
    public List<BorrowCategoryStatsDTO> getBorrowStatsByCategory(List<Long> bookIds) {
        List<BookBorrow> borrows;
        if (bookIds != null && !bookIds.isEmpty()) {
            borrows = bookBorrowMapper.selectList(Wrappers.<BookBorrow>lambdaQuery()
                    .in(BookBorrow::getBookId, bookIds)
                    .eq(BookBorrow::getIsDeleted, false));
        } else {
            borrows = bookBorrowMapper.selectList(Wrappers.<BookBorrow>lambdaQuery()
                    .eq(BookBorrow::getIsDeleted, false));
        }

        if (borrows.isEmpty()) {
            return List.of();
        }

        Map<Long, String> categoryMap = bookService.listByIds(borrows.stream()
                        .map(BookBorrow::getBookId)
                        .distinct()
                        .toList())
                .stream()
                .filter(book -> !Boolean.TRUE.equals(book.getIsDeleted()))
                .collect(Collectors.toMap(Book::getId, book -> BookClcUtils.resolveCategory(book.getClc()), (a, b) -> a));

        Map<String, BorrowCategoryStatsDTO> grouped = new java.util.HashMap<>();
        for (BookBorrow borrow : borrows) {
            String category = categoryMap.getOrDefault(borrow.getBookId(), "未分类");
            BorrowCategoryStatsDTO dto = grouped.computeIfAbsent(category, key -> {
                BorrowCategoryStatsDTO item = new BorrowCategoryStatsDTO();
                item.setCategory(key);
                item.setTotal(0);
                item.setReading(0);
                item.setRead(0);
                return item;
            });

            dto.setTotal(dto.getTotal() + 1);
            if (borrow.getStatus() != null && borrow.getStatus() == BookBorrowStatus.RETURNED) {
                dto.setRead(dto.getRead() + 1);
            } else if (borrow.getStatus() != null) {
                dto.setReading(dto.getReading() + 1);
            }
        }

        return grouped.values().stream()
                .sorted(Comparator.comparing(BorrowCategoryStatsDTO::getTotal, Comparator.reverseOrder())
                        .thenComparing(BorrowCategoryStatsDTO::getCategory))
                .limit(10)
                .toList();
    }

    @Override
    public BorrowCategoryStatsDTO getBorrowSummary(List<Long> bookIds) {
        List<BookBorrow> borrows;
        if (bookIds != null && !bookIds.isEmpty()) {
            borrows = bookBorrowMapper.selectList(Wrappers.<BookBorrow>lambdaQuery()
                    .in(BookBorrow::getBookId, bookIds)
                    .eq(BookBorrow::getIsDeleted, false));
        } else {
            borrows = bookBorrowMapper.selectList(Wrappers.<BookBorrow>lambdaQuery()
                    .eq(BookBorrow::getIsDeleted, false));
        }

        BorrowCategoryStatsDTO dto = new BorrowCategoryStatsDTO();
        dto.setTotal(borrows.size());
        dto.setReading((int) borrows.stream()
                .filter(borrow -> borrow.getStatus() != null && borrow.getStatus() != BookBorrowStatus.RETURNED)
                .count());
        dto.setRead((int) borrows.stream()
                .filter(borrow -> borrow.getStatus() != null && borrow.getStatus() == BookBorrowStatus.RETURNED)
                .count());
        return dto;
    }

    @Override
    public List<Long> getBooksWithNullSummary() {
        return bookService.list(Wrappers.<Book>lambdaQuery()
                        .eq(Book::getIsDeleted, false)
                        .and(wrapper -> wrapper.isNull(Book::getSummary).or().eq(Book::getSummary, ""))
                        .select(Book::getId))
                .stream()
                .map(Book::getId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getBooksPendingEmbedding() {
        return bookService.list(Wrappers.<Book>lambdaQuery()
                        .eq(Book::getIsDeleted, false)
                        .and(wrapper -> wrapper.isNull(Book::getEmbeddingStatus)
                                .or()
                                .eq(Book::getEmbeddingStatus, EMBEDDING_STATUS_PENDING))
                        .isNotNull(Book::getSummary)
                        .ne(Book::getSummary, "")
                        .select(Book::getId))
                .stream()
                .map(Book::getId)
                .collect(Collectors.toList());
    }

    @Override
    public void updateSummary(Long bookId, String summary) {
        Book book = new Book();
        book.setId(bookId);
        book.setSummary(summary);
        bookService.updateById(book);
        bookRemoteCacheService.evictBook(bookId);
    }

    @Override
    public void updateEmbeddingStatus(Long bookId, Integer status) {
        Book book = new Book();
        book.setId(bookId);
        book.setEmbeddingStatus(status);
        bookService.updateById(book);
        bookRemoteCacheService.evictBook(bookId);
    }

    @Override
    public CommunityBorrowFlowVO createCommunityBorrowFlow(CommunityBorrowCreateDTO dto) {
        return bookBorrowService.createCommunityBorrowFlow(dto);
    }

    private ShelfRemoteDTO toShelfRemoteDTO(Shelf shelf) {
        ShelfRemoteDTO dto = new ShelfRemoteDTO();
        dto.setId(shelf.getId());
        dto.setShelfName(shelf.getShelfName());
        dto.setUserId(shelf.getUserId());
        dto.setIsPublic(shelf.getIsPublic());
        dto.setRemark(shelf.getRemark());
        return dto;
    }
}
