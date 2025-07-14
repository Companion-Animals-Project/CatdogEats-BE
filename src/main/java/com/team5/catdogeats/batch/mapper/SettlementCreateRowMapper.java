package com.team5.catdogeats.batch.mapper;

import com.team5.catdogeats.batch.dto.SettlementBatchItem;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 정산 생성용 RowMapper
 */
public class SettlementCreateRowMapper implements RowMapper<SettlementBatchItem> {

    @Override
    public SettlementBatchItem mapRow(ResultSet rs, int rowNum) throws SQLException {
        return SettlementBatchItem.builder()
                .sellerId(rs.getString("seller_id"))
                .orderItemId(rs.getString("order_item_id"))
                .orderNumber(rs.getString("order_number"))
                .productTitle(rs.getString("product_title"))
                .itemPrice(rs.getLong("item_price"))
                .build();
    }
}