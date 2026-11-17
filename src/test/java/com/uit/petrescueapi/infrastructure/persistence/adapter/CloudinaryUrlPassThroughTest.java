package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The demo data stores full photo URLs as public ids; {@code buildUrl} must hand them back
 * untouched instead of turning them into a Cloudinary path.
 */
class CloudinaryUrlPassThroughTest {

    private final Cloudinary cloudinary = new Cloudinary("cloudinary://key:secret@demo-cloud");

    @Test
    void aFullUrlComesBackAsItIs() {
        String photo = "https://images.unsplash.com/photo-1552053831-71594a27632d?w=1200&q=80&auto=format&fit=crop";
        assertThat(cloudinary.url().generate(photo)).isEqualTo(photo);
    }

    @Test
    void aPublicIdStillBecomesACloudinaryUrl() {
        assertThat(cloudinary.url().generate("pets/abc")).contains("res.cloudinary.com/demo-cloud").endsWith("pets/abc");
    }
}
