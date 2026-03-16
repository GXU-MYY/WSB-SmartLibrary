package com.wsb.book.convert;

import com.wsb.book.api.dto.ShelfAddDTO;
import com.wsb.book.api.dto.ShelfUpdateDTO;
import com.wsb.book.api.vo.ShelfVO;
import com.wsb.book.domain.Shelf;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * 书架转换器
 */
@Mapper(componentModel = "spring")
public interface ShelfConvert {

    @Mapping(target = "isDeleted", constant = "false")
    Shelf toShelf(ShelfAddDTO dto);

    ShelfVO toShelfVO(Shelf shelf);

    List<ShelfVO> toShelfVOList(List<Shelf> list);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateShelfFromDto(ShelfUpdateDTO dto, @MappingTarget Shelf shelf);
}