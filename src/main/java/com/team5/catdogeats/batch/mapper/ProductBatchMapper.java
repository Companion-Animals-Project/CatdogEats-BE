package com.team5.catdogeats.batch.mapper;

import com.team5.catdogeats.products.domain.Products;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductBatchMapper {
    @Select("SELECT * FROM products WHERE product_number = #{productNumber}")
    Products findByProductNumber(@Param("productNumber") Long productNumber);

    @Select("SELECT * FROM products")
    List<Products> selectAllProducts();
}
