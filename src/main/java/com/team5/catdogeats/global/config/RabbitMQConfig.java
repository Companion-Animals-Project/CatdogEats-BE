package com.team5.catdogeats.global.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    /* ---------- Exchange & Routing ---------- */
    public static final String EXCHANGE_ORDERS = "order.events";

    public static final String RK_ORDER_CREATED   = "order.created";
    public static final String RK_PAYMENT_SUCCESS = "payment.completed";
    public static final String RK_PAYMENT_FAILED  = "payment.failed";
    public static final String RK_ORDER_PAYMENT_TIMEOUT = "order.payment.timeout";

    /* ---------- Queue 이름 ---------- */
    public static final String Q_ORDER_CREATED        = "q.order.created";
    public static final String Q_ORDER_CREATED_STOCK   = "q.order.created.stock";
    public static final String Q_ORDER_CREATED_PAYMENT = "q.order.created.payment";
    public static final String Q_PAYMENT_COMPLETED    = "q.payment.completed";
    public static final String Q_PAYMENT_FAILED       = "q.payment.failed";
    public static final String DLX_ORDER_EVENTS       = "dlx.order.events";
    public static final String Q_ORDER_PAYMENT_TIMEOUT_DELAY = "q.order.payment.timeout.delay";
    public static final String Q_ORDER_PAYMENT_TIMEOUT       = "q.order.payment.timeout";

    /* ---------- JSON Converter ---------- */
    @Bean
    public MessageConverter jacksonConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /* ---------- Exchange ---------- */
    @Bean
    public TopicExchange orderEventsExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_ORDERS).durable(true).build();
    }

    /* ---------- Dead-Letter Exchange ---------- */
    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(DLX_ORDER_EVENTS).durable(true).build();
    }

    @Bean
    public DirectExchange paymentEventsExchange() {
        return ExchangeBuilder
                .directExchange("payment.events")
                .durable(true)
                .build();
    }

    /* ---------- Queues + DLQ 연결 ---------- */
    @Bean
    public Queue orderCreatedQueue() {
        return buildQueue(Q_ORDER_CREATED, RK_ORDER_CREATED);
    }
    @Bean
    public Queue orderCreatedQueueForStock() {
        return buildQueue(Q_ORDER_CREATED_STOCK, RK_ORDER_CREATED);
    }

    @Bean
    public Queue orderCreatedQueueForPayment() {
        return buildQueue(Q_ORDER_CREATED_PAYMENT, RK_ORDER_CREATED);
    }

    @Bean
    public Queue paymentCompletedQueue() {
        return buildQueue(Q_PAYMENT_COMPLETED, RK_PAYMENT_SUCCESS);
    }
    @Bean
    public Queue paymentFailedQueue() {
        return buildQueue(Q_PAYMENT_FAILED, RK_PAYMENT_FAILED);
    }

    private Queue buildQueue(String name, String deadLetterRoutingKey) {
        return QueueBuilder.durable(name)
                .withArgument("x-dead-letter-exchange", DLX_ORDER_EVENTS)
                .withArgument("x-dead-letter-routing-key", deadLetterRoutingKey + ".dlq")
                .build();
    }

    // 1) Delay Queue Bean
    @Bean
    public Queue paymentTimeoutDelayQueue() {
        return QueueBuilder.durable(Q_ORDER_PAYMENT_TIMEOUT_DELAY)
                .withArgument("x-message-ttl", 600_000)                                 // 10분
                .withArgument("x-dead-letter-exchange", EXCHANGE_ORDERS)               // 최종 exchange
                .withArgument("x-dead-letter-routing-key", RK_ORDER_PAYMENT_TIMEOUT)   // 실제 처리 큐 라우팅키
                .build();
    }

    // 2) 실제 처리용 큐 Bean
    @Bean
    public Queue paymentTimeoutQueue() {
        return QueueBuilder.durable(Q_ORDER_PAYMENT_TIMEOUT).build();
    }

    // 3) 실제 처리 큐 바인딩
    @Bean
    public Binding bindPaymentTimeoutQueue(TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(paymentTimeoutQueue())
                .to(orderEventsExchange)
                .with(RK_ORDER_PAYMENT_TIMEOUT);
    }


    /* ---------- DLQ 자체 ---------- */
    @Bean
    public Queue dlq() {
        return QueueBuilder.durable("q.order.events.dlq").build();
    }
    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(dlq())
                .to(deadLetterExchange())
                .with("#");
    }

    /* ---------- 바인딩 ---------- */
    @Bean
    public Binding bindOrderCreated(TopicExchange ex) {
        return BindingBuilder.bind(orderCreatedQueue()).to(ex).with(RK_ORDER_CREATED);
    }
    @Bean
    public Binding bindOrderCreatedForStock(TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(orderCreatedQueueForStock())
                .to(orderEventsExchange)
                .with(RK_ORDER_CREATED);
    }

    @Bean
    public Binding bindOrderCreatedForPayment(TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(orderCreatedQueueForPayment())
                .to(orderEventsExchange)
                .with(RK_ORDER_CREATED);
    }

    @Bean
    public Binding bindPaymentCompleted(Queue paymentCompletedQueue,
                                        DirectExchange paymentEventsExchange) {
        return BindingBuilder.bind(paymentCompletedQueue)
                .to(paymentEventsExchange)
                .with(RK_PAYMENT_SUCCESS);
    }
    @Bean
    public Binding bindPaymentFailed(Queue paymentFailedQueue,
                                                      DirectExchange paymentEventsExchange) {
        return BindingBuilder.bind(paymentFailedQueue)
                .to(paymentEventsExchange)
                .with(RK_PAYMENT_FAILED);
    }

    /* ---------- RabbitTemplate ---------- */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter cvt) {
        RabbitTemplate tpl = new RabbitTemplate(cf);
        tpl.setMessageConverter(cvt);
        return tpl;
    }

    /* ---------- Listener Container (재시도 & 수동 ACK) ---------- */
    @Bean
    public SimpleRabbitListenerContainerFactory listenerContainerFactory(
            ConnectionFactory cf, MessageConverter cvt) {

        SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(cf);
        f.setMessageConverter(cvt);
        f.setDefaultRequeueRejected(false);          // 재시도 후에도 실패 시 DLQ 전송
        f.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build());
        return f;
    }
}

