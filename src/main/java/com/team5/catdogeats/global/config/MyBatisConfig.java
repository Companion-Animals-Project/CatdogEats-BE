package com.team5.catdogeats.global.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandler;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@MapperScan(
        basePackages = {"com.team5.catdogeats.users.mapper",
                        "com.team5.catdogeats.batch.mapper",
                        "com.team5.catdogeats.products.mapper",
                        "com.team5.catdogeats.orders.mapper",
                        "com.team5.catdogeats.reviews.mapper",
                        "com.team5.catdogeats.coupons.mapper",
                        "com.team5.catdogeats.forecast.mapper",

        },

        sqlSessionFactoryRef = "sqlSessionFactory"
)

public class MyBatisConfig {


    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) {
        // MyBatis Configuration 설정
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();

        //ZonedDateTime TypeHandler 등록
        configuration.getTypeHandlerRegistry().register(com.team5.catdogeats.global.config.mybatis.ZonedDateTimeTypeHandler.class);

        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        // ZonedDateTime TypeHandler 등록
        factoryBean.setTypeHandlers(new TypeHandler[]{new ZonedDateTimeTypeHandler()});

        // YAML 설정에서 가져온 type-aliases-package 적용
        try {
            return factoryBean.getObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean(name = "mybatisTransactionManager")
    public PlatformTransactionManager batchTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}