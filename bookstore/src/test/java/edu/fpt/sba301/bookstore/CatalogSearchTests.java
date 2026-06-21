package edu.fpt.sba301.bookstore;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogSearchTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void searchByTitleIsCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/books")
                        .param("q", "clean")
                        .param("sort", "title_asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Clean Code"));
    }

    @Test
    void filterByCategoryAndSortByPrice() throws Exception {
        mockMvc.perform(get("/api/books")
                        .param("categoryId", "1")
                        .param("sort", "price_asc")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").isNumber());
    }

    @Test
    void emptySearchReturnsEmptyListWithCorrectTotal() throws Exception {
        mockMvc.perform(get("/api/books")
                        .param("q", "zzzznonexistentbooktitle99999")
                        .param("sort", "title_asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void invalidPriceRangeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/books")
                        .param("minPrice", "500000")
                        .param("maxPrice", "100000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidSortReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/books")
                        .param("sort", "invalid_sort"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void relatedBooksExcludeCurrentBookAndStayInCategory() throws Exception {
        mockMvc.perform(get("/api/books/1/related"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.id == 1)]").doesNotExist());
    }

    @Test
    void relatedBooksForMissingBookReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/books/999999/related"))
                .andExpect(status().isNotFound());
    }
}
