package com.team5.catdogeats.batch.mapper;

import com.team5.catdogeats.batch.dto.SettlementBatchItem;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 정산 완료용 RowMapper
 */
public class SettlementCompleteRowMapper implements RowMapper<SettlementBatchItem> {

    @Override
    public SettlementBatchItem mapRow(ResultSet rs, int rowNum) throws SQLException {
        return SettlementBatchItem.builder()
                .settlementId(rs.getString("settlement_id"))
                .sellerId(rs.getString("seller_id"))
                .orderNumber(rs.getString("order_number"))
                .productTitle(rs.getString("product_title"))
                .settlementAmount(rs.getLong("settlement_amount"))
                .build();
    }
}