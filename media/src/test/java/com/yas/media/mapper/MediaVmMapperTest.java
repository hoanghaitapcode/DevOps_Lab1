package com.yas.media.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.media.model.Media;
import com.yas.media.viewmodel.MediaVm;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class MediaVmMapperTest {

    private final MediaVmMapper mapper = Mappers.getMapper(MediaVmMapper.class);

    @Test
    void toVm_shouldMapAllFields() {
        Media media = new Media();
        media.setId(10L);
        media.setCaption("caption");
        media.setFileName("file.png");
        media.setMediaType("image/png");

        MediaVm vm = mapper.toVm(media);

        assertThat(vm).isNotNull();
        assertThat(vm.getId()).isEqualTo(10L);
        assertThat(vm.getCaption()).isEqualTo("caption");
        assertThat(vm.getFileName()).isEqualTo("file.png");
        assertThat(vm.getMediaType()).isEqualTo("image/png");
    }

    @Test
    void toModel_shouldMapAllFields() {
        MediaVm vm = new MediaVm(20L, "c2", "file2.jpg", "image/jpeg", null);

        Media media = mapper.toModel(vm);

        assertThat(media).isNotNull();
        assertThat(media.getId()).isEqualTo(20L);
        assertThat(media.getCaption()).isEqualTo("c2");
        assertThat(media.getFileName()).isEqualTo("file2.jpg");
        assertThat(media.getMediaType()).isEqualTo("image/jpeg");
    }

    @Test
    void partialUpdate_shouldIgnoreNullProperties() {
        Media media = new Media();
        media.setId(1L);
        media.setCaption("old");
        media.setFileName("old.png");

        MediaVm vm = new MediaVm(null, "new-caption", null, null, null);

        mapper.partialUpdate(media, vm);

        assertThat(media.getId()).isEqualTo(1L);
        assertThat(media.getCaption()).isEqualTo("new-caption");
        assertThat(media.getFileName()).isEqualTo("old.png");
    }
}
