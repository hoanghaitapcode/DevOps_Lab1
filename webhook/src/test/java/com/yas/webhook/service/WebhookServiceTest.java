package com.yas.webhook.service;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.webhook.integration.api.WebhookApi;
import com.yas.webhook.model.Webhook;
import com.yas.webhook.model.WebhookEvent;
import com.yas.webhook.model.WebhookEventNotification;
import com.yas.webhook.model.dto.WebhookEventNotificationDto;
import com.yas.webhook.model.enums.NotificationStatus;
import com.yas.webhook.model.mapper.WebhookMapper;
import com.yas.webhook.model.viewmodel.webhook.EventVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookDetailVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookListGetVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookPostVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookVm;
import com.yas.webhook.repository.EventRepository;
import com.yas.webhook.repository.WebhookEventNotificationRepository;
import com.yas.webhook.repository.WebhookEventRepository;
import com.yas.webhook.repository.WebhookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebhookServiceTest {

    WebhookRepository webhookRepository = mock(WebhookRepository.class);
    EventRepository eventRepository = mock(EventRepository.class);
    WebhookEventRepository webhookEventRepository = mock(WebhookEventRepository.class);
    WebhookEventNotificationRepository webhookEventNotificationRepository = mock(WebhookEventNotificationRepository.class);
    WebhookMapper webhookMapper = mock(WebhookMapper.class);
    WebhookApi webHookApi = mock(WebhookApi.class);

    WebhookService service;

    @BeforeEach
    void setUp() {
        service = new WebhookService(webhookRepository, eventRepository, webhookEventRepository,
            webhookEventNotificationRepository, webhookMapper, webHookApi);
    }

    @Test
    void getPageableWebhooks_delegatesToMapper() {
        Webhook w = new Webhook(); w.setId(1L);
        Page<Webhook> page = new PageImpl<>(List.of(w));
        when(webhookRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);
        WebhookListGetVm vm = WebhookListGetVm.builder().webhooks(List.of(new WebhookVm())).build();
        when(webhookMapper.toWebhookListGetVm(page, 0, 10)).thenReturn(vm);

        var res = service.getPageableWebhooks(0, 10);
        assertSame(vm, res);
        verify(webhookRepository).findAll(any());
    }

    @Test
    void findAllWebhooks_mapsEach() {
        Webhook w = new Webhook(); w.setId(2L);
        when(webhookRepository.findAll((Sort) any())).thenReturn(List.of(w));
        when(webhookMapper.toWebhookVm(w)).thenReturn(new WebhookVm());

        var res = service.findAllWebhooks();
        assertEquals(1, res.size());
    }

    @Test
    void findById_notFound_throws() {
        when(webhookRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.findById(5L));
    }

    @Test
    void create_withEvents_initializesAndReturnsDetailVm() {
        WebhookPostVm postVm = new WebhookPostVm("u", null, null, null,
            List.of(EventVm.builder().id(7L).build()));
        Webhook created = new Webhook(); created.setId(9L);
        when(webhookMapper.toCreatedWebhook(postVm)).thenReturn(created);
        when(webhookRepository.save(created)).thenReturn(created);
        when(eventRepository.findById(7L)).thenReturn(Optional.of(new com.yas.webhook.model.Event()));
        when(webhookEventRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        when(webhookMapper.toWebhookDetailVm(created)).thenReturn(new WebhookDetailVm());

        var res = service.create(postVm);
        assertNotNull(res);
        verify(webhookEventRepository).saveAll(any());
    }

    @Test
    void delete_notFound_throws() {
        when(webhookRepository.existsById(10L)).thenReturn(false);
        assertThrows(NotFoundException.class, () -> service.delete(10L));
    }

    @Test
    void notifyToWebhook_callsApi_and_updatesNotification() {
        WebhookEventNotification n = new WebhookEventNotification(); n.setId(100L); n.setNotificationStatus(NotificationStatus.NOTIFYING);
        when(webhookEventNotificationRepository.findById(100L)).thenReturn(Optional.of(n));

        WebhookEventNotificationDto dto = WebhookEventNotificationDto.builder()
            .notificationId(100L)
            .url("u")
            .secret("")
            .payload(null).build();

        service.notifyToWebhook(dto);

        verify(webHookApi).notify("u", "s", "p");
        ArgumentCaptor<WebhookEventNotification> cap = ArgumentCaptor.forClass(WebhookEventNotification.class);
        verify(webhookEventNotificationRepository).save(cap.capture());
        assertEquals(NotificationStatus.NOTIFIED, cap.getValue().getNotificationStatus());
    }

    @Test
    void initializeWebhookEvents_whenEventMissing_throws() {
        WebhookPostVm postVm = WebhookPostVm.builder().events(List.of(EventVm.builder().id(999L).build())).build();
        when(webhookMapper.toCreatedWebhook(postVm)).thenReturn(new Webhook());
        when(webhookRepository.save(any())).thenReturn(new Webhook());
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        // call create to exercise initializeWebhookEvents via create
        assertThrows(NotFoundException.class, () -> service.create(postVm));
    }
}

