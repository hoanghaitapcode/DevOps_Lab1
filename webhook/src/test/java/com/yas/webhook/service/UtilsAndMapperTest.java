package com.yas.webhook.service;

import static org.junit.jupiter.api.Assertions.*;

import com.yas.webhook.model.WebhookEvent;
import com.yas.webhook.model.mapper.WebhookMapper;
import com.yas.webhook.model.viewmodel.webhook.EventVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookListGetVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookVm;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

public class UtilsAndMapperTest {

    @Test
    void toWebhookEventVms_emptyList_returnsEmpty() {
        WebhookMapper mapper = new WebhookMapper() {
            @Override
            public WebhookVm toWebhookVm(com.yas.webhook.model.Webhook webhook) {
                return null;
            }

            @Override
            public com.yas.webhook.model.Webhook toUpdatedWebhook(com.yas.webhook.model.Webhook webhook, com.yas.webhook.model.viewmodel.webhook.WebhookPostVm webhookPostVm) {
                return null;
            }

            @Override
            public com.yas.webhook.model.Webhook toCreatedWebhook(com.yas.webhook.model.viewmodel.webhook.WebhookPostVm webhookPostVm) {
                return null;
            }

            @Override
            public com.yas.webhook.model.viewmodel.webhook.WebhookDetailVm toWebhookDetailVm(com.yas.webhook.model.Webhook createdWebhook) {
                return null;
            }
        };

        List<EventVm> vms = mapper.toWebhookEventVms(List.of());
        assertNotNull(vms);
        assertTrue(vms.isEmpty());
    }

    @Test
    void toWebhookEventVms_nonEmpty_mapsEventId() {
        WebhookEvent ev = new WebhookEvent();
        ev.setEventId(42L);
        WebhookMapper mapper = new WebhookMapper() {
            @Override
            public WebhookVm toWebhookVm(com.yas.webhook.model.Webhook webhook) { return null; }
            @Override
            public com.yas.webhook.model.Webhook toUpdatedWebhook(com.yas.webhook.model.Webhook webhook, com.yas.webhook.model.viewmodel.webhook.WebhookPostVm webhookPostVm) { return null; }
            @Override
            public com.yas.webhook.model.Webhook toCreatedWebhook(com.yas.webhook.model.viewmodel.webhook.WebhookPostVm webhookPostVm) { return null; }
            @Override
            public com.yas.webhook.model.viewmodel.webhook.WebhookDetailVm toWebhookDetailVm(com.yas.webhook.model.Webhook createdWebhook) { return null; }
        };

        List<EventVm> vms = mapper.toWebhookEventVms(List.of(ev));
        assertEquals(1, vms.size());
        assertEquals(42L, vms.get(0).getId());
    }

    @Test
    void toWebhookListGetVm_buildsVm() {
        com.yas.webhook.model.Webhook w1 = new com.yas.webhook.model.Webhook();
        w1.setId(1L);
        com.yas.webhook.model.Webhook w2 = new com.yas.webhook.model.Webhook();
        w2.setId(2L);

        WebhookMapper mapper = new WebhookMapper() {
            @Override
            public WebhookVm toWebhookVm(com.yas.webhook.model.Webhook webhook) {
                WebhookVm vm = new WebhookVm();
                vm.setId(webhook.getId());
                return vm;
            }

            @Override
            public com.yas.webhook.model.Webhook toUpdatedWebhook(com.yas.webhook.model.Webhook webhook, com.yas.webhook.model.viewmodel.webhook.WebhookPostVm webhookPostVm) { return null; }
            @Override
            public com.yas.webhook.model.Webhook toCreatedWebhook(com.yas.webhook.model.viewmodel.webhook.WebhookPostVm webhookPostVm) { return null; }
            @Override
            public com.yas.webhook.model.viewmodel.webhook.WebhookDetailVm toWebhookDetailVm(com.yas.webhook.model.Webhook createdWebhook) { return null; }
        };

        PageImpl<com.yas.webhook.model.Webhook> page = new PageImpl<>(List.of(w1, w2), PageRequest.of(0, 2), 2);
        WebhookListGetVm vm = mapper.toWebhookListGetVm(page, 0, 2);
        assertNotNull(vm);
        assertEquals(2, vm.getWebhooks().size());
        assertEquals(0, vm.getPageNo());
        assertEquals(2, vm.getPageSize());
        assertEquals(2, vm.getTotalElements());
    }
}
