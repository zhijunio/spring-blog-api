package com.example.blog.post.domain;

import com.example.blog.post.domain.model.CommentDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface PostMapper {
    CommentDto toCommentDto(Comment comment);
}
