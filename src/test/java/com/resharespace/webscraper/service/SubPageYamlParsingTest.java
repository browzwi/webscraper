package com.browzwi.webscraper.service;

import com.browzwi.webscraper.repository.ScraperRecipeRepository;
import com.browzwi.webscraper.scraper.model.RecipeConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SubPageYamlParsingTest {

    @Mock
    private ScraperRecipeRepository repository;

    @Test
    void testYamlParsingWithSubPages() {
        String yaml = """
            name: Test Multi-Page
            match:
              urlRegexes:
                - 'https://www.facebook.com/.*'
            page:
              fields:
                - name: title
                  selectors: ["title"]
                  source: TEXT
              subPages:
                - path: /about
                  fields:
                    - name: about_info
                      selectors: ["div"]
                      source: TEXT
                - path: /photos
                  fields:
                    - name: photo_info
                      selectors: ["span"]
                      source: TEXT
            options:
              stripCss: true
            """;

        ScraperRecipeService service = new ScraperRecipeService(repository);
        RecipeConfig config = service.parse(yaml);

        assertNotNull(config);
        assertNotNull(config.getPage());
        assertNotNull(config.getPage().getSubPages());
        assertEquals(2, config.getPage().getSubPages().size());
        
        assertEquals("/about", config.getPage().getSubPages().get(0).getPath());
        assertEquals("/photos", config.getPage().getSubPages().get(1).getPath());
        
        assertEquals("about_info", config.getPage().getSubPages().get(0).getFields().get(0).getName());
        assertEquals("photo_info", config.getPage().getSubPages().get(1).getFields().get(0).getName());
        
        System.out.println("Sub-pages parsed successfully:");
        config.getPage().getSubPages().forEach(subPage -> 
            System.out.println("  Path: " + subPage.getPath() + ", Fields: " + subPage.getFields().size())
        );
    }
}
