package com.xxj.insurance.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置：声明交换机、队列、DLX、绑定关系
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "insurance.settle.direct";
    public static final String DLX_EXCHANGE = "insurance.dlx.direct";

    // ---- 业务队列 ----
    public static final String QUEUE_OUTPATIENT = "queue.outpatient.settle";
    public static final String QUEUE_REMOTE = "queue.remote.settle";
    public static final String QUEUE_RECONCILE = "queue.batch.reconcile";
    public static final String QUEUE_NOTIFY = "queue.audit.notify";

    // ---- 死信队列 ----
    public static final String DLQ_OUTPATIENT = "queue.outpatient.settle.dlq";
    public static final String DLQ_REMOTE = "queue.remote.settle.dlq";
    public static final String DLQ_RECONCILE = "queue.batch.reconcile.dlq";

    // ---- 路由键 ----
    public static final String RK_OUTPATIENT = "settle.outpatient";
    public static final String RK_REMOTE = "settle.remote";
    public static final String RK_RECONCILE = "batch.reconcile";
    public static final String RK_NOTIFY = "audit.notify";

    @Bean
    public DirectExchange settleExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange dlxExchange() {
        return ExchangeBuilder.directExchange(DLX_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue outpatientQueue() {
        return QueueBuilder.durable(QUEUE_OUTPATIENT)
                .deadLetterExchange(DLX_EXCHANGE).deadLetterRoutingKey(DLQ_OUTPATIENT).build();
    }

    @Bean
    public Queue remoteQueue() {
        return QueueBuilder.durable(QUEUE_REMOTE)
                .deadLetterExchange(DLX_EXCHANGE).deadLetterRoutingKey(DLQ_REMOTE).build();
    }

    @Bean
    public Queue reconcileQueue() {
        return QueueBuilder.durable(QUEUE_RECONCILE)
                .deadLetterExchange(DLX_EXCHANGE).deadLetterRoutingKey(DLQ_RECONCILE).build();
    }

    @Bean
    public Queue notifyQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFY).build();
    }

    @Bean
    public Queue outpatientDlq() { return QueueBuilder.durable(DLQ_OUTPATIENT).build(); }
    @Bean
    public Queue remoteDlq() { return QueueBuilder.durable(DLQ_REMOTE).build(); }
    @Bean
    public Queue reconcileDlq() { return QueueBuilder.durable(DLQ_RECONCILE).build(); }

    @Bean
    public Binding bindOutpatient() { return BindingBuilder.bind(outpatientQueue()).to(settleExchange()).with(RK_OUTPATIENT); }
    @Bean
    public Binding bindRemote() { return BindingBuilder.bind(remoteQueue()).to(settleExchange()).with(RK_REMOTE); }
    @Bean
    public Binding bindReconcile() { return BindingBuilder.bind(reconcileQueue()).to(settleExchange()).with(RK_RECONCILE); }
    @Bean
    public Binding bindNotify() { return BindingBuilder.bind(notifyQueue()).to(settleExchange()).with(RK_NOTIFY); }
    @Bean
    public Binding bindDlqOutpatient() { return BindingBuilder.bind(outpatientDlq()).to(dlxExchange()).with(DLQ_OUTPATIENT); }
    @Bean
    public Binding bindDlqRemote() { return BindingBuilder.bind(remoteDlq()).to(dlxExchange()).with(DLQ_REMOTE); }
    @Bean
    public Binding bindDlqReconcile() { return BindingBuilder.bind(reconcileDlq()).to(dlxExchange()).with(DLQ_RECONCILE); }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }
}
