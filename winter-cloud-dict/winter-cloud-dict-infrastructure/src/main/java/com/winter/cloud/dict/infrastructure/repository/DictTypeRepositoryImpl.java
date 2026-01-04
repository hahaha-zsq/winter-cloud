package com.winter.cloud.dict.infrastructure.repository;

import com.winter.cloud.dict.domain.repository.DictTypeRepository;
import com.winter.cloud.dict.infrastructure.service.IDictTypeMPService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


@Repository
@RequiredArgsConstructor
public class DictTypeRepositoryImpl implements DictTypeRepository {
    private final IDictTypeMPService dictTypeMPService;


}
