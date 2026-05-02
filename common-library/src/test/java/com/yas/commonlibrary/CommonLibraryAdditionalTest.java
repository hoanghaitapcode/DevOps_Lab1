package com.yas.commonlibrary;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.commonlibrary.kafka.cdc.BaseCdcConsumer;
import com.yas.commonlibrary.kafka.cdc.message.Operation;
import com.yas.commonlibrary.kafka.cdc.message.Product;
import com.yas.commonlibrary.kafka.cdc.message.ProductCdcMessage;
import com.yas.commonlibrary.kafka.cdc.message.ProductMsgKey;
import com.yas.commonlibrary.viewmodel.error.ErrorVm;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.MessageHeaders;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class CommonLibraryAdditionalTest {

    @Test
    void operation_enum_getName() {
        assertEquals("c", Operation.CREATE.getName());
        assertEquals("d", Operation.DELETE.getName());
    }

    @Test
    void product_and_msgkey_and_cdc_message_builder() {
        Product p = Product.builder().id(5).isPublished(true).build();
        assertEquals(5L, p.getId());
        assertTrue(p.isPublished());

        ProductMsgKey key = ProductMsgKey.builder().id(55L).build();
        assertEquals(55L, key.getId());

        ProductCdcMessage msg = ProductCdcMessage.builder().before(p).after(p).op(Operation.UPDATE).build();
        assertNotNull(msg.getBefore());
        assertEquals(Operation.UPDATE, msg.getOp());
    }

    @Test
    void baseCdcConsumer_processMessage_variants() {
        class TestConsumer extends BaseCdcConsumer<ProductMsgKey, ProductCdcMessage> {
            public void callProcessMessage(ProductCdcMessage record, MessageHeaders headers, java.util.function.Consumer<ProductCdcMessage> consumer) {
                processMessage(record, headers, consumer);
            }

            public void callProcessMessageWithKey(ProductMsgKey key, ProductCdcMessage value, MessageHeaders headers, java.util.function.BiConsumer<ProductMsgKey, ProductCdcMessage> consumer) {
                processMessage(key, value, headers, consumer);
            }
        }

        TestConsumer tc = new TestConsumer();
        AtomicBoolean called1 = new AtomicBoolean(false);
        ProductCdcMessage rec = ProductCdcMessage.builder().op(Operation.READ).build();
        MessageHeaders headers = new MessageHeaders(Map.of(KafkaHeaders.RECEIVED_KEY, "k1"));

        tc.callProcessMessage(rec, headers, r -> called1.set(true));
        assertTrue(called1.get());

        AtomicBoolean called2 = new AtomicBoolean(false);
        ProductMsgKey key = ProductMsgKey.builder().id(7L).build();
        tc.callProcessMessageWithKey(key, rec, headers, (k, v) -> called2.set(true));
        assertTrue(called2.get());
    }

    @Test
    void exceptions_use_messages_utils_fallback() {
        BadRequestException bre = new BadRequestException("err.key.not.found");
        assertEquals("err.key.not.found", bre.getMessage());

        NotFoundException nfe = new NotFoundException("not.found.code");
        assertEquals("not.found.code", nfe.getMessage());
    }

    @Test
    void errorVm_threeArg_constructor_has_empty_fieldErrors() {
        ErrorVm vm = new ErrorVm("400", "title", "detail");
        assertEquals("400", vm.statusCode());
        assertEquals("title", vm.title());
        assertTrue(vm.fieldErrors().isEmpty());
    }
}
