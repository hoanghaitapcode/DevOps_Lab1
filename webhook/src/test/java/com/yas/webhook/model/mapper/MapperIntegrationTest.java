package com.yas.webhook.model.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.yas.webhook.model.Webhook;
import com.yas.webhook.model.WebhookEvent;
import com.yas.webhook.model.viewmodel.webhook.EventVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookPostVm;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class MapperIntegrationTest {

    WebhookMapper mapper = Mappers.getMapper(WebhookMapper.class);
    EventMapper eventMapper = Mappers.getMapper(EventMapper.class);

    @Test
    void toCreatedWebhook_mapsFields() {
        WebhookPostVm post = new WebhookPostVm("http://x", "sec", "ct", true, null);
        Webhook w = mapper.toCreatedWebhook(post);
        assertNotNull(w);
        assertEquals("http://x", w.getPayloadUrl());
        assertEquals("sec", w.getSecret());
        assertEquals(Boolean.TRUE, w.getIsActive());
    }

    @Test
    void toUpdatedWebhook_updatesTargetFields() {
        Webhook existing = new Webhook();
        existing.setPayloadUrl("old");
        existing.setSecret("oldsec");
        existing.setIsActive(true);

        WebhookPostVm post = new WebhookPostVm("newUrl", "newsec", "ct", Boolean.FALSE, null);
        Webhook updated = mapper.toUpdatedWebhook(existing, post);
        assertNotNull(updated);
        assertEquals("newUrl", updated.getPayloadUrl());
        assertEquals("newsec", updated.getSecret());
        assertEquals(Boolean.FALSE, updated.getIsActive());
    }

    @Test
    void toWebhookDetailVm_mapsEvents_and_ignoresSecret() {
        Webhook w = new Webhook();
        w.setId(10L);
        WebhookEvent we = new WebhookEvent();
        we.setEventId(5L);
        w.setWebhookEvents(List.of(we));

        var detail = mapper.toWebhookDetailVm(w);
        assertNotNull(detail);
        assertNull(detail.getSecret(), "secret should be ignored in mapping");
        assertNotNull(detail.getEvents());
        assertEquals(1, detail.getEvents().size());
        assertEquals(5L, detail.getEvents().get(0).getId());
    }

    @Test
    void eventMapper_mapsEventId() {
        com.yas.webhook.model.Event e = new com.yas.webhook.model.Event();
        e.setId(77L);
        EventVm vm = eventMapper.toEventVm(e);
        assertEquals(77L, vm.getId());
    }
}
