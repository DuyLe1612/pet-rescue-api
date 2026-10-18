package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.port.out.CloudStoragePort;
import com.uit.petrescueapi.application.port.out.PostMediaPort.AttachedMedia;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.infrastructure.persistence.entity.MediaFileJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.repository.MediaFileJpaRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PostMediaJpaAdapterTest {

    final MediaFileJpaRepository repo = mock(MediaFileJpaRepository.class);
    final CloudStoragePort cloud = mock(CloudStoragePort.class);
    final PostMediaJpaAdapter adapter = new PostMediaJpaAdapter(repo, cloud);
    final UUID postId = UUID.randomUUID();

    private MediaFileJpaEntity media(UUID id, String publicId, String status) {
        MediaFileJpaEntity e = new MediaFileJpaEntity();
        e.setMediaId(id);
        e.setPublicId(publicId);
        e.setStatus(status);
        e.setResourceType("image");
        when(repo.findByMediaIdAndDeletedFalse(id)).thenReturn(Optional.of(e));
        return e;
    }

    @Test
    void tempMediaIsPromotedIntoThePostFolderAndPermanentMediaIsUntouched() {
        UUID temp = UUID.randomUUID();
        UUID permanent = UUID.randomUUID();
        MediaFileJpaEntity tempEntity = media(temp, "temp/abc", "TEMP");
        media(permanent, "pet-rescue/old", "PERMANENT");
        when(cloud.moveToPermament("temp/abc", "posts/" + postId, "image")).thenReturn("posts/" + postId + "/abc");
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<AttachedMedia> attached = adapter.attach(postId, List.of(temp, permanent));

        assertThat(attached).containsExactly(
                new AttachedMedia(temp, "posts/" + postId + "/abc"),
                new AttachedMedia(permanent, "pet-rescue/old"));
        assertThat(tempEntity.getStatus()).isEqualTo("PERMANENT");
        assertThat(tempEntity.getFolder()).isEqualTo("posts/" + postId);
        verify(cloud, never()).moveToPermament(eq("pet-rescue/old"), any(), any());
    }

    @Test
    void unknownMediaIsRejectedBeforeAnythingMoves() {
        UUID temp = UUID.randomUUID();
        media(temp, "temp/abc", "TEMP");
        UUID missing = UUID.randomUUID();
        when(repo.findByMediaIdAndDeletedFalse(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.attach(postId, List.of(temp, missing))).isInstanceOf(BusinessException.class);
        verifyNoInteractions(cloud);
    }

    private static String eq(String s) {
        return org.mockito.ArgumentMatchers.eq(s);
    }
}
