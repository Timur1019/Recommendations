package com.kurashnation.mapper;

public interface BaseMapper<E, D> {
    D toDto(E entity);
}

