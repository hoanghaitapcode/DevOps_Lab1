package com.yas.webhook.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.yas.webhook.model.viewmodel.webhook.WebhookDetailVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookListGetVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookPostVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookVm;
import com.yas.webhook.service.WebhookService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

class WebhookControllerTest {

    WebhookService service = mock(WebhookService.class);
    WebhookController controller;

    @BeforeEach
    void setUp() {
        controller = new WebhookController(service);
    }

    @Test
    void getPageableWebhooks_returnsOk() {
        WebhookListGetVm vm = WebhookListGetVm.builder().webhooks(List.of()).pageNo(0).pageSize(10).totalElements(0).totalPages(0).isLast(true).build();
        when(service.getPageableWebhooks(0,10)).thenReturn(vm);
        ResponseEntity<WebhookListGetVm> res = controller.getPageableWebhooks(0,10);
        assertEquals(200, res.getStatusCode().value());
        assertSame(vm, res.getBody());
    }

    @Test
    void listWebhooks_returnsList() {
        when(service.findAllWebhooks()).thenReturn(List.of(new WebhookVm()));
        ResponseEntity<List<WebhookVm>> res = controller.listWebhooks();
        assertEquals(200, res.getStatusCode().value());
        assertEquals(1, res.getBody().size());
    }

    @Test
    void getWebhook_returnsDetail() {
        WebhookDetailVm d = new WebhookDetailVm(); d.setId(5L);
        when(service.findById(5L)).thenReturn(d);
        ResponseEntity<WebhookDetailVm> res = controller.getWebhook(5L);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(5L, res.getBody().getId());
    }

    @Test
    void createWebhook_returnsCreated() {
        WebhookPostVm post = new WebhookPostVm();
        WebhookDetailVm d = new WebhookDetailVm(); d.setId(11L);
        when(service.create(post)).thenReturn(d);
        ResponseEntity<WebhookDetailVm> res = controller.createWebhook(post, UriComponentsBuilder.fromPath(""));
        assertEquals(201, res.getStatusCode().value());
        assertEquals(d, res.getBody());
    }

    @Test
    void updateWebhook_callsService_andReturnsNoContent() {
        WebhookPostVm post = new WebhookPostVm();
        ResponseEntity<Void> res = controller.updateWebhook(3L, post);
        verify(service).update(post, 3L);
        assertEquals(204, res.getStatusCode().value());
    }

    @Test
    void deleteWebhook_callsService_andReturnsNoContent() {
        ResponseEntity<Void> res = controller.deleteWebhook(7L);
        verify(service).delete(7L);
        assertEquals(204, res.getStatusCode().value());
    }
}
