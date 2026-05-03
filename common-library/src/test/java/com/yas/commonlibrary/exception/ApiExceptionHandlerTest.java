package com.yas.commonlibrary.exception;

import com.yas.commonlibrary.viewmodel.error.ErrorVm;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void handleNotFoundException_buildsResponse() {
        WebRequest request = new ServletWebRequest(new MockHttpServletRequest("GET", "/items"));
        NotFoundException ex = new NotFoundException("greet", "User");

        ResponseEntity<ErrorVm> response = handler.handleNotFoundException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Hello User", response.getBody().detail());
    }

    @Test
    void handleMethodArgumentNotValid_buildsFieldErrors() throws Exception {
        WebRequest request = new ServletWebRequest(new MockHttpServletRequest("POST", "/items"));

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "obj");
        bindingResult.addError(new FieldError("obj", "name", "must not be blank"));

        Method method = Dummy.class.getDeclaredMethod("create", String.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ErrorVm> response = handler.handleMethodArgumentNotValid(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(1, response.getBody().fieldErrors().size());
        assertTrue(response.getBody().fieldErrors().get(0).contains("name"));
    }

    @Test
    void handleConstraintViolation_buildsFieldErrors() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Dummy> violation = (ConstraintViolation<Dummy>) Mockito.mock(ConstraintViolation.class);
        Path path = Mockito.mock(Path.class);
        Mockito.when(path.toString()).thenReturn("email");
        Mockito.when(violation.getRootBeanClass()).thenReturn(Dummy.class);
        Mockito.when(violation.getPropertyPath()).thenReturn(path);
        Mockito.when(violation.getMessage()).thenReturn("invalid");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ErrorVm> response = handler.handleConstraintViolation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(1, response.getBody().fieldErrors().size());
    }

    @Test
    void handleOtherHandlers_buildResponses() throws Exception {
        WebRequest request = new ServletWebRequest(new MockHttpServletRequest("GET", "/orders"));

        ResponseEntity<ErrorVm> badRequest = handler.handleBadRequestException(
            new BadRequestException("PAYMENT_FAIL_MESSAGE"), request);
        assertEquals(HttpStatus.BAD_REQUEST, badRequest.getStatusCode());

        ResponseEntity<ErrorVm> dataIntegrity = handler.handleDataIntegrityViolationException(
            new DataIntegrityViolationException("dup"));
        assertEquals(HttpStatus.BAD_REQUEST, dataIntegrity.getStatusCode());

        ResponseEntity<ErrorVm> duplicated = handler.handleDuplicated(
            new DuplicatedException("greet", "Dup"));
        assertEquals(HttpStatus.BAD_REQUEST, duplicated.getStatusCode());

        ResponseEntity<ErrorVm> internalServerError = handler.handleInternalServerErrorException(
            new InternalServerErrorException("item.count", 1));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, internalServerError.getStatusCode());

        ResponseEntity<ErrorVm> missingParam = handler.handleMissingParams(
            new MissingServletRequestParameterException("q", "String"));
        assertEquals(HttpStatus.BAD_REQUEST, missingParam.getStatusCode());

        ResponseEntity<ErrorVm> resourceExisted = handler.handleResourceExistedException(
            new ResourceExistedException("greet", "Res"), request);
        assertEquals(HttpStatus.CONFLICT, resourceExisted.getStatusCode());

        ResponseEntity<ErrorVm> accessDenied = handler.handleAccessDeniedException(
            new AccessDeniedException("ACCESS_DENIED"), request);
        assertEquals(HttpStatus.FORBIDDEN, accessDenied.getStatusCode());

        ResponseEntity<ErrorVm> wrongEmail = handler.handleWrongEmailFormatException(
            new WrongEmailFormatException("greet", "Mail"), request);
        assertEquals(HttpStatus.BAD_REQUEST, wrongEmail.getStatusCode());

        ResponseEntity<ErrorVm> createGuest = handler.handleCreateGuestUserException(
            new CreateGuestUserException("guest"), request);
        assertEquals(HttpStatus.BAD_REQUEST, createGuest.getStatusCode());

        ResponseEntity<ErrorVm> stockExisting = handler.handleStockExistingException(
            new StockExistingException("item.count", 2), request);
        assertEquals(HttpStatus.BAD_REQUEST, stockExisting.getStatusCode());

        ResponseEntity<ErrorVm> signIn = handler.handleSignInRequired(
            new SignInRequiredException("greet", "SignIn"));
        assertEquals(HttpStatus.FORBIDDEN, signIn.getStatusCode());

        ResponseEntity<ErrorVm> forbidden = handler.handleForbidden(
            new NotFoundException("greet", "Forbidden"), request);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());

        ResponseEntity<ErrorVm> other = handler.handleOtherException(new Exception("boom"), request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, other.getStatusCode());
    }

    private static class Dummy {
        @SuppressWarnings("unused")
        void create(String name) {
        }
    }
}
