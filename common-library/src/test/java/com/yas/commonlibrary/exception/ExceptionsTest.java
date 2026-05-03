package com.yas.commonlibrary.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionsTest {

    @Test
    void messageBasedExceptions_resolveMessages() {
        assertEquals("Hello User", new NotFoundException("greet", "User").getMessage());
        assertEquals("Hello Resource", new ResourceExistedException("greet", "Resource").getMessage());
        assertEquals("Hello Dup", new DuplicatedException("greet", "Dup").getMessage());
        assertEquals("You have 3 items", new InternalServerErrorException("item.count", 3).getMessage());
        assertEquals("Hello Mail", new WrongEmailFormatException("greet", "Mail").getMessage());
        assertEquals("You have 2 items", new StockExistingException("item.count", 2).getMessage());
        assertEquals("Hello Forbidden", new ForbiddenException("greet", "Forbidden").getMessage());
        assertEquals("Hello SignIn", new SignInRequiredException("greet", "SignIn").getMessage());
        assertEquals("You have 1 items", new BadRequestException("item.count", 1).getMessage());
    }

    @Test
    void mutableMessageExceptions_canUpdateMessage() {
        ResourceExistedException resourceExisted = new ResourceExistedException("RESOURCE_ALREADY_EXISTED");
        resourceExisted.setMessage("custom");
        assertEquals("custom", resourceExisted.getMessage());

        Forbidden forbidden = new Forbidden("FORBIDDEN");
        forbidden.setMessage("custom");
        assertEquals("custom", forbidden.getMessage());

        SignInRequiredException signInRequired = new SignInRequiredException("SIGN_IN_REQUIRED");
        signInRequired.setMessage("custom");
        assertEquals("custom", signInRequired.getMessage());
    }

    @Test
    void simpleRuntimeExceptions_preserveMessageAndCause() {
        MultipartFileContentException noArg = new MultipartFileContentException();
        assertNull(noArg.getMessage());

        MultipartFileContentException withMessage = new MultipartFileContentException("bad");
        assertEquals("bad", withMessage.getMessage());

        Throwable cause = new IllegalStateException("boom");
        MultipartFileContentException withCause = new MultipartFileContentException(cause);
        assertSame(cause, withCause.getCause());

        MultipartFileContentException withBoth = new MultipartFileContentException("bad", cause);
        assertEquals("bad", withBoth.getMessage());
        assertSame(cause, withBoth.getCause());

        UnsupportedMediaTypeException unsupported = new UnsupportedMediaTypeException("unsupported");
        assertEquals("unsupported", unsupported.getMessage());

        CreateGuestUserException createGuest = new CreateGuestUserException("guest");
        assertEquals("guest", createGuest.getMessage());

        AccessDeniedException accessDenied = new AccessDeniedException("ACCESS_DENIED");
        assertEquals("ACCESS_DENIED", accessDenied.getMessage());
    }
}
