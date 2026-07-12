package com.yas.media.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class FileTypeValidatorTest {

    private FileTypeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new FileTypeValidator();
        ValidFileType validFileType = mock(ValidFileType.class);
        when(validFileType.allowedTypes()).thenReturn(new String[] {"image/jpeg", "image/png", "image/gif"});
        when(validFileType.message()).thenReturn("invalid");
        validator.initialize(validFileType);
    }

    @Test
    void isValid_whenFileIsNull_thenFalse() {
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder builder =
            mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate("invalid")).thenReturn(builder);

        boolean result = validator.isValid(null, context);

        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenUnsupportedType_thenFalse() {
        MultipartFile file = new MockMultipartFile("f", "a.txt", "text/plain", "abc".getBytes());
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder builder =
            mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate("invalid")).thenReturn(builder);

        boolean result = validator.isValid(file, context);

        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenAllowedTypeButNotImage_thenFalse() {
        MultipartFile file = new MockMultipartFile("f", "a.png", "image/png", "not-image".getBytes());
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

        boolean result = validator.isValid(file, context);

        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenValidPngImage_thenTrue() throws IOException {
        byte[] image = createImage("png");
        MultipartFile file = new MockMultipartFile("f", "a.png", "image/png", image);
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

        boolean result = validator.isValid(file, context);

        assertThat(result).isTrue();
    }

    private byte[] createImage(String type) throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, type, out);
        return out.toByteArray();
    }
}
