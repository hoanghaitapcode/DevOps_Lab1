package com.yas.media.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.media.model.Media;
import com.yas.media.model.dto.MediaDto;
import com.yas.media.service.MediaService;
import com.yas.media.viewmodel.MediaPostVm;
import com.yas.media.viewmodel.MediaVm;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = MediaController.class,
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class MediaControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MediaService mediaService;

    private byte[] createImageBytes(String formatName) throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, formatName, outputStream);
        return outputStream.toByteArray();
    }

    @Test
    void testCreateMediaEndpoint_withValidFile_thenReturnCreatedMedia() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "multipartFile",
            "test.png",
            "image/png",
            createImageBytes("png")
        );

        Media media = new Media();
        media.setId(1L);
        media.setCaption("Test Caption");
        media.setFileName("test.png");
        media.setMediaType("image/png");

        when(mediaService.saveMedia(any(MediaPostVm.class))).thenReturn(media);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/medias")
                .file(file)
        .param("caption", "Test Caption")
        .param("fileNameOverride", "test.png"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.caption").value("Test Caption"))
            .andExpect(jsonPath("$.fileName").value("test.png"))
            .andExpect(jsonPath("$.mediaType").value("image/png"));
    }

    @Test
    void testDeleteMediaEndpoint_withValidId_thenReturnNoContent() throws Exception {
        doNothing().when(mediaService).removeMedia(1L);

        mockMvc.perform(MockMvcRequestBuilders.delete("/medias/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteMediaEndpoint_withInvalidId_thenReturnNotFound() throws Exception {
        doThrow(new NotFoundException("Media 999 is not found"))
            .when(mediaService).removeMedia(999L);

        mockMvc.perform(MockMvcRequestBuilders.delete("/medias/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testGetMediaEndpoint_withValidId_thenReturnMedia() throws Exception {
        MediaVm mediaVm = new MediaVm(1L, "Test Caption", "test.png", "image/png", "/media/medias/1/file/test.png");
        when(mediaService.getMediaById(1L)).thenReturn(mediaVm);

        mockMvc.perform(MockMvcRequestBuilders.get("/medias/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.caption").value("Test Caption"))
            .andExpect(jsonPath("$.fileName").value("test.png"))
            .andExpect(jsonPath("$.mediaType").value("image/png"))
            .andExpect(jsonPath("$.url").value("/media/medias/1/file/test.png"));
    }

    @Test
    void testGetMediaEndpoint_withInvalidId_thenReturnNotFound() throws Exception {
        when(mediaService.getMediaById(999L)).thenReturn(null);

        mockMvc.perform(MockMvcRequestBuilders.get("/medias/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testGetMediasByIdsEndpoint_withValidIds_thenReturnMediaList() throws Exception {
        MediaVm mediaVm1 = new MediaVm(1L, "Caption 1", "file1.png", "image/png", "/media/medias/1/file/file1.png");
        MediaVm mediaVm2 = new MediaVm(2L, "Caption 2", "file2.png", "image/png", "/media/medias/2/file/file2.png");
        List<MediaVm> mediaVms = List.of(mediaVm1, mediaVm2);

        when(mediaService.getMediaByIds(anyList())).thenReturn(mediaVms);

        mockMvc.perform(MockMvcRequestBuilders.get("/medias")
                .param("ids", "1", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].caption").value("Caption 1"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].caption").value("Caption 2"));
    }

    @Test
    void testGetMediasByIdsEndpoint_withEmptyResult_thenReturnNotFound() throws Exception {
        when(mediaService.getMediaByIds(anyList())).thenReturn(List.of());

        mockMvc.perform(MockMvcRequestBuilders.get("/medias")
                .param("ids", "999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testGetFileEndpoint_withValidIdAndFileName_thenReturnFile() throws Exception {
        byte[] fileContent = "test file content".getBytes();
        InputStream inputStream = new ByteArrayInputStream(fileContent);
        MediaDto mediaDto = MediaDto.builder()
            .content(inputStream)
            .mediaType(org.springframework.http.MediaType.valueOf("image/png"))
            .build();

        when(mediaService.getFile(1L, "test.png")).thenReturn(mediaDto);

        mockMvc.perform(MockMvcRequestBuilders.get("/medias/1/file/test.png"))
            .andExpect(status().isOk())
            .andExpect(result -> {
                byte[] responseContent = result.getResponse().getContentAsByteArray();
                assertTrue(responseContent.length > 0);
            });
    }

    @Test
    void testCreateMediaEndpoint_withJpegFile_thenReturnCreatedMedia() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "multipartFile",
            "test.jpeg",
            "image/jpeg",
            createImageBytes("jpg")
        );

        Media media = new Media();
        media.setId(2L);
        media.setCaption("JPEG Image");
        media.setFileName("test.jpeg");
        media.setMediaType("image/jpeg");

        when(mediaService.saveMedia(any(MediaPostVm.class))).thenReturn(media);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/medias")
                .file(file)
        .param("caption", "JPEG Image")
        .param("fileNameOverride", "test.jpeg"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(2))
            .andExpect(jsonPath("$.mediaType").value("image/jpeg"));
    }

    @Test
    void testCreateMediaEndpoint_withGifFile_thenReturnCreatedMedia() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "multipartFile",
            "test.gif",
            "image/gif",
            createImageBytes("gif")
        );

        Media media = new Media();
        media.setId(3L);
        media.setCaption("GIF Image");
        media.setFileName("test.gif");
        media.setMediaType("image/gif");

        when(mediaService.saveMedia(any(MediaPostVm.class))).thenReturn(media);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/medias")
                .file(file)
        .param("caption", "GIF Image")
        .param("fileNameOverride", "test.gif"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(3))
            .andExpect(jsonPath("$.mediaType").value("image/gif"));
    }
}
