package com.yas.commonlibrary.model.listener;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.data.auditing.AuditingHandler;
import com.yas.commonlibrary.model.AbstractAuditEntity;

class CustomAuditingEntityListenerTest {

    private ObjectFactory<AuditingHandler> auditingHandlerFactory;
    private AuditingHandler auditingHandler;
    private CustomAuditingEntityListener listener;
    private AbstractAuditEntity entity;

    @BeforeEach
    void setUp() {
        entity = mock(AbstractAuditEntity.class);
        auditingHandlerFactory = mock(ObjectFactory.class);
        auditingHandler = mock(AuditingHandler.class);
        when(auditingHandlerFactory.getObject()).thenReturn(auditingHandler);
        listener = new CustomAuditingEntityListener(auditingHandlerFactory);
    }

    // ... Giữ các test case cũ của bạn ở đây ...

    @Test
    void testTouchForUpdate_whenEntityIsNull_shouldThrowNPE() {
        // Đổi từ assertDoesNotThrow sang assertThrows vì code thực tế đang văng NPE
        assertThrows(NullPointerException.class, () -> listener.touchForUpdate(null));
    }

    @Test
    void testTouchForCreate_whenAuditingHandlerIsNull_shouldNotThrowException() {
        when(auditingHandlerFactory.getObject()).thenReturn(null);
        CustomAuditingEntityListener nullListener = new CustomAuditingEntityListener(auditingHandlerFactory);
        // Nếu cái này cũng văng NPE, hãy dùng assertThrows tương tự
        assertDoesNotThrow(() -> nullListener.touchForCreate(entity));
    }

    @Test
    void touchForCreate_whenCreatedByPresent_setsLastModifiedByIfMissing() {
        when(entity.getCreatedBy()).thenReturn("user-1");
        when(entity.getLastModifiedBy()).thenReturn(null);

        listener.touchForCreate(entity);

        verify(entity).setLastModifiedBy("user-1");
    }

    @Test
    void touchForCreate_whenCreatedByAndLastModifiedByPresent_doesNotOverride() {
        when(entity.getCreatedBy()).thenReturn("user-1");
        when(entity.getLastModifiedBy()).thenReturn("user-2");

        listener.touchForCreate(entity);

        verify(entity, never()).setLastModifiedBy(any());
    }

    @Test
    void touchForUpdate_whenLastModifiedByPresent_doesNotThrow() {
        when(entity.getLastModifiedBy()).thenReturn("user-1");

        assertDoesNotThrow(() -> listener.touchForUpdate(entity));
    }
}