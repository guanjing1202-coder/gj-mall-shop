package com.gj.mall.product.repo;

import com.gj.mall.product.doc.ProductDetailDoc;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ProductDetailRepository extends MongoRepository<ProductDetailDoc, String> {
    Optional<ProductDetailDoc> findBySpuId(Long spuId);
}
