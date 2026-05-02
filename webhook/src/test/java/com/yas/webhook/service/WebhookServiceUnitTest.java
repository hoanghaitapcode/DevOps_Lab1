package com.yas.webhook.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.webhook.integration.api.WebhookApi;
import com.yas.webhook.model.Webhook;
import com.yas.webhook.model.WebhookEventNotification;
import com.yas.webhook.model.dto.WebhookEventNotificationDto;
import com.yas.webhook.model.enums.NotificationStatus;
import com.yas.webhook.model.mapper.WebhookMapper;
import com.yas.webhook.model.viewmodel.webhook.WebhookListGetVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookPostVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookVm;
import com.yas.webhook.repository.EventRepository;
import com.yas.webhook.repository.WebhookEventNotificationRepository;
import com.yas.webhook.repository.WebhookEventRepository;
import com.yas.webhook.repository.WebhookRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
public class WebhookServiceUnitTest {

    @Mock
    WebhookRepository webhookRepository;
    @Mock
    EventRepository eventRepository;
    @Mock
    WebhookEventRepository webhookEventRepository;
    @Mock
    WebhookEventNotificationRepository webhookEventNotificationRepository;
    @Mock
    WebhookMapper webhookMapper;
    @Mock
    WebhookApi webHookApi;

    WebhookService service;

    @Captor
    ArgumentCaptor<WebhookEventNotification> notifCaptor;

    @BeforeEach
    void setUp() {
        service = new WebhookService(webhookRepository, eventRepository, webhookEventRepository,
            webhookEventNotificationRepository, webhookMapper, webHookApi);
    }

    @Test
    void getPageableWebhooks_returnsMapperValue() {
        PageImpl<Webhook> page = new PageImpl<>(List.of(new Webhook()), PageRequest.of(0, 1), 1);
        when(webhookRepository.findAll(any(PageRequest.class))).thenReturn(page);
        WebhookListGetVm expected = WebhookListGetVm.builder().pageNo(0).pageSize(1).totalElements(1).totalPages(1).isLast(true).webhooks(List.of()).build();
        when(webhookMapper.toWebhookListGetVm(page, 0, 1)).thenReturn(expected);

        var res = service.getPageableWebhooks(0, 1);
        assertSame(expected, res);
    }

    @Test
    void findAllWebhooks_delegatesToMapper() {
        Webhook w = new Webhook();
        doReturn(List.of(w)).when(webhookRepository).findAll(any(org.springframework.data.domain.Sort.class));
        when(webhookMapper.toWebhookVm(any())).thenReturn(new WebhookVm());

        var res = service.findAllWebhooks();
        assertNotNull(res);
        assertEquals(1, res.size());
    }

    @Test
    void findById_whenMissing_throwsNotFound() {
        when(webhookRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.findById(1L));
    }

    @Test
    void create_withEvents_initializesEvents() {
        WebhookPostVm postVm = new WebhookPostVm();
        EventVmWrapper ev = new EventVmWrapper(5L);
        // use reflection-style: WebhookPostVm has events field; set via setter if exists
        postVm.setEvents(List.of(ev.toEventVm()));

        Webhook created = new Webhook();
        created.setId(10L);
        when(webhookMapper.toCreatedWebhook(postVm)).thenReturn(created);
        when(webhookRepository.save(created)).thenReturn(created);
        when(eventRepository.findById(5L)).thenReturn(Optional.of(new com.yas.webhook.model.Event()));
        when(webhookEventRepository.saveAll(any())).thenReturn(List.of(new com.yas.webhook.model.WebhookEvent()));
        when(webhookMapper.toWebhookDetailVm(created)).thenReturn(null);

        service.create(postVm);

        verify(webhookEventRepository).saveAll(any());
        verify(webhookRepository).save(created);
    }

    @Test
    void create_withMissingEvent_throwsNotFound() {
        WebhookPostVm postVm = new WebhookPostVm();
        EventVmWrapper ev = new EventVmWrapper(99L);
        postVm.setEvents(List.of(ev.toEventVm()));

        Webhook created = new Webhook();
        created.setId(11L);
        when(webhookMapper.toCreatedWebhook(postVm)).thenReturn(created);
        when(webhookRepository.save(created)).thenReturn(created);
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.create(postVm));
    }

    @Test
    void delete_whenNotExists_throwsNotFound() {
        when(webhookRepository.existsById(7L)).thenReturn(false);
        assertThrows(NotFoundException.class, () -> service.delete(7L));
    }

    @Test
    void notifyToWebhook_updatesNotificationStatus() {
        tools.jackson.databind.JsonNode payload = mock(tools.jackson.databind.JsonNode.class);
        WebhookEventNotificationDto dto = WebhookEventNotificationDto.builder().notificationId(100L).url("u").secret("s").payload(payload).build();
        WebhookEventNotification notif = new WebhookEventNotification();
        notif.setId(100L);
        notif.setNotificationStatus(NotificationStatus.NOTIFYING);
        when(webhookEventNotificationRepository.findById(100L)).thenReturn(Optional.of(notif));

        service.notifyToWebhook(dto);

        verify(webHookApi).notify(eq("u"), eq("s"), eq(payload));
        verify(webhookEventNotificationRepository).save(notifCaptor.capture());
        assertEquals(NotificationStatus.NOTIFIED, notifCaptor.getValue().getNotificationStatus());
    }

    // small helper to avoid depending on EventVm class layout in this test file
    static class EventVmWrapper {
        final long id;
        EventVmWrapper(long id) { this.id = id; }
        com.yas.webhook.model.viewmodel.webhook.EventVm toEventVm() {
            com.yas.webhook.model.viewmodel.webhook.EventVm e = new com.yas.webhook.model.viewmodel.webhook.EventVm();
            e.setId(id);
            return e;
        }
    }
}
