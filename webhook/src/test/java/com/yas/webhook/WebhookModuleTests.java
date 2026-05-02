package com.yas.webhook;

import com.yas.webhook.model.Webhook;
import com.yas.webhook.model.WebhookEvent;
import com.yas.webhook.model.mapper.WebhookMapper;
import com.yas.webhook.model.viewmodel.webhook.EventVm;
import com.yas.webhook.utils.HmacUtils;
import com.yas.webhook.utils.MessagesUtils;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WebhookModuleTests {

    @Test
    void messagesUtils_fallbackForMissingKey() {
        String key = "some.random.key";
        String message = MessagesUtils.getMessage(key, "p1");
        assertEquals(key, message);
    }

    @Test
    void hmacUtils_hash_returnsNonEmpty() throws NoSuchAlgorithmException, InvalidKeyException {
        String hashed = HmacUtils.hash("data", "secret");
        assertNotNull(hashed);
        assertTrue(hashed.length() > 0);
    }

    @Test
    void webhookMapper_default_toWebhookEventVms_and_listVm() {
        WebhookMapper mapper = new WebhookMapper() {
            @Override
            public com.yas.webhook.model.viewmodel.webhook.WebhookVm toWebhookVm(Webhook webhook) {
                com.yas.webhook.model.viewmodel.webhook.WebhookVm vm = new com.yas.webhook.model.viewmodel.webhook.WebhookVm();
                vm.setId(webhook.getId());
                vm.setPayloadUrl(webhook.getPayloadUrl());
                vm.setContentType(webhook.getContentType());
                vm.setIsActive(webhook.getIsActive());
                return vm;
            }

            @Override
            public Webhook toUpdatedWebhook(Webhook webhook, com.yas.webhook.model.viewmodel.webhook.WebhookPostVm webhookPostVm) {
                return webhook;
            }

            @Override
            public Webhook toCreatedWebhook(com.yas.webhook.model.viewmodel.webhook.WebhookPostVm webhookPostVm) {
                return new Webhook();
            }

            @Override
            public com.yas.webhook.model.viewmodel.webhook.WebhookDetailVm toWebhookDetailVm(Webhook createdWebhook) {
                return null;
            }
        };

        WebhookEvent e1 = new WebhookEvent();
        e1.setEventId(10L);
        WebhookEvent e2 = new WebhookEvent();
        e2.setEventId(11L);

        List<EventVm> vms = mapper.toWebhookEventVms(List.of(e1, e2));
        assertEquals(2, vms.size());
        assertEquals(10L, vms.get(0).getId());

        // test list page mapping
        Webhook w1 = new Webhook(); w1.setId(1L);
        Webhook w2 = new Webhook(); w2.setId(2L);
        Page<Webhook> page = new PageImpl<>(List.of(w1, w2));

        var listVm = mapper.toWebhookListGetVm(page, 0, 10);
        assertEquals(2, listVm.getWebhooks().size());
        assertEquals(0, listVm.getPageNo());
        assertEquals(10, listVm.getPageSize());
    }
}
