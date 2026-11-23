package com.example.truyendex;

import com.example.truyendex.core.SearchText;
import java.text.Normalizer;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SearchTextTest {
    @Test
    public void accentedAndDecomposedVietnamese_matchPlainQuery() {
        String title = "  ĐƯỜNG   đến Huế ";
        assertEquals("duong den hue", SearchText.normalize(title));
        assertEquals("duong den hue", SearchText.normalize(Normalizer.normalize(title, Normalizer.Form.NFD)));
    }
}
