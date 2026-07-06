package com.yas.media.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.commonlibrary.exception.UnsupportedMediaTypeException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

class ControllerAdvisorTest {

    private final ControllerAdvisor controllerAdvisor = new ControllerAdvisor();

    @Test
    void handleUnsupportedMediaTypeException_shouldReturnBadRequest() {
        UnsupportedMediaTypeException exception = new UnsupportedMediaTypeException("bad type");
        WebRequest request = new ServletWebRequest(new MockHttpServletRequest());

        var response = controllerAdvisor.handleUnsupportedMediaTypeException(exception, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().title()).isEqualTo("Unsupported media type");
    }

    @Test
    void handleNotFoundException_shouldReturnNotFound() {
        NotFoundException notFoundException = mock(NotFoundException.class);
        when(notFoundException.getMessage()).thenReturn("Media not found");
        WebRequest request = new ServletWebRequest(new MockHttpServletRequest());

        var response = controllerAdvisor.handleNotFoundException(notFoundException, request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().detail()).isEqualTo("Media not found");
    }

    @Test
    void handleConstraintViolation_shouldReturnBadRequest() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = (ConstraintViolation<Object>) mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(violation.getRootBeanClass()).thenReturn((Class) String.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(path.toString()).thenReturn("field");
        when(violation.getMessage()).thenReturn("must not be blank");
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));

        var response = controllerAdvisor.handleConstraintViolation(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().fieldErrors()).hasSize(1);
    }

    @Test
    void handleIoException_shouldReturnInternalServerError() {
        RuntimeException exception = new RuntimeException("runtime error");
        WebRequest request = new ServletWebRequest(new MockHttpServletRequest());

        var response = controllerAdvisor.handleIoException(exception, request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().detail()).isEqualTo("runtime error");
    }

    @Test
    void handleOtherException_shouldReturnInternalServerError() {
        Exception exception = new Exception("generic error");
        WebRequest request = new ServletWebRequest(new MockHttpServletRequest());

        var response = controllerAdvisor.handleOtherException(exception, request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().detail()).isEqualTo("generic error");
    }
}
