package com.shady.landing.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.shady.landing.common.slug.SlugGenerator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SlugGeneratorTest {

    private final SlugGenerator slugs = new SlugGenerator();

    @Test
    void slugifiesEnglishText() {
        assertThat(slugs.slugify("  Handmade Leather Bag — Large!  ")).isEqualTo("handmade-leather-bag-large");
    }

    @Test
    void stripsDiacritics() {
        assertThat(slugs.slugify("Café Crème Brûlée")).isEqualTo("cafe-creme-brulee");
    }

    @Test
    void arabicOnlyInputGetsRandomValidSlug() {
        String slug = slugs.slugify("حقيبة جلد");
        assertThat(slug).startsWith("item-");
        assertThat(slugs.isValid(slug)).isTrue();
    }

    @Test
    void truncatesLongInputWithoutTrailingHyphen() {
        String slug = slugs.slugify("word ".repeat(60));
        assertThat(slug.length()).isLessThanOrEqualTo(SlugGenerator.MAX_LENGTH);
        assertThat(slug).doesNotEndWith("-");
    }

    @Test
    void addsNumericSuffixWhenTaken() {
        Set<String> taken = Set.of("mug", "mug-2");
        assertThat(slugs.unique("mug", taken::contains)).isEqualTo("mug-3");
        assertThat(slugs.unique("bowl", taken::contains)).isEqualTo("bowl");
    }

    @Test
    void validatesSlugFormat() {
        assertThat(slugs.isValid("good-slug-2")).isTrue();
        assertThat(slugs.isValid("Bad Slug")).isFalse();
        assertThat(slugs.isValid("../etc/passwd")).isFalse();
        assertThat(slugs.isValid("-leading")).isFalse();
    }
}
